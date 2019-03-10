import java.util.Vector;
public class FileTable {  // Each table entry should have
    
    private Vector table;   //The actual entity o this file table
    private Directory dir;  //Root directory

    //Inode flag mappings
    private final static int UNUSED = 0;
    private final static int USED = 1; 
    private final static int READ = 2; 
    private final static int WRITE = 3; 
    private final static int DELETE = 4;

    public FileTable( Directory directory ) {   //Constructor
        table = new Vector();   //Instantiate a file (structure) table
        dir = directory;        //Receive a reference to the Directory from the file system
    }

    //major public methods
    public synchronized FileTableEntry falloc( String filename, String mode ){
        // Allocate a new file (structure) table entry for this file name 
        short iNumber; 
        Inode inode = null; 
        /*
        NOTE FOR FUTURE DEVELOPMENT: Get/create the associated inode. If the inode 
        existed prior to the falloc call, it means it may be in use (written to/read from)
        so check inode.flag == USED/UNUSED and WIRTE/READ to see if it's safe to allow another
        process to access this FileTableEntry. If it's in read only, it's safe. If it's being 
        written to, call wait() on the call. --> Call notify() when a writing process is done writing. 
        
        */

        // Allocate/retrieve and register the corresponding inode using dir
        while (true) { //wait() / notify() calls dependent on this while-loop
        	iNumber = (filename.equals("/") ? 0 : dir.namei(filename)); //Either root or needs to grab associated filename
            //System.out.println(iNumber);
            if (iNumber >= 0){ //File already open
                inode = new Inode(iNumber);
        		if (mode.equals("r")){
        			if (inode.flag == READ){
        				inode.flag = READ;
        				break;
        			} else if (inode.flag == UNUSED || inode.flag == USED){
                        inode.flag = READ;
                        break; 
                    } else if (inode.flag == WRITE){
                        //File is currently being written to
                        try{ 
                            wait();
                        } catch (InterruptedException e) {}
        			} 
                } else if (mode.equals("w") || mode.equals("w+") || mode.equals("a")){
                    //Only one write/append can be done at any given time. 
                    if (inode.flag == WRITE || inode.flag == READ){
                        // Another write/read is occurring. need to wait for it to finish. 
                        try{
                            wait();
                        } catch (InterruptedException e) {}
                    } else if (inode.flag == USED || inode.flag == UNUSED){
                        inode.flag = WRITE; 
                        break; 
                    } else {
                        inode.flag = WRITE; 
                        break;
                    }
                }
            }
            if (!(mode.equals("r"))) { //Means file doesn't exist. Need to create a new one and allocate. 
                inode = new Inode();
                iNumber = dir.ialloc(filename);
                inode.flag = WRITE; // Not read. Could be w, w+, or a
            } else {
                return null; //Delete ??
            }
        }
        
        // Increment this inodes' count
        inode.count++; 
        // Immediately write back this inode to the disk
        inode.toDisk(iNumber);
        // Return a reference to this file (structure) table entry
        FileTableEntry ftentry = new FileTableEntry(inode, iNumber, mode); 
        table.addElement(ftentry);
        return ftentry;
    } 

    public synchronized boolean ffree ( FileTableEntry e ){
        // Receieve a file table entry reference 
        if (table.removeElement(e)){ 
        	//Was found in the table
            e.inode.count--; 
            switch(e.inode.flag){
                case READ: {
                    e.inode.flag = USED;
                }
                case WRITE: {
                    e.inode.flag = USED; 
                } 
            }
            // Save the corresponding inode to the disk 
            e.inode.toDisk(e.iNumber);
            // Free this file table entry ??
            e = null; 
            // Returns true if this file table entry is found in my table
        	notify();
        	return true; 
        }
        return false; //Wasn't found in table. 
    }

    public synchronized boolean fempty( ){
        return table.isEmpty(); //Should be called before starting a format
    }
/*
    private short getMode(String mode){
        if (mode.equalsIgnoreCase("r")){            // Read from an existing file
            return READ; 
        } else if (mode.equalsIgnoreCase("w")){     // Write to an existing
            return WRITE;
        } else if (mode.equalsIgnoreCase("w+")){    // Create if it doesn't exist + write
            return WRITE_PLUS;
        } else if (mode.equalsIgnoreCase("a")){     // Append
            return APPEND; 
        }
        return -1; //Not found in access mdoes 
    }*/
}


