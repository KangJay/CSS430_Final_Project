
public class FileTable {  // Each table entry should have
    
    private Vector table;   //The actual entity o this file table
    private Directory dir;  //Root directory

    private final static int READ = 0; 
    private final static int WRITE = 1;
    private final static int WRITE_PLUS = 2; 
    private final static int APPEND = 4; 

    public FileTable( Directory directory ) {   //Constructor
        table = new Vector();   //Instantiate a file (structure) table
        dir = directory;        //Receive a reference to the Directory from the file system
    }

    //major public methods
    public synchronized FileTableEntry falloc( String filename, String mode ){
        // Allocate a new file (structure) table entry for this file name 
        short accessMode = getMode(mode); 

        /*
        NOTE FOR FUTURE DEVELOPMENT: Get/create the associated inode. If the inode 
        existed prior to the falloc call, it means it may be in use (written to/read from)
        so check inode.flag == USED/UNUSED and WIRTE/READ to see if it's safe to allow another
        process to access this FileTableEntry. If it's in read only, it's safe. If it's being 
        written to, call wait() on the call. --> Call notify() when a writing process is done writing. 
        
        */

        // Allocate/retrieve and register the corresponding inode using dir
        // Increment this inodes' count
        // Immediately write back this inode to the disk
        // Return a reference to this file (structure) table entry
    } 

    public synchronized boolean ffree ( FileTableEntry e ){
        // Receieve a file table entry reference 
        // Save the corresponding inode to the disk 
        // Free this file table entry
        // Returns true if this file table entryu is found in my table
    }

    public synchronized boolean fempty( ){
        return table.isEmpty(); //Should be called before starting a format
    }

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
    }
}


