package nachos.network;

import nachos.machine.*;
import nachos.threads.*;

/**
 * A collection of message queues, one for each local port. A
 * <tt>PostOffice</tt> interacts directly with the network hardware. Because
 * of the network hardware, we are guaranteed that messages will never be
 * corrupted, but they might get lost.
 *
 * <p>
 * The post office uses a "postal worker" thread to wait for messages to arrive
 * from the network and to place them in the appropriate queues. This cannot
 * be done in the receive interrupt handler because each queue (implemented
 * with a <tt>SynchList</tt>) is protected by a lock.
 */
public class PostOffice {
    /**
     * Allocate a new post office, using an array of <tt>SynchList</tt>s.
     * Register the interrupt handlers with the network hardware and start the
     * "postal worker" thread.
     */
    public PostOffice() {
		messageReceived = new Semaphore(0);
		messageSent = new Semaphore(0);
		sendLock = new Lock();
		portLock = new Lock();

		queues = new ExtendedSynchList[MailMessage.portLimit];
	
	for (int i=0; i<queues.length; i++)
	    queues[i] = new ExtendedSynchList();

	Runnable receiveHandler = new Runnable() {
	    public void run() { receiveInterrupt(); }
	};
	Runnable sendHandler = new Runnable() {
	    public void run() { sendInterrupt(); }
	};
	Machine.networkLink().setInterruptHandlers(receiveHandler,
						   sendHandler);

	KThread t = new KThread(new Runnable() {
		public void run() { postalDelivery(); }
	    });

	t.fork();
    }

    /**
     * Retrieve a message on the specified port, waiting if necessary.
     *
     * @param	port	the port on which to wait for a message.
     *
     * @return	the message received.
     */
    public MailMessage receive(int port) {
	Lib.assertTrue(port >= 0 && port < queues.length);

	// System.out.println("Here 1");
	
	Lib.debug(dbgNet, "waiting for mail on port " + port);
	
//	System.out.println("Here 2");
	MailMessage mail = (MailMessage) queues[port].synchList.removeFirst();
	if (queues[port].synchList.list.size() == 0) {
		queues[port].isFree = true;
		}
//	System.out.println("Here 3");
	
	if (Lib.test(dbgNet))
	    System.out.println("got mail on port " + port + ": " + mail);

	// System.out.println("Here 4");
	
	return mail;
    }

    /**
     * Wait for incoming messages, and then put them in the correct mailbox.
     */
    private void postalDelivery() {
	while (true) {
	    messageReceived.P();

	    Packet p = Machine.networkLink().receive();

	    MailMessage mail;

	    try {
	    	mail = new MailMessage(p);
	    }
	    catch (MalformedPacketException e) {
	    	continue;
	    }

	    if (Lib.test(dbgNet))
	    	System.out.println("delivering mail to port " + mail.dstPort
				   + ": " + mail);

	    // atomically add message to the mailbox and wake a waiting thread
	    queues[mail.dstPort].synchList.add(mail);
//	    System.out.println("added mail to the synch list");
	    queues[mail.dstPort].isFree = false;
	}
   }

    /**
     * Called when a packet has arrived and can be dequeued from the network
     * link.
     */
    private void receiveInterrupt() {
    	messageReceived.V();
    }

    /**
     * Send a message to a mailbox on a remote machine.
     */
    public void send(MailMessage mail) {
    	if (Lib.test(dbgNet))
    		System.out.println("sending mail: " + mail);

    		sendLock.acquire();

    		Machine.networkLink().send(mail.packet);
    		messageSent.P();

    		sendLock.release();
    }

    /**
     * Called when a packet has been sent and another can be queued to the
     * network link. Note that this is called even if the previous packet was
     * dropped.
     */
    private void sendInterrupt() {
	messageSent.V();
    }
    
    public int findPort() {
    	portLock.acquire();
    	int i = 0;
		for (ExtendedSynchList listObj : queues) {
			if (listObj.isFree == true) {
				listObj.isFree = false;
				portLock.release();
				return i;
			}
			i++;
		}
		
		portLock.release();

		return -1;
    }
    
    public void PortUsed(int i) {
		portLock.acquire();
		if (!queues[i].isFree)
			System.out.println("port is not free");
		else queues[i].isFree = false;
		portLock.release();
	}

    private ExtendedSynchList[] queues;
    private Semaphore messageReceived;	// V'd when a message can be dequeued
    private Semaphore messageSent;	// V'd when a message can be queued
    private Lock sendLock;
    private Lock portLock;

    private static final char dbgNet = 'n';
    
    public class ExtendedSynchList {
   		SynchList synchList;
   		boolean isFree;
   		public ExtendedSynchList(){
			synchList = new SynchList();
			isFree = true;
		}
    }
}
