package nachos.network;

import nachos.userprog.*;
import nachos.machine.*;
import nachos.threads.*;
import nachos.network.*;

import java.util.Arrays;
import java.lang.Math;

public class Connection extends OpenFile{
	
	public int srcLink;
	public int srcPort;
	public int dstLink;
	public int dstPort;
	public int curSeqNum;
	public int sendSeqNum;

	public Connection(int dstLink, int dstPort, int srcLink, int srcPort){
		super(null, "Connection");
		this.srcLink = srcLink;
		this.srcPort = srcPort;
		this.dstLink = dstLink;
		this.dstPort = dstPort;
		this.curSeqNum = 0;
		this.sendSeqNum = 0;
	}

	//Pull a message from mailbox and return number of bytes read from it
	
	public int read(byte[] buffer, int offset, int length) {
		MailMessage message = NetKernel.postOffice.receive(srcPort); //Grab message from mailbox
		if (message == null) {
			return 0;
		}
		curSeqNum++; //increase sequence number

		int numBytesRead = Math.min(length, message.contents.length); //Count bytes
		System.arraycopy(message.contents, 0, buffer, offset, numBytesRead); //Save contents of the packet

		return numBytesRead;
	}

	//Fill a packet with bytes from an input buffer, and send

   public int write(byte[] buf, int offset, int length) {
        int amount = Math.min(offset + length, buf.length);	//get necessary size for this message
        byte[] contents = Arrays.copyOfRange(buf, offset, amount); //store info into contents
        
        try {
        	MailMessage message = new MailMessage(dstLink, dstPort, srcLink, srcPort, 0, sendSeqNum + 1, contents); 
            System.out.println("write: " + message);
            NetKernel.postOffice.send(message); //send message with the current seq+1,
            sendSeqNum++;
            return amount;
        }
        catch (MalformedPacketException e) {
            return -1;
	}
    }
}
