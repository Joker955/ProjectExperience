package com.quickly.devploment.jvm;

/**
 * @Author lidengjin
 * @Date 2020/7/1 5:37 下午
 * @Version 1.0
 */
public class GarbageTest {

	private static final int _1MB = 1024 * 1024;

	/**
	 * VM参数：-verbose:gc -Xms20M -Xmx20M -Xmn10M -XX:+PrintGCDetails -XX:SurvivorRatio=8
	 */
	public static void testAllocation() {
		byte[] allocation1, allocation2, allocation3, allocation4;
		allocation1 = new byte[2 * _1MB];
		allocation2 = new byte[2 * _1MB];
		allocation3 = new byte[2 * _1MB];
		allocation4 = new byte[4 * _1MB]; // 出现一次Minor GC
	}

	public static void main(String[] args) {
		//		testAllocation();
		//		testPretenureSizeThreshold();
		testTenuringThreshold();
	}

	/**
	 * VM参数：-verbose:gc -Xms20M -Xmx20M -Xmn10M -XX:+PrintGCDetails -XX:SurvivorRatio=8
	 * -XX:PretenureSizeThreshold=3145728 大于这个内存的 直接放到老年代
	 */
	public static void testPretenureSizeThreshold() {
		byte[] allocation;
		allocation = new byte[4 * _1MB]; //直接分配在老年代中
	}

	/**
	 * VM参数：-verbose:gc -Xms20M -Xmx20M -Xmn10M -XX:+PrintGCDetails -XX:Survivor-
	 * Ratio=8 -XX:MaxTenuringThreshold=1
	 * -XX:+PrintTenuringDistribution
	 */
	@SuppressWarnings("unused")
	public static void testTenuringThreshold() {
		byte[] allocation1, allocation2, allocation3;
		allocation1 = new byte[_1MB / 4]; // 什么时候进入老年代决定于XX:MaxTenuring-
		// Threshold设置
		allocation2 = new byte[4 * _1MB];
		allocation3 = new byte[4 * _1MB];
		allocation3 = null;
		allocation3 = new byte[4 * _1MB];
	}

	/*
		1 什么样的内存进行垃圾回收？
		2 什么时候回收？
		3 如何回收？

		1 如何判断Java对象 已死？
			1 引用计数器法  -- 循环引用 不可避免 ，如果是循环引用，那么就可能会导致内存泄漏，进而导致内存溢出。
			2 可达性分析法  已 GC root 为根结点，向下搜索，所走的过程称为引用链。
				-- 可以作为 GC root 的 ，栈中引用的对象，或者说 各个线程被调用的方法堆栈中使用到的 参数，局部变量，等。
					方法区中 类静态属性 引用的对象 方法区中常量引用的对象，同步锁持有的对象，或者内部调用的对象等等。
		2 引用 --强引用 软引用，弱引用 虚引用。
		3 finalize() F-queue ,不一定会等待其执行结束，担心里面执行缓慢，造成队列堵塞，使得其他的队列中的对象 称为永久等待过程中。对象在垃圾回收之前的最后一次拯救，只会运行一次。
		4 方法区中垃圾回收 效果不大，性价比低。回收的目标 ： 废弃的常量和不再使用的类。

		2 分代收集 理论，分代存放不同的对象。根据对象年龄 进行收集
			Minor GC, major jc（cms 只有单独对老年代的收集，）, full gc，mix gc 整个新生代以及部分老年代 （只有 g1）
			对象之间的跨代引用 -怎么解决？ 跨代引用假说， 在新生代 建立数据结构 记忆集，标记到 老年代划分小块儿，然后标记出某一个老年代内存 存在跨代引用，当发生 minor gc 时，把包含了 跨代引用的对象 加入到
			gcroot 进行扫描，然后进行可达性分析。
		3 标记-复制 标记-清除 标记-整理
			标记-清除 缺点： 1 效率不稳定，当有大量的对象 需要回收时，进行大量的标记和清除工作，导致执行效率下降，2 内存空间的碎片化 过多。导致，当有大对象需要分配的时候没有足够的连续的内存 进行分配，不得已而进行一次垃圾回收动作。
			标记-复制 空间浪费过多，内存使用率减少。 Serial、ParNew 新生代垃圾回收器 使用的这种思想 eden survivor
			标记-整理 标记出来对象后，让活动的对象向内存空间一端移动，然后清理掉边界以外的内存。
			如果是老年代，用标记-整理的话，需要移动对象，那么老年代每次回收都会有大量的对象存活区域，移动并且标记的话 会有极大的负重，所以 老年代也是区分场景了，关注吞吐量的 parallel scavenge 标记-整理 ，关注延迟的cms 是标记清除算法
			和稀泥的解决方案 ，平时使用标记清除，暂时容忍内存碎片的存在，直到影响对象的内存分配的时候，在进行标记整理 以获得规整的空间，cms --就是这样的。
		4 分配担保 当新生代gc 时，survivor 放不下时 可以去老年代 放点儿对象。
		5 用户线程以及垃圾收集线程， 存在安全点，当用户线程达到安全点儿的时候，就可以进行垃圾回收了。中断两种 1 抢先式中断，-- 垃圾回收进行处理，当有的用户线程没有达到安全点的时候，恢复用户线程进行执行，然后再进行中断。2 主动式中断
			安全区域，安全点有缺点，当用户线程 sleep 的时候，没办法响应，所以引入了安全区域概念。
		6 卡表，字节数组，记忆集的具体实现。卡表的每一个元素都对应着 内存区域的某一块，俗称卡页。一般是2的n 次幂。只有卡页中有个一对象存在跨代引用，那么就标志为1 ，俗称元素变脏。
			通过写屏障 维护卡表的状态。类似于 AOP 环绕通知。一般都用写后屏障。还可能会遇到伪屏障，通过标志位 去解决。
		7 可达性分析算法理论上要求全过程都基于一个能保障一致性的快照中才能够进行分析，这意味着必须全程冻结用户线程的运行，那么并发下的可达性分析？三色域。白色，未被垃圾收集器访问过，黑色，被访问过，都已经扫过了，灰色，被垃圾回收器扫过，但是还有未扫过的对象。
		8 7种垃圾收集器
			1 serial 收集器，简单粗暴，单线程，并不是强调是一个线程，或者一组线程，而是，当其在执行的时候，需要暂停所有的用户线程，直到其收集完毕。 新生代使用标记复制算法，老年代使用标记整理算法，对客户端友好，因为在单线程中效率高。
			2 parnew 是serial的多线程版本，能与 cms 进行使用。但是因为 g1 的存在，使得其 成为了历史。
			3 parallel  scavenge ,和 parnew 差不多，但是关注点不一样，cms 关注的是尽可能缩短l垃圾收集器时 用户线程的等待时间。而parallel scavenge 是达到可控制的吞吐量。
				吞吐量 = 运行用户代码时间 / 运行用户代码时间 + 垃圾回收的时间。高吞吐量可以有效的利用处理器的资源，尽快完成处理器的计算任务。适合吞吐量优先的系统。
			4 serial old 是老年代版本
			5 parallel old 是 Parallel Scavenge的老年代版本。支持多线程并发收集
			6 cms 收集器，以获取最短回收时间为目的的收集器，关注服务的响应时间，减少系统的停顿时间，收集的时候 4个 步骤 {
				1 初始标记  存在 stop the world,只是标记一下 gcroot 能关联到的对象，速度很快
				2 并发标记  并发标记阶段就是从GC Roots的直接关联对象开始遍历整个对象图的过程，这个过程耗时较长但是不需要停顿用户线程，可以与垃圾收集线程一起并发运行，
				3 重新标记  存在 stop the world，为了修正并发标记期间，因用户程序继续运作而导致标记产生变动的那一部分对象的标记记录。时间的话 比初始的时间长，但是比并发的短，
				4 并发清除  并发清除阶段，清理删除掉标记阶段判断的已经死亡的对象，由于不需要移动存活对象，所以这个阶段也是可以与用户线程同时并发的
			}
			cms 特点，并发收集，低停顿，缺点，可能会占用处理器资源，导致吞吐量降低，2 如何处理浮动垃圾，只能等待下一次执行 3 必须不能等到老年代满了再进行收集，需要预留空间给并发清理阶段的用户线程使用。4 cms 使用的是 标记清除，那么就会产生碎片空间。因此需要设置参数，保证能够分配给对象内存空间。
			7 g1 收集器，面向局部收集设计，以及基于Region 的内存分布形式。一般通过 collection set Cset 进行收集，衡量标准不再是 哪个分代，而是哪块儿内存的存放垃圾多 回收效益大，俗称 Mixed Gc
				Region 中还有Humongous 区域，专门用来存储大对象，一般只要是超过了region的容量一半 就称为 大对象，进入到 humongous 中，region可以自己配置，1--32MB 之间。如果很大的对象，超过了Region 那么就用连续的humongous 进行存储对象。
				G1 可以建立可预测的停顿时间模型 是因为将region作为了最小的回收单元。即每次回收的内存空间都是 region的整数倍，让g1去跟踪每个region里面的垃圾堆积的价值大小，然后在后台维护了一个优先级表，每次进行回收时，根据用户设置的允许的收集停顿时间，优先处理价值收益最大的region区域，所以保证了在有限的时间内获取尽可能高的收集效率。
				region中的跨代引用，其实在每个region中也是有记忆集 卡表的，是一个哈希表，key 是region的地址，value 是集合。
				在回收的过程中产生的新对象，通过两个指针解决，在TAMS 指针之上的 表示新创建的，不需要做回收处理。如何保证在用户设置的时间内完成回收呢？ 通过每次回收Region ，会计算回收耗时，分析得出平均值，标准偏差等信息，然后进行回收，达到最高的效益。
				4个步骤 {
					1 初始标记，仅仅是标记一下能够 GCroot 关联的对象，然后修改ATMS 指针，让下一阶段的用户线程创建的新对象能够分配。时间很短。此时 用户线程等待
					2 并发标记 进行gcroot 可达性分析，此时时间比较长，可以和用户线程 并发执行，在结束时 需要重新更新 TAMS 的位置，因为用户线程在活动可能会产生新的对象。
					3 最终标记 此时对用户线程 做一个短暂停，用于处理最后的少量的记录
					4 筛选回收 负责更新 region的数据，并对每个Region 进行排序，通过回收价值以及成本 作为衡量标准，然后可以进行任意的选择Region 进行回收处理。这里的操作涉及到对象的移动，所以需要停止用户线程。由多条收集器线程进行并行执行。
				}
				g1 也是有缺点的 ，比如负载高，占用内存多，维护卡表 多，复杂。
			8 衡量垃圾收集器的标准 内存占用 吞吐量 延迟
			9 cms 和 g1 分别通过 增量更新 以及原始快照的方式 处理了用户并发阶段的标记。



	 */

}
