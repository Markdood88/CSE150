package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());

	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());

	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());

	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);

	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority()
    {
    	// Disable interrupts, and keep track of the old interrupt state.
	boolean oldInterruptState = Machine.interrupt().disable();

	// Get the current priority of the current thread.
	int currentPriority = getPriority(KThread.currentThread());

	// If the priority is already at the maximum, then do not increase the priority.
	if (currentPriority == priorityMaximum)
	    return false;

	// Now increment the priority of the current thread.
	setPriority(KThread.currentThread(), currentPriority + 1);

	// Now set the interrupt state back to where it once was.
	Machine.interrupt().restore(oldInterruptState);

	// Return that the priority was successfully increased.
	return true;
    }

    public boolean decreasePriority()
    {
    	// Disable interrupts, and keep track of the old interrupt state.
	boolean oldInterruptState = Machine.interrupt().disable();

	// Get the current priority of the current thread.
	int currentPriority = getPriority(KThread.currentThread());

	// If the priority is already at the minimum, then do not decrease the priority.
	if (currentPriority == priorityMinimum)
	    return false;

	// Now decrement the priority of the current thread.
	setPriority(KThread.currentThread(), currentPriority - 1);

	// Now set the interrupt state back to where it once was.
	Machine.interrupt().restore(oldInterruptState);

	// Return that the priority was successfully increased.
	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
	}

	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).waitForAccess(this);
	}

	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).acquire(this);
	}

	public KThread nextThread() {
		Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me

		ThreadState t = pickNextThread(); //get next thread
		//check if t is null
      		if(t != null) {
			t.acquire(this);
        		return t.thread; //return the ThreadState's KThread
      		}
      		else {
        		return null; //return null if doesn't exist
      		}
	}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected ThreadState pickNextThread() {
	    	// implement me
      		//find ThreadState with the next highest priority

		int nextPriority = priorityMinimum; //set to priorityMinimum to compare with others
      		KThread nextThread = null; //init this as null, we will later getThreadState once highest priority is found
		//for each KThread t in the waitQueue
		for(KThread t : waitQueue) {
        		int priority = getEffectivePriority(t); //get KThreads priority
        		//find the highest priority by just comparing
        		if(nextThread == null || (priority > nextPriority)) {
				//if we found larger priority save that KThread and its priority
          			nextThread = t;
          			nextPriority = priority;
       			}
      		}
		if(nextThread == null)
			return null;
     	 	//return the highest priority
		return getThreadState(nextThread);
	}

	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me (if you want)
	}

	//helper method to check if PriorityQueue's waitQueue is empty
	public boolean empty() {
	    return waitQueue.size() == 0;
	}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;

  	//waiting queue
  	LinkedList<KThread> waitQueue = new LinkedList<KThread>();

	ThreadState hasLock = null;
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
	    this.thread = thread;

	    setPriority(priorityDefault);
	}

	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {
	    return priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
	public int getEffectivePriority() {
		// implement me
		// call the helper method with a new HashSet
		return getEffectivePriority(new HashSet<ThreadState>());
	}

	private int getEffectivePriority(HashSet<ThreadState> threadStates) {
		//if this ThreadState already exists in the hash table, return the priority
		if(threadStates.contains(this)) {
			return priority;
		}

		//set effectivePriority to this priority
		effectivePriority = priority;

		for(PriorityQueue pq : donateQueue) {
			//if transferPriority is true loop through each KThread to compare and update its effectivePriority
			if(pq.transferPriority) {
				for(KThread k : pq.waitQueue) {
					threadStates.add(this);
					int temp = getThreadState(k).getEffectivePriority(threadStates);
					threadStates.remove(this);
					//effectivePriority will be higher number
					effectivePriority = Math.max(temp, effectivePriority);
				}
			}
		}

		return effectivePriority;
	}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    if (this.priority == priority)
		return;

	    this.priority = priority;

		// implement me
		//call getEffectivePriority to update effectivePrioritys
		getEffectivePriority();
	}

	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
	public void waitForAccess(PriorityQueue waitQueue) {
		// implement me
		//add the thread this object is associated with from the PriorityQueue's waitQueue
		waitQueue.waitQueue.add(thread);
		//if the PriorityQueue's hasLock ThreadState is null then method is done
		if(waitQueue.hasLock == null) {
			return;
		}
		//else call the PriorityQueue's hasLock's getEffectivePriority()
		waitQueue.hasLock.getEffectivePriority();
	}

	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
	public void acquire(PriorityQueue waitQueue) {
		// implement me
		//remove the thread this object is associated with from the PriorityQueue's waitQueue
		waitQueue.waitQueue.remove(thread);
		//set this object to be hasLock of the PriorityQueue
		waitQueue.hasLock = this;
		//add the PriorityQueue to this object's dontateQueue
		donateQueue.add(waitQueue);
		//call getEffectivePriority to update this objects priority
		getEffectivePriority();
	}

	/** The thread with which this object is associated. */
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
	/** The effective prioty of the associated thread */
	protected int effectivePriority;
	/** The donation queue */
	protected LinkedList<PriorityQueue> donateQueue = new LinkedList<PriorityQueue>();
    }
}
