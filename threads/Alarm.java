package nachos.threads;

import nachos.machine.*;

import java.util.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
	
    public Alarm() {
		waitQueue = new ArrayList<KThread>();
        ticksQueue = new ArrayList<Long>();
        waitQueueLock = new Lock();
		Machine.timer().setInterruptHandler(new Runnable() {
                public  void run() { timerInterrupt(); }
            });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    
    public void timerInterrupt() {
		
		boolean restore = Machine.interrupt().disable();
		
		for (int i = 0; i < waitQueue.size(); i++) {
            if (Machine.timer().getTime() > ticksQueue.get(i)){
                waitQueue.get(i).ready();
                waitQueue.remove(i);
                ticksQueue.remove(i);
            }
        }
		
		Machine.interrupt().restore(restore);
		KThread.yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
	 
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
	
		long waitTime = Machine.timer().getTime() + x;
	
		boolean restore = Machine.interrupt().disable();
		waitQueueLock.acquire();
		
		//Add our current thread and wait times to a list of arrays to keep track of order
		waitQueue.add(KThread.currentThread());
		ticksQueue.add(waitTime);
		
		waitQueueLock.release();
		KThread.sleep();
		Machine.interrupt().restore(restore);
    }
    
    private Lock waitQueueLock;
	private List<KThread> waitQueue;
    private List<Long> ticksQueue;
 }