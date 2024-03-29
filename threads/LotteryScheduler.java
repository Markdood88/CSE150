package nachos.threads;

import java.util.HashSet;
import java.util.Random;

import nachos.machine.Lib;
import nachos.machine.Machine;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking the
 * maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
	/**
	 * Allocate a new lottery scheduler.
	 */
	public LotteryScheduler() {
	}

	public static final int priorityDefault = 1;

	public static final int priorityMinimum = 1;

	public static final int priorityMaximum = Integer.MAX_VALUE;

	@Override
	protected LotteryThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new LotteryThreadState(thread);

		//identical to PriorityScheduler getThreadState except returns as LotterThreadSTate
		return (LotteryThreadState) thread.schedulingState;
	}

	/**
	 * Allocate a new lottery thread queue.
	 *
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer tickets from
	 *            waiting threads to the owning thread.
	 * @return a new lottery thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new LotteryQueue(transferPriority);
	}

	protected class LotteryQueue extends PriorityScheduler.PriorityQueue {
		LotteryQueue(boolean transferPriority) {
			// call PriorityQueue constructor
			super(transferPriority);
		}

		@Override
		protected LotteryThreadState pickNextThread() {
			// return null if no next thread
			if (waitQueue.isEmpty())
				return null;

			// keep track of total effectivePriority
			int sum = 0;

			// find sum
			for (KThread thread : waitQueue) {
				sum += getThreadState(thread).getEffectivePriority();
			}

			// lottery is just random int
			Random random = new Random();
			int lottery = random.nextInt(sum) + 1;

			sum = 0;

			// find winner iof lottery
			for (KThread thread : waitQueue) {
				sum += getThreadState(thread).effectivePriority;
				if (lottery <= sum)
					return getThreadState(thread);
			}
			// return null if failed
			Lib.assertNotReached();
			return null;
		}
	}

	protected class LotteryThreadState extends PriorityScheduler.ThreadState {
		public LotteryThreadState(KThread thread) {
			// call ThreadState constructor
			super(thread);
		}

		@Override
		public int getEffectivePriority() {
			// method identical to PriorityScheduler's getEffectivePriority() except create new Hashset of LotteryThreadState
			return getEffectivePriority(new HashSet<LotteryThreadState>());
		}

		private int getEffectivePriority(HashSet<LotteryThreadState> threadSet) {
			// method identical to PriorityScheduler's getEffectivePriority()
			if (threadSet.contains(this)) {
				return priority;
			}

			effectivePriority = priority;

			for (PriorityQueue pq : donateQueue)
				if (pq.transferPriority)
					for (KThread thread : pq.waitQueue) {
						threadSet.add(this);
						effectivePriority += getThreadState(thread)
								.getEffectivePriority(threadSet);
						threadSet.remove(this);
					}

			return effectivePriority;
		}
	}
}
