class FileSystem{

    private SuperBlock superblock;
    private Directory directory; 
    private FileTable filetable; 


    public static final int FS_ERROR = -1; 

    public FileSystem(int diskBlocks){
        //Create a new supoerblock and format disk with 64 inodes in default. 
        superblock = new SuperBlock(diskBlocks);

        //Create directory and register "/" in directory entry 0
        directory = new Directory(superblock.inodeBlocks);
        
        //File table is created and store directory in the file table
        filetable = new FileTable(directory);

        FileTableEntry dirEnt = open("/", "r"); 
        int dirSize = fsize(dirEnt); 
        if (dirSize > 0){ //Root directory already has files populated
            byte[] dirData = new byte[dirSize]; 
            read(dirEnt, dirData); 
            directory.bytes2directory(dirData);
        }
        close(dirEnt);
    }

    public void sync(){
        FileTableEntry ftEnt = open("/", "w"); //open root directory
        //copy over current data of directory to disk
        byte[] data = directory.directory2bytes();
        //update disk contents 
        write(ftEnt, data); 
        close(ftEnt); //Remove this file table entry from pointing to the file
        superblock.sync(); //Updates the disk with latest inode/meta data information
    }

    public boolean format(int files){
        //What if there's files currently open?
        if (!filetable.fempty()){
            SysLib.cerr("Files currently open!\n");
            return false; 
        }
        superblock.format(files);
        directory = new Directory(superblock.inodeBlocks);
        filetable = new FileTable(directory);  
        return true; 
    }

    public FileTableEntry open(String filename, String mode){
        FileTableEntry perProcTbEnt = filetable.falloc(filename, mode);
        if ((mode.equals("w")) && !deallocAllBlocks(perProcTbEnt)){
            return null; 
        }
        return perProcTbEnt; 
    }

    public boolean close(FileTableEntry ftEnt){
        synchronized(ftEnt){ //should be the only method using this variable at this given time (Race condition avoidance)
            ftEnt.count--;
            if (ftEnt.count > 0){
                return true; 
            }
            return filetable.ffree(ftEnt);
        }
    }

    public int fsize(FileTableEntry ftEnt){
        synchronized(ftEnt){
            return ftEnt.inode.length; 
        }
    }

    public int read(FileTableEntry ftEnt, byte[] buffer){
        if(ftEnt.mode.equals("w") && ftEnt.mode.equals("a")){
            SysLib.cerr("Error: FileSystem.read(). Invalid mode: " + ftEnt.mode + " in call...\n");
            return -1;
        }
        if (ftEnt == null){
            SysLib.cerr("Error: FileSystem.read(). FileTableEntry object is null\n");
            return -1; 
        }
        int totalRead = 0; //How many bytes we've read
        synchronized (ftEnt){ 
            int toRead = buffer.length; 
            int readAmount = 0;
            while(toRead > 0 && ftEnt.seekPtr < fsize(ftEnt)){ 
                int blockNum = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
                if (fsize(ftEnt) < ftEnt.seekPtr){ //Out of bounds 
                    return -1; 
                }
                if (blockNum == -1){
			        System.out.println(readAmount);
                    break; //Return -1 instead?... Read error or call error? Catch error at end of while-loop?. NOTE: Leave as is unless testing says otherwise
                }
                //Read in current block
                byte[] data = new byte[Disk.blockSize]; 
                SysLib.rawread(blockNum, data); 
                
                int offset = ftEnt.seekPtr % Disk.blockSize; //Read byte index of given block
                
                /* Amount to read in each iteration. 
                If readAmount is Disk.blockSize - offset, it's reading to the end of the block
                If readAmount is toRead, the amount ot read left to the EOF is less than a block
                If readAmount is fsize(ftEnt), it's reading to the end of the file. inode.length will be the smallest value if it reads over
                */
                readAmount = Math.min( Math.min(Disk.blockSize - offset, toRead), fsize(ftEnt) - ftEnt.seekPtr);
                //readAmount = Math.min((Disk.blockSize - offset < ))
                System.arraycopy(data, offset, buffer, totalRead, readAmount);             
                ftEnt.seekPtr += readAmount;    // Advance seekPtr by amount of bytes read
                toRead -= readAmount;           // Initially length of 'buffer'. Decrement amount left to read
                totalRead += readAmount;        // Used for index offset in System.arraycopy. Signifies amounts read in this iteration so we don't overwrite bytes. 
            }
            return totalRead;    
        }
    }

    /*
    public final static int NOT_FREE = -1;
    public final static int BAD_DIRECT_ACCESS = -2;
    public final static int BAD_INDIRECT_ACCESS = -3;
    public final static int OK = 0;
    */ 
    public int write(FileTableEntry ftEnt, byte[] buffer){
        //Set pointers accordingly if not before -- EXPERIMENTAL
        if (ftEnt.mode.equals("r")){
            SysLib.cerr("Error: FileSystem.write(). Read mode in a write call...\n");
            return -1; 
        }
        if (ftEnt.mode.equals("w")){ 
            if (deallocAllBlocks((ftEnt))){
                return -1; 
            }
        }
        int amountWritten = 0;
        //w should clear the entire thing first, w+ can continue on from seekptr
        //Implement clear first if time allots 
        synchronized(ftEnt){ //Make sure this process is the only one that can access this file entry 
            int currentByteIndex, bytesLeft, writeAmount;  
            int amountLeft = buffer.length; 
            while (amountLeft > 0){
                int blockNum = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);

                if (blockNum == -1){ //Writing past EOF CASE: 'a' and 'w+ with EOF'
                    int freeBlock = superblock.getFreeBlock();
                    //Link this block
                    switch(ftEnt.inode.registerTargetBlock(ftEnt.seekPtr, (short) freeBlock)){
                        case Inode.OK: 
                            break;  // Signals correct linking
                        case Inode.BLOCK_ERROR:
                        case Inode.MISSING_ERROR:
                            return -1;  
                        default: 
                            ftEnt.inode.registerIndexBlock((short) superblock.getFreeBlock());
                            ftEnt.inode.registerTargetBlock(ftEnt.seekPtr, (short) freeBlock);
                            break; 
                    }
                    blockNum = freeBlock; 
                }
                byte[] data = new byte[Disk.blockSize]; 
                SysLib.rawread(blockNum, data);
                /*if (ftEnt.mode.equals("w+") || ftEnt.mode.equals("a")){ //Keep current blocks, else wipe
                    SysLib.rawread(blockNum, data);
                    if (ftEnt.mode.equals("a")){ //Put seekPtr to the end of the file. 
                        ftEnt.seekPtr = fsize(ftEnt);
                    } 
                } else if (ftEnt.mode.equals("w")){ 
                    //Clear the entire file first
                    if (deallocAllBlocks((ftEnt))){
                        return -1; 
                    }

                }*/
                //Follow similar semantics to read call
                currentByteIndex = ftEnt.seekPtr % Disk.blockSize; 
                bytesLeft = Disk.blockSize - currentByteIndex;
                writeAmount = Math.min(bytesLeft, amountLeft); //to ensure we don't go over buffer size. 
                //Write from 'buffer' the index indicated by amountWritten already to 'data' 
                //at the current byte index and write 'writeAmount' number of bytes
                System.arraycopy(buffer, amountWritten, data, currentByteIndex, writeAmount); 
                SysLib.rawwrite(blockNum, data); 
                ftEnt.seekPtr += writeAmount; 
                amountLeft -= writeAmount; 
                amountWritten += writeAmount; 
                //Extend file size
                if (ftEnt.inode.length <= ftEnt.seekPtr){
                    ftEnt.inode.length = ftEnt.seekPtr; 
                }
                //Write file meta data back to disk 
                ftEnt.inode.toDisk(ftEnt.iNumber);  
            }
        }
        return amountWritten;
    }

    public boolean delete(String filename){
        FileTableEntry ftEnt = open(filename, "w"); // Get corresponding inode
        if (directory.ifree(ftEnt.iNumber) && close(ftEnt)){
            //With this file that closed along with other processes, no process has this open
            return true; 
        }
        return false; //Some files currently have this open
    }

    
    private final int SEEK_SET = 0; 
    private final int SEEK_CUR = 1; 
    private final int SEEK_END = 2;
    
    public int seek(FileTableEntry ftEnt, int offset, int whence){
        synchronized (ftEnt){
            switch(whence){
                case SEEK_SET:  //Set to offet
                    ftEnt.seekPtr = offset;     // Set as normally   
                    break; 
                case SEEK_CUR: //Current + offset
                    ftEnt.seekPtr += offset;    // Set as normally
                    break;
                case SEEK_END: // At end + offset
                    ftEnt.seekPtr = fsize(ftEnt) + offset; // Set as normal
                    break;
            }
        if (ftEnt.seekPtr < 0) {
            ftEnt.seekPtr = 0;
        } else if (ftEnt.seekPtr > fsize(ftEnt)){
            ftEnt.seekPtr = fsize(ftEnt);
        }
        return ftEnt.seekPtr;
        }
    }

    private boolean deallocAllBlocks(FileTableEntry ftEnt){
        //Reset all direct blocks
        if (ftEnt.inode.count != 1){ //Open call should be the only one pointing to it
            return false; 
        }
        for (int i = 0; i < ftEnt.inode.direct.length; i++){
            //Direct pointers currently point to data blocks. Need to reset and append to freelist
            if (ftEnt.inode.direct[i] != -1){
                superblock.returnBlock(ftEnt.inode.direct[i]);
            }
            ftEnt.inode.direct[i] = -1;
        }
        //Deallocate indirect index block if applicable
        byte[] indirectBlock = ftEnt.inode.unregisterIndexBlock();
        if (indirectBlock != null){
            //all short pointers in indirect index block have a value of -1
            //See Inode.registerIndirectBlock(...)
            int blockNum;
            //Comb through the entire index block
            for (int offset = 0; offset < Disk.blockSize; offset += 2){
                blockNum = SysLib.bytes2short(indirectBlock, offset);
                if (blockNum != -1){
                    superblock.returnBlock(blockNum);
                }
            }
        }
        ftEnt.inode.toDisk(ftEnt.iNumber);
        return true; 
    }

}
