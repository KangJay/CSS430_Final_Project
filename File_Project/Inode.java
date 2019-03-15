
/* Starting from the blocks after the superblock will be the inode blocks. Each inode describes one file. 
Our inode is a simplified version of the UNIX inode. It includes 12 pointers of the index block. 
The first 11 of these pointers point to direct blocks (Data). 
The last pointer points to an indrect block. 
Each inode must include 
1. The length of the corresponding file. 
2. Number of file table entries pointing to this inode. 
3. Flag to indicate if it's unused (= 0), used (= 1), or in some other status. 
16 inodes can be stored in one block. 
*/
public class Inode
{
    private final static int iNodeSize = 32;     // Default size is 32 bytes
    private final static int directSize = 11;    // # of direct pointers
    private final static int iNodePerBlock = 16; // 16 inodes per block

    public final static int OK = 0;                 // Everything's good
    public final static int BLOCK_ERROR = -1;       // Signals a simple index error
    public final static int MISSING_ERROR = -2;     // Not found in Inode at all
    public final static int REGISTER_ERROR = -3;    // Signals the call was invalid
    
    public int length;      // file size in bytes
    public short count;     // # of file-table entries that opened this file = open count
    public short flag;      // 0 = unused, 1 = used
    public short direct[] = new short[directSize];  //holds all direct pointers
    public short indirect;  // One indirect pointer

    public Inode() //Default constructor
    {
        length = 0; 
        count = 0; 
        flag = 1; 
        for (int i = 0; i < directSize; i++){
            direct[i] = -1; 
        }
        indirect = -1; 
    }

    /* Inode's second constructor retrieves and read the iNumber block. 
    Then it locates inode info and initializes the new inode with this info. If a inode already exists, we need to find it and copy its attributes. 
    First, refer to the directory in order to find the inode number. From this inode number, you can calculate which disk block contains the inode. 
    Read this disk block and get this inode information. Where should you store such inode information then? 
    You should instantiate an inode object first, and the nreinitialize it with the inode info retrieved from the disk. */
    public Inode(short iNumber){
        // 16 iNodes per block so we need to find the block number. 
        int blockNum = (iNumber / iNodePerBlock) + 1; //0th is superblock.
        byte[] data = new byte[Disk.blockSize]; // blockSize = 512 bytes. 
        SysLib.rawread(blockNum, data); //Read in the given block the inode is in
        // Get the iNode's bytes within a block's offset. 
        // the i-th iNode within a block * 32 bytes = starting 32-byte index of this iNode 
        int offset =  (iNumber % iNodePerBlock) * iNodeSize; 
        //Need to get the space representing the inode's length, count, and flag (4 : 2 : 2 bytes)
        length = SysLib.bytes2int(data, offset); 
        offset += 4; //Move 4 bytes since an int is 4 bytes 
        count = SysLib.bytes2short(data, offset); 
        offset += 2; //count is a short so 2 bytes 
        flag = SysLib.bytes2short(data, offset); 
        offset +=2; //Got all the instance variable's data set for this given inode. 

        //Need to get all the direct data block pointers (2 bytes each because they're shorts)
        for (int i = 0; i < directSize; i++){
            direct[i] = SysLib.bytes2short(data, offset);
            offset += 2;
        }
        indirect = SysLib.bytes2short(data, offset); //Done reading in this inode's data from disk. 
    }

   /**  @param iNumber represents the inode number
    *   Writes the given inode indicated by the iNumber to disk.     
    */
    public synchronized void toDisk(short iNumber){
   		byte data[] = new byte[Disk.blockSize];
   		int blockNum = (iNumber / iNodePerBlock) + 1;
   		SysLib.rawread(blockNum, data);
   		// Get the iNode's bytes within a block's offset. 
        // the i-th iNode within a block * 32 bytes = starting 32-byte index of this iNode 
        int offset = (iNumber % 16) * 32;
        //Need to get the space representing the inode's length, count, and flag (4 : 2 : 2 bytes)
        SysLib.int2bytes(length, data, offset); 
        offset += 4; //Move 4 bytes since an int is 4 bytes 
        SysLib.short2bytes(count, data, offset); 
        offset += 2; //count is a short so 2 bytes 
        SysLib.short2bytes(flag, data, offset); 
        offset +=2; //Got all the instance variable's data set for this given inode. 

        for (int i = 0; i < directSize; i++){ //Offset by 2 because of short = 2 bytes
        	SysLib.short2bytes(direct[i], data, offset);
        	offset += 2; 
        }
        SysLib.short2bytes(indirect, data, offset); //Write once more for indirect
        SysLib.rawwrite(blockNum, data);    //Write updated block back to memory
    }

    /** @param numBytes is
     * 
     */
    //Exception in thread "main" java.lang.NoSuchMethodError: Inode.findTargetBlock(I)
    public int findTargetBlock(int numBytes) {
        int blockNum = numBytes / Disk.blockSize;
        if (blockNum < directSize) { //11 direct pointers 0 to 10 
            return direct[blockNum];
        }
        if (indirect < 0) { //Indirect not set up
            return -1;
        }
        // else Indirect pointer has information to retrieve
        byte[] indirectIndexBlock = new byte[Disk.blockSize];
        SysLib.rawread(indirect, indirectIndexBlock);
        int indexBlockLoc = blockNum - directSize;
        return SysLib.bytes2short(indirectIndexBlock, indexBlockLoc * 2);
    }

    //Exception in thread "Thread-5" java.lang.NoSuchMethodError: Inode.registerTargetBlock(IS)I
    public int registerTargetBlock(int offset, short indexBlockNum){
        if (offset < 0){ //
            SysLib.cerr("Invalid block number\n");
            return BLOCK_ERROR;
        }
        int block = offset / Disk.blockSize; 
        if (block < 11){
            if (direct[block] >= 0){
                return BLOCK_ERROR;
            }
            if (block > 0 && direct[block - 1] == -1){
                return MISSING_ERROR; 
            }
            direct[block] = indexBlockNum;
            return OK;
        }
        //return REGISTER_ERROR;
        if (indirect < 0){ //Not in either direct or indirect pointers
            return REGISTER_ERROR; 
        }
        byte[] data = new byte[Disk.blockSize]; 
        SysLib.rawread(indirect, data);

        int targetBlock = block - directSize; 
        //Test targetBlock range?
        if (SysLib.bytes2short(data, targetBlock * 2) > 0){
            return BLOCK_ERROR;
        }
        SysLib.short2bytes(indexBlockNum, data, targetBlock * 2); 
        SysLib.rawwrite(indirect, data); 
        return OK; 
    }

    public int findIndexBlock(){
        return indirect;
    }
    //Exception in thread "Thread-5" java.lang.NoSuchMethodError: Inode.registerIndexBlock(S)Z
    public boolean registerIndexBlock(short indexBlockNum){
        if (indirect != -1){ //Index block already in use
            return false; 
        }
        //all direct blocks have to be used first before index
        for (int i = 0; i < directSize; i++){ 
            if (direct[i] < 0){ 
                return false; 
            }
        }
        indirect = indexBlockNum; //Can allocate the index block for use
        byte[] data = new byte[Disk.blockSize];
        for (int i = 0; i < Disk.blockSize / 2; i += 2){ //Short = 2 bytes, so increment by 2 bytes each time
            SysLib.short2bytes((short) -1, data, i);
        }
        SysLib.rawwrite(indirect, data);
        return true; 
    }

    //Exception in thread "Thread-5" java.lang.NoSuchMethodError: Inode.unregisterIndexBlock()[B
    public byte[] unregisterIndexBlock(){
        if (indirect >= 0){
            byte[] data = new byte[Disk.blockSize]; 
            SysLib.rawread(indirect, data);
            indirect = -1; //dereference 
            return data; 
        }
        return null;
    }
}