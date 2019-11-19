package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.io.EOFException;
import java.util.HashMap;


/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
    private static final int ROOT = 1;
    private static int unique = ROOT;
    private int process_id;
    private UThread thread;    
    private HashMap<Integer, childProcess> map;
    private childProcess myChildProcess;

        class childProcess{
                UserProcess child;
                int status;

                childProcess(UserProcess process){
                        this.child=process;
                        this.status=arb;
                }
        }
    /**
     * Allocate a new process.
     */
    public UserProcess() 
    {
        map=new HashMap<Integer, childProcess>();  
            
            
        this.process_id = unique;
            unique++;

        // Reserve the first two file spots for stdin and stdout.
        files[0] = UserKernel.console.openForReading();
        files[1] = UserKernel.console.openForWriting();
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return  a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
    return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param   name    the name of the file containing the executable.
     * @param   args    the arguments to pass to the executable.
     * @return  <tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
    if (!load(name, args))
        return false;
    
     //new UThread(this).setName(name).fork();
        thread = new UThread(this);
        thread.setName(name).fork();
        

    return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
    Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param   vaddr   the starting virtual address of the null-terminated
     *          string.
     * @param   maxLength   the maximum number of characters in the string,
     *              not including the null terminator.
     * @return  the string read, or <tt>null</tt> if no null terminator was
     *      found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
    Lib.assertTrue(maxLength >= 0);

    byte[] bytes = new byte[maxLength+1];

    int bytesRead = readVirtualMemory(vaddr, bytes);

    for (int length=0; length<bytesRead; length++) {
        if (bytes[length] == 0)
        return new String(bytes, 0, length);
    }

    return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param   vaddr   the first byte of virtual memory to read.
     * @param   data    the array where the data will be stored.
     * @return  the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
    return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param   vaddr   the first byte of virtual memory to read.
     * @param   data    the array where the data will be stored.
     * @param   offset  the first byte to write in the array.
     * @param   length  the number of bytes to transfer from virtual memory to
     *          the array.
     * @return  the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
                 int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
        byte[] memory = Machine.processor().getMemory();

        int amount = 0;
        while(offset<data.length && length>0)
        {
            Machine.processor();
            int vpn = Processor.pageFromAddress(vaddr);
            int aOffset = Processor.offsetFromAddress(vaddr);
            if (vpn < 0 || vpn>=pageTable.length||!pageTable[vpn].valid)
            {
                break;//page out of bounds case
            }
            pageTable[vpn].used = true;

            int itt = Math.min(length, Math.min(pageSize-aOffset,data.length - offset));
            amount = amount+itt;
            System.arraycopy(memory, pageTable[vpn].ppn*pageSize + aOffset, data, offset, itt);
            vaddr = vaddr+itt;
            offset = offset+itt;
            length = length-itt;
        }
        return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param   vaddr   the first byte of virtual memory to write.
     * @param   data    the array containing the data to transfer.
     * @return  the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
    return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param   vaddr   the first byte of virtual memory to write.
     * @param   data    the array containing the data to transfer.
     * @param   offset  the first byte to transfer from the array.
     * @param   length  the number of bytes to transfer from the array to
     *          virtual memory.
     * @return  the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
              int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
        byte[] memory = Machine.processor().getMemory();

        int amount = 0;
        while(offset<data.length && length>0)
        {
            Machine.processor();
            int vpn = Processor.pageFromAddress(vaddr);
            int aOffset = Processor.offsetFromAddress(vaddr);
            if (vpn < 0 || vpn>=pageTable.length||pageTable[vpn].readOnly||!pageTable[vpn].valid)
            {
                break;//page out of bounds case
            }

            pageTable[vpn].used = true;
            pageTable[vpn].dirty = true;

            int itt = Math.min(length, Math.min(pageSize-aOffset,data.length - offset));
            amount = amount+itt;
            int paddr = pageTable[vpn].ppn*pageSize + aOffset;
            System.arraycopy(data, offset, memory, paddr, itt);
            vaddr = vaddr+itt;
            offset = offset+itt;
            length = length-itt;
            break;
        }
        return amount;
}
    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param   name    the name of the file containing the executable.
     * @param   args    the arguments to pass to the executable.
     * @return  <tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
    Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
    
    OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
    if (executable == null) {
        Lib.debug(dbgProcess, "\topen failed");
        return false;
    }

    try {
        coff = new Coff(executable);
    }
    catch (EOFException e) {
        executable.close();
        Lib.debug(dbgProcess, "\tcoff load failed");
        return false;
    }

    // make sure the sections are contiguous and start at page 0
    numPages = 0;
    for (int s=0; s<coff.getNumSections(); s++) {
        CoffSection section = coff.getSection(s);
        if (section.getFirstVPN() != numPages) {
        coff.close();
        Lib.debug(dbgProcess, "\tfragmented executable");
        return false;
        }
        numPages += section.getLength();
    }

    // make sure the argv array will fit in one page
    byte[][] argv = new byte[args.length][];
    int argsSize = 0;
    for (int i=0; i<args.length; i++) {
        argv[i] = args[i].getBytes();
        // 4 bytes for argv[] pointer; then string plus one for null byte
        argsSize += 4 + argv[i].length + 1;
    }
    if (argsSize > pageSize) {
        coff.close();
        Lib.debug(dbgProcess, "\targuments too long");
        return false;
    }

    // program counter initially points at the program entry point
    initialPC = coff.getEntryPoint();   

    // next comes the stack; stack pointer initially points to top of it
    numPages += stackPages;
    initialSP = numPages*pageSize;

    // and finally reserve 1 page for arguments
    numPages++;

    if (!loadSections())
        return false;

    // store arguments in last page
    int entryOffset = (numPages-1)*pageSize;
    int stringOffset = entryOffset + args.length*4;

    this.argc = args.length;
    this.argv = entryOffset;
    
    for (int i=0; i<argv.length; i++) {
        byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
        Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
        entryOffset += 4;
        Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
               argv[i].length);
        stringOffset += argv[i].length;
        Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
        stringOffset += 1;
    }

    return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return  <tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
    if (numPages > Machine.processor().getNumPhysPages()) {
        coff.close();
        Lib.debug(dbgProcess, "\tinsufficient physical memory");
        return false;
    }

    if(UserKernel.pageTable == null)
        return false;

    // int numPhysPages = Machine.processor().getNumPhysPages();
    pageTable = new TranslationEntry[numPages];
    for (int i=0; i<numPages; i++)
    {
        int p = UserKernel.addPage();
        if(p>=0)
            pageTable[i] = new TranslationEntry(i,p , true,false,false,false);  
        else
        {
            coff.close();
            return false;
        }
    }

    // load sections
    for (int s=0; s<coff.getNumSections(); s++) {
        CoffSection section = coff.getSection(s);
        
        Lib.debug(dbgProcess, "\tinitializing " + section.getName()
              + " section (" + section.getLength() + " pages)");

        for (int i=0; i<section.getLength(); i++) {
        int vpn = section.getFirstVPN()+i;
        pageTable[vpn].readOnly = section.isReadOnly();
        section.loadPage(i, pageTable[vpn].ppn);
        }
    }
    
    return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        for(int i = 0; i<numPages;i++)
        {
            UserKernel.removePage(pageTable[i].ppn);
            pageTable[i].valid = false;
        }
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
    Processor processor = Machine.processor();

    // by default, everything's 0
    for (int i=0; i<processor.numUserRegisters; i++)
        processor.writeRegister(i, 0);

    // initialize PC and SP according
    processor.writeRegister(Processor.regPC, initialPC);
    processor.writeRegister(Processor.regSP, initialSP);

    // initialize the first two argument registers to argc and argv
    processor.writeRegister(Processor.regA0, argc);
    processor.writeRegister(Processor.regA1, argv);
    }
    
    /*
     * BEGIN SYSCALL IMPLEMENTATIONS ===========================================================
     */
    
    /** The maximum allowed number of files to be opened at once, as described by StubFileSystem.*/
    private static final int maxOpenFiles = 16;
    
    /** The number of currently open files. Initialized at 2 because of stdin and stdout.*/
    private int openFiles = 2;
    
    /** The "file system". Each index in this array represents the "fileDescriptor".*/
    protected OpenFile[] files = new OpenFile[maxOpenFiles];

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() 
    {
        Machine.halt();
        
        Lib.assertNotReached("Machine.halt() did not halt machine!");
        return 0;
    }
    
    /**
     * Written by: Jeff Foreman on 20 March 2018 <p>
     * 
     * Creates a file in the File System. This is accomplished by invoking the stubFileSystem.
     * Also updates the "files" to keep track of the system state.
     * 
     * @param a0 : The syscall argument represents "char *name", the name of the file to be created.
     * 
     * @return The fileDescriptor of the file, which is its index in "files". Or an error.
     */
    private int handleCreat(int a0)
    {
        // Get the file name from the syscall argument.
        String name = readVirtualMemoryString(a0, 256);
        
        // Make sure the created name is valid.
        if (name == null) return -1;
        
        // Now create the file in the stubFileSystem.
        OpenFile file = ThreadedKernel.fileSystem.open(name, true);
        
        // Now, if the fileSystem.open() returns a null value, then the file could not be created. Return an "error".
        if (file == null) return -1;
        
        // Find an empty spot to "place" the file.
        for(int fileDescriptor = 0; fileDescriptor < maxOpenFiles; fileDescriptor++)
        {
            if(files[fileDescriptor] == null)
            {
                openFiles++;
                
                // Place the file in the empty spot.
                files[fileDescriptor] = file;
                
                // Return the fileDescriptor.
                return fileDescriptor;
                
            } // End if.
        } // End i loop.
        
        // If this point is reached, then an empty spot could not be found.
        // Return an "error".
        return -1;
       
    } // End handleCreat().
    
    /**
     * Written by: Jeff Foreman on 20 March 2018 <p>
     * 
     * Opens a file in the file system. Marks that the file is currently open, then returns the file descriptor. 
     * If the file name cannot be found, it returns an error.
     * 
     * @param a0 : The syscall argument represents "char *name", the name of the file to be opened.
     * 
     * @return The fileDescriptor of the file, which is its index in "files". Or an error. 
     */
    private int handleOpen(int a0)
    {
        // Before doing anything, check to see if the max number of open files has been reached.
        if(openFiles == maxOpenFiles) return -1;
        
        // Get the file name from the syscall argument.
        String name = readVirtualMemoryString(a0, 256);
        
        // Make sure the created name is valid.
        if (name == null) return -1;
        
        // Now create the file in the stubFileSystem.
        OpenFile file = ThreadedKernel.fileSystem.open(name, false);
        
        // Now, if the fileSystem.open() returns a null value, then the file could not be created. Return an "error".
        if (file == null) return -1;
        
        // Find an empty spot to "place" the file.
        for(int fileDescriptor = 0; fileDescriptor < maxOpenFiles; fileDescriptor++)
        {
            if(files[fileDescriptor] == null)
            {
		// Show that a file has been opened.
                openFiles++;
                
                // Place the file in the empty spot.
                files[fileDescriptor] = file;
                
                // Return the fileDescriptor.
                return fileDescriptor;
                
            } // End if.
        } // End i loop.
        
        // If this point is reached, then an empty spot could not be found.
        // Return an "error".
        return -1;
		
    } // End handleOpen().
    
    /**
     * Written by: Jeff Foreman on 20 March 2018 <p>
     * 
     * The read system call takes in a file descriptor, a file position pointer, and the expected number of bytes to be read. 
     * The call will then read the file byte by byte, up to the expected number of bytes or until the end of file or stream. 
     * Upon completion, it returns the amount of bytes read. 
     * If the file descriptor is invalid, part of the buffer is unreadable, or if the stream was terminated, the call returns an error. 
     * This results in returning an undefined file position.
     * 
     * @param a0 : The syscall argument represents the fileDescriptor.
     * @param a1 : The syscall argument represents the buffer address.
     * @param a2 : The syscall argument represents the buffer size.
     * @return : The number of bytes read from the file onto the buffer, or "error".
     */
    private int handleRead(int a0, int a1, int a2)
    {
        // a0 contains the fileDescriptor.
        int fileDescriptor = a0;
        
        // Before proceeding, check if the fileDescriptor is valid.
        if(fileDescriptor < 0 || fileDescriptor >= maxOpenFiles)
                return -1;
        
        // Make sure there is a file there.
        if(files[fileDescriptor] == null)
            return -1;
        
        // a1 contains the buffer address.
        int bufferAddress = a1;
        
        // a2 contains the buffer size.
        int bufferSize = a2;
        
        // Create a temporary buffer.
        byte[] buffer = new byte[bufferSize];
        
        // Now read from the file on to the temporary buffer.
        int bytesRead = files[fileDescriptor].read(buffer, 0, bufferSize);
        
        // Check if data was successfully read from the file to the buffer.
        if(bytesRead <= 0)
            return -1;
        
        // Write to the real buffer.
        bytesRead = writeVirtualMemory(bufferAddress, buffer, 0, bufferSize);
        
        // Check if data was successfully read from the temporary buffer to the real buffer.
        if(bytesRead <= 0)
            return -1;
        
        // Return the number of bytes read from the file to the buffer.
        return bytesRead;
    	
    }
    
    /**
     *
     * Written by: Nimitt Tripathy on 21 March 2018 <p>
     *
     * The write system call takes in a file descriptor and return the number of bytes to be written. 
     * It also takes in a file position pointer that is incremented by the number of bytes written when the call is successful. 
     * An error is expected when the number of bytes requested is larger than the number of bytes written. 
     * The call will calculate this by adding the number of bytes written towards the file position. 
     * If the file descriptor or buffer is invalid, -1 is returned, and making the file position undefined. 
     * Note that a write to a stream can block if kernel queues are full,
     * and therefore the streams can be terminated with regards to remote host.
     *  
     * @param a0 : The syscall argument represents the fileDescriptor.
     * @param a1 : The syscall argument represents the buffer address.
     * @param a2 : The syscall argument represents the buffer size.
     * @return : The number of bytes written to the file from the buffer, or "error".
     */
    private int handleWrite(int a0, int a1, int a2)
    {
        // a0 contains the fileDescriptor.
        int fileDescriptor = a0;
        
        // Before proceeding, check if the fileDescriptor is valid.
        if(fileDescriptor < 0 || fileDescriptor >= maxOpenFiles)
                return -1;
        
        // Make sure there is a file there.
        if(files[fileDescriptor] == null)
            return -1;
        
        // a1 contains the buffer address.
        int bufferAddress = a1;
        
        // a2 contains the buffer size.
        int bufferSize = a2;
        
        // Create a temporary buffer.
        byte[] buffer = new byte[bufferSize];
        
        // Write from the real buffer onto the temporary buffer.
        int bytesWritten = readVirtualMemory(bufferAddress, buffer);
        
        // Check if data was successfully written from the real buffer to the temporary buffer.
        if(bytesWritten <= 0)
            return -1;
        
        // Write from the temporary buffer on to the file.
        bytesWritten = files[fileDescriptor].write(buffer, 0, bufferSize);
        
	// Return the number of bytes written from the buffer to the file.
    	return bytesWritten;
    }
    
    /**
     * Written by: Jeff Foreman on 24 March 2018 <p>
     * 
     * The close system call takes in a fileDescriptor, and closes this file using the 
     * stubFileSystem. It then frees up the fileDescriptor for other use.
     * 
     * @param a0 : The syscall argument represents the fileDescriptor.
     * @return : Success or "error".
     */
    private int handleClose(int a0)
    {
        // Get the fileDescriptor from the syscall argument.
        int fileDescriptor = a0;
        
        // Make sure it is valid.
        if(fileDescriptor < 0 || fileDescriptor >= maxOpenFiles)
            return -1;
        
        // Make sure there is even a file to close.
        if(files[fileDescriptor] ==  null)
            return -1;
        
        // Close the file in the stubFileSystem.
        files[fileDescriptor].close();
        
        // Free up the fileDescriptor.
        files[fileDescriptor] = null;
        
        // The file was successfully closed.
        return 0;
        
    } // End close().
    
    /**
     * Written by: Jeff Foreman on 24 March 2018 <p>
     * 
     * The unlink system call takes in a file name. 
     * If the file is found, and is not currently open, then the file is deleted immediately and returns success. 
     * If it is open, then this call will wait until it is no longer being referred to. 
     * If the file was not found, then it returns an error.
     *  
     * @param a0 : The syscall argument represents the file name.
     * @return : Success or Fail.
     */
    private int handleUnlink(int a0)
    {
        // Get the file name from the syscall argument.
        String filename = readVirtualMemoryString(a0, 256);
        
        // Make sure it is valid.
        if(filename == null)
            return -1;
        
        // Remove the file using the stubFileSystem.
        boolean status = ThreadedKernel.fileSystem.remove(filename);
        
        // Check if the file was successfully removed.
        if(status == false)
            return -1;
        
        // The file was successfully unlinked.
        return 0;
    }
   
    /* If inputs are negative, returns -1
     * @return child processID
     */
    private int handleExec(int file, int argc, int argv){
        if(file < 0 || argc < 0 || argv < 0)
            return -1;
        
        String filename = readVirtualMemoryString(file, 256);
        
        if(filename == null)
            return -1;
        
        String args[] = new String[argc];
        
        int receivedByte, argAddress;
        byte temp[] = new byte[4];
        for(int i = 0; i < argc; i++)
        {
            receivedByte = readVirtualMemory(argv*i+4, temp);
            if(receivedByte != 4)
                return -1;
                argAddress = Lib.bytesToInt(temp, 0);
            args[i] = readVirtualMemoryString(argAddress, 256);
            
            if(args[i] == null)
                return -1;
        }
        
        UserProcess child = UserProcess.newUserProcess();
        childProcess newProcessData = new childProcess(child);
        child.myChildProcess = newProcessData;
        
        if(child.execute(filename, args))
        {
            map.put(child.process_id, newProcessData);
            return child.process_id;
        }
        
        return -1;
        
    }     
    
    private int handleJoin(int pid, int status){
        if(pid < 0 || status<0)
            return -1;
        childProcess childData;
        if(map.containsKey(pid))
            childData = map.get(pid);
        else
            return -1;
        childData.child.thread.join();
        map.remove(pid);
        if(childData.status != arb)
        {
            byte exitStatus[] = new byte[4];
            exitStatus = Lib.bytesFromInt(childData.status);
            int byteTransferred = writeVirtualMemory(status, exitStatus);
            if(byteTransferred == 4)
                return 1;
            else
                return 0;
        }
        return 0;
    }
    
    
    private void handleExit(int status){
        if(myChildProcess != null)
            myChildProcess.status = status;
        for(int i = 0; i<16; i++)
            handleClose(i);
        
        this.unloadSections();
        
        if(this.process_id==ROOT)
            Kernel.kernel.terminate();
        
        else{
            KThread.finish();
            Lib.assertNotReached();
        }
    }
            


    private static final int
        syscallHalt = 0,
    syscallExit = 1,
    syscallExec = 2,
    syscallJoin = 3,
    syscallCreate = 4,
    syscallOpen = 5,
    syscallRead = 6,
    syscallWrite = 7,
    syscallClose = 8,
    syscallUnlink = 9,
    syscallConnect = 11,
    syscallAccept = 12;
  	

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     *                              </tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *                              </tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *                              </tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param   syscall the syscall number.
     * @param   a0  the first syscall argument.
     * @param   a1  the second syscall argument.
     * @param   a2  the third syscall argument.
     * @param   a3  the fourth syscall argument.
     * @return  the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) 
    {
        // Switch through syscall cases, and call the respective syscall handler.
        switch (syscall) 
        {
            case syscallHalt:
                return handleHalt();
                
            case syscallCreate:
                return handleCreat(a0);
                
            case syscallOpen:
                return handleOpen(a0);
                
            case syscallRead:
                return handleRead(a0, a1, a2);
                
            case syscallWrite:
                return handleWrite(a0, a1, a2);
                
            case syscallClose:
                return handleClose(a0);
                
            case syscallUnlink:
                return handleUnlink(a0);
                
            case syscallExit:
                handleExit(a0);
                break;
                
            case syscallExec:
                return handleExec(a0,a1,a2);
            
            case syscallJoin:
                return handleJoin(a0, a1);
        
            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                Lib.assertNotReached("Unknown system call!");
        }
        
        return 0;
    }
    
    /*
     * END SYSCALL IMPLEMENTATIONS ===========================================================
     */

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param   cause   the user exception that occurred.
     */
    public void handleException(int cause) {
    Processor processor = Machine.processor();

    switch (cause) {
    case Processor.exceptionSyscall:
        int result = handleSyscall(processor.readRegister(Processor.regV0),
                       processor.readRegister(Processor.regA0),
                       processor.readRegister(Processor.regA1),
                       processor.readRegister(Processor.regA2),
                       processor.readRegister(Processor.regA3)
                       );
        processor.writeRegister(Processor.regV0, result);
        processor.advancePC();
        break;                     
                       
    default:
        Lib.debug(dbgProcess, "Unexpected exception: " +
              Processor.exceptionNames[cause]);
        Lib.assertNotReached("Unexpected exception");
    }
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
    
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static int arb = -999;	
	
    
}
