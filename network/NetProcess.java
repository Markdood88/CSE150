package nachos.network;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import nachos.network.*;

import java.util.Arrays;
import java.lang.Math;

/**
 * A <tt>VMProcess</tt> that supports networking syscalls.
 */

public class NetProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public NetProcess() {
	super();
    }

    private static final int
	syscallConnect = 11,
	syscallAccept = 12;
    
    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>11</td><td><tt>int  connect(int host, int port);</tt></td></tr>
     * <tr><td>12</td><td><tt>int  accept(int port);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallAccept:
            return handleAccept(a0);
        case syscallConnect:
            return handleConnect(a0,a1);
		default:
		    return super.handleSyscall(syscall, a0, a1, a2, a3);
		}
    }
    
    private int handleConnect(int host, int port){
    	  int srcLink = Machine.networkLink().getLinkAddress();
          int srcPort = NetKernel.postOffice.findPort();
          
          Connection connection = new Connection(host, port, srcLink, srcPort);
          int i;
          for(i = 2; i < files.length; i ++){
        	  if (files[i] == null) {
              files[i] = connection;
              break;
        	  }		
          }

          try {
              MailMessage message = new MailMessage(host, port, srcLink, srcPort, 1, 0, new byte[0]);
              NetKernel.postOffice.send(message);
          } catch (MalformedPacketException e) {

              System.out.println("Malformed packet exception");

              Lib.assertNotReached();

              return -1;
          }



          System.out.println("acknowledge");
          MailMessage acknowledgement = NetKernel.postOffice.receive(srcPort);
          System.out.println("Acknowledge " + acknowledgement);
          
          String str1 = "";
          String str2 = "";
          String str3 = "";
          for(int a = 0; a<20;a++)
          {
            str1+= 'a';
            str2+= 'b';
            str3+= 'c';
          }
//          System.out.println(str1);
//          System.out.println(str1.getBytes());
//          System.out.println(str1.getBytes().toString());
           try {
              MailMessage message = new MailMessage(host, port, srcLink, srcPort, 1, 10, str1.getBytes());
              NetKernel.postOffice.send(message);
          } catch (MalformedPacketException e) {

              System.out.println("Malformed packet exception");

              Lib.assertNotReached();

              return -1;
          }


          return i;       
    }
    
    
    private int handleAccept(int port){
    	MailMessage message = NetKernel.postOffice.receive(port);

      System.out.println("ACCEPT CALLED");
        if (message == null) {
            return -1;
        }

        int dstLink  = message.packet.srcLink;
        int srcLink = Machine.networkLink().getLinkAddress();
        int dstPort = message.srcPort;
        Connection connection = new Connection(dstLink, dstPort, srcLink, port);
        NetKernel.postOffice.PortUsed(port);
        int i;

        for(i = 2; i < files.length; i++){
                if (files[i] == null) {
                files[i] = connection;
                break;
            }
        }

        try {
            MailMessage acknowledgement = new MailMessage(dstLink, dstPort, srcLink, port,  3, 0, new byte[0]);
            NetKernel.postOffice.send(acknowledgement);
        } catch(MalformedPacketException e) {
                System.out.println("malformed packed exception");
                Lib.assertNotReached();
                return -1;
        }
        

          while (true) {
        MailMessage ping = NetKernel.postOffice.receive(dstPort);

        MailMessage ack;

        try {
        // System.out.println("TEST");
          ack = new MailMessage(ping.packet.srcLink, ping.srcPort,
              ping.packet.dstLink, ping.dstPort,
              ping.status, ping.seqNum,
              ping.contents);
          }
      catch (MalformedPacketException e) {
    // should never happen...
    continue;
      }
      System.out.println(ping.contents.toString());
      NetKernel.postOffice.send(ack);
        }
//            return i;           
  } 
}

