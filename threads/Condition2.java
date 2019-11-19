package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {//this is a test
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	     this.conditionLock = conditionLock;
        waitQueue = new LinkedList<KThread>();
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
     public void sleep() {
       /* This assert function was pre-written for all methods*/
	     Lib.assertTrue(conditionLock.isHeldByCurrentThread());

         boolean machineStatus = Machine.interrupt().disable(); //make sure machine is disabled before start
	     conditionLock.release(); //releases lock
         KThread thread = KThread.currentThread(); //current thread
         waitQueue.add(thread);
         KThread.sleep(); // puts thread to sleep
	     conditionLock.acquire(); //re-acquire lock
         Machine.interrupt().restore(machineStatus); //re-enable the machine
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	     Lib.assertTrue(conditionLock.isHeldByCurrentThread());

             boolean machineStatus = Machine.interrupt().disable();
             if(!waitQueue.isEmpty()) {
            	 KThread thread = waitQueue.removeFirst();
            	 if (thread != null) //if there is still another thread in queue
            		 thread.ready();
             }
             Machine.interrupt().restore(machineStatus);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	     Lib.assertTrue(conditionLock.isHeldByCurrentThread());
             while(!waitQueue.isEmpty()) //if there are still threads in Queue
             {
                  wake(); //wakes up current thread
             }
             Machine.interrupt().enable();
    }

    private Lock conditionLock;
    private LinkedList<KThread> waitQueue;

}