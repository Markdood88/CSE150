package nachos.threads;
import nachos.machine.*;
/**
* Written by: 
*	Jeff Foreman and Nimitt Tripathy
*/

/**
* A <i>communicator</i> allows threads to synchronously exchange 32-bit
* messages. Multiple threads can be waiting to <i>speak</i>,
* and multiple threads can be waiting to <i>listen</i>. But there should never
* be a time when both a speaker and a listener are waiting, because the two
* threads can be paired off at this point.
*/
public class Communicator 
{
	// The lock for the communicator. Only one will be allocated for the entire class.
	// Thus, the functions listen and speak must share it.
	private Lock comLock;

	// A global storage for the word to be passed between the speakers and listeners.
	private int word;
	
	// A check for whether or not a word has been spoken or not.
	private boolean wordAvailable;

	// Counters to keep track of the active number of speakers and listeners.
	private int speaker;
	private int listener;

	// Condition objects to store the condition of the current threads.
	// Allocate one for both a speaker and a listener.
	private Condition2 speakerThread;
	private Condition2 listenerThread;

	/**
	* Allocate a new communicator.
	*/
	public Communicator() 
	{
		// Allocate memory for the communicator's lock and call the constructor.
		comLock = new Lock();
		
		// Set an initial value for the global word temporarily.
		word = 0;
		
		// Set the check to false, no word is available.
		wordAvailable = false;
		
		// Initialize the counters.
		speaker = 0;
		listener = 0;
		
		// Allocate memory for the condition of the threads and call the constructor.
		speakerThread = new Condition2(comLock);
		listenerThread = new Condition2(comLock);
	}

	/**
	* Wait for a thread to listen through this communicator, and then transfer
	* <i>word</i> to the listener.
	*
	* <p>
	* Does not return until this thread is paired up with a listening thread.
	* Exactly one listener should receive <i>word</i>.
	*
	* @param	word	the integer to transfer.
	*/
	public void speak(int word) 
	{
		// Acquire the lock for this thread.
		comLock.acquire();

		// Show that a speaker is available by incrementing the counter.
		// This count also serves as the unique identifier for this speaker.
		speaker++;

		// If the speakers and listeners are not equal, they cannot be paired up and thus this thread should sleep.
		// Also, if there is a word that has been spoken and not retrieved, then wait as well.
		// Lastly, before going to sleep, wake up any and all possible listeners to have them run their own checks and
		// possibly "listen".
		while (listener != speaker || wordAvailable == true) {listenerThread.wakeAll();  speakerThread.sleep();}
		
		/*
		 * At this point, a listener has been paired up with this speaker
		 * therefore a word can be "spoken". 
		 */

		// "Speak" out word.
		this.word = word;

		// Show that a word has been "spoken" and is available to be "listened" to.
		wordAvailable = true;
		
		// Make sure listeners are awake to "listen" to the "spoken" word.
		listenerThread.wakeAll();
		
		// This speaker is done so decrement the number of speakers available.
		speaker--;

		// This function is finished, therefore release the lock.
		comLock.release();
	}

	/**
	* Wait for a thread to speak through this communicator, and then return
	* the <i>word</i> that thread passed to <tt>speak()</tt>.
	*
	* @return	the integer transferred.
	*/    
	public int listen() 
	{
		// Acquire the lock for this thread.
		comLock.acquire();

		// Show that a listener is available by incrementing the counter.
		// This count also serves as the unique identifier for this speaker.
		listener++;

		// The listener must check if a word is available.
		// Lastly, before going to sleep, wake up any and all possible speakers to have them run their own checks and
		// possibly "speak".
		while (wordAvailable == false) { speakerThread.wakeAll(); listenerThread.sleep(); }

		/*
		 * At this point, a speaker has been paired up with this listener
		 * therefore a word can be "listened" to. 
		 */

		// Show that a word has been "listened" to.
		wordAvailable = false;
		
		// This listener is done so decrement the number of listeners available.
		listener--;

		// This function is finished, therefore release the lock.
		comLock.release();

		// Return the "listened" word, which is stored in the global variable.
		return word;
	}
}
