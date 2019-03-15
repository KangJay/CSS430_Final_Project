/** @author Ji Kang
 *  @author William Eng
 *  SuperBlock is the first block in Disk and represents the meta data of the file system overall.
 *  It'll specify the total number of blocks in the disk, the total number of inodes, and a freelist pointer to the first
 *  free block. It does this with 3 variables int totalBlocks, inodeBlocks, and freeList.
 *  These variables are mapped to the first 12 bytes in the Superblock since each of them are 
 *  represented by an integer value.
*/

public class SuperBlock
{
    private static final int _0 = 0;
	private final int DEFAULT_INODE_BLOCKS = 64;
    //Locations within the super block that shows the details of the disk
    //Offset in multiples of 4 --> 4 bytes in a int
    private final int totalBlocksLoc = 0;
    private final int totalInodeLoc = 4;
    private final int freeListLoc = 8;

    public int totalBlocks;
    public int inodeBlocks; //java.lang.NoSuchFieldError: inodeBlocks
    public int freeList;

    // Disk.blockSize = 512. Static variable
    public SuperBlock( int diskSize ) {
        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.rawread(0, superBlock); // Reads the first block from Disk into the superblock

        //Get the total from the offset locations within the superblock
        totalBlocks = SysLib.bytes2int(superBlock, totalBlocksLoc);
        inodeBlocks = SysLib.bytes2int(superBlock, totalInodeLoc);
        freeList = SysLib.bytes2int(superBlock, freeListLoc);

        if (totalBlocks == diskSize && inodeBlocks > 0 && freeList >= 2)
        {
            //The disk's contents are valid
            return;
        }
        else
        {
            // Need to format/re-format the disk
            totalBlocks = diskSize;
            format(); //Implement format
        }
    }

    public void format() {
        format(DEFAULT_INODE_BLOCKS);
    }

    //RETEST with default format later
    public void format(int numInodes) {
        inodeBlocks = numInodes;
    	//New format
    	/*byte[] newSuperBlock = new byte[Disk.blockSize];
    	SysLib.int2bytes(totalBlocks, newSuperBlock, totalBlocksLoc);
    	SysLib.int2bytes(numInodes, newSuperblock, totalInodeLoc);
        */
        for (int i = 0; i < numInodes; i++){
            Inode inode = new Inode();
            inode.flag = 0; //UNUSED in the Inode class
            inode.toDisk((short) i);
        }
        freeList = ((numInodes * 32) / Disk.blockSize);
        freeList += numInodes % 16 == 0 ? 1 : 2;
        //Free blocks need to map to the next available free block
        for (int i = freeList; i < totalBlocks; i++){
            byte[] data = new byte[Disk.blockSize];
            SysLib.int2bytes(i + 1, data, 0); //Linking free list to next blocks
            SysLib.rawwrite(i, data);
        }
        byte[] endList = new byte[Disk.blockSize]; //Last block in link shouldn't point to anything initially. 
        SysLib.int2bytes(-1, endList, 0);
        SysLib.rawwrite(totalBlocks - 1, endList);
        sync(); 
    }

    public void sync() {
        /* In order to sync the contents of the superblock to disk, it is necessary to convert all
        necessary data to byte format within a temporary byte array, which then gets written to disk*/
        
        byte[] tempSuperBlock = new byte[Disk.blockSize]; //should be 512 (required size for rawread/rawwrite)
        /* 	totalBlocks = SysLib.bytes2int(superblock, totalBlocksLoc);
        	totalInodes = SysLib.bytes2int(superBlock, totalInodeLoc);
        	freeList = SysLib.bytes2int(superBlock, freeListLoc); 		*/
        
        //public static void int2bytes( int i, byte[] b, int offset )
        SysLib.int2bytes(totalBlocks, tempSuperBlock, totalBlocksLoc);
        SysLib.int2bytes(inodeBlocks, tempSuperBlock, totalInodeLoc);
        SysLib.int2bytes(freeList, tempSuperBlock, freeListLoc);
        //tempSuperBlock has the updated contents of the current file system's state.
        SysLib.rawwrite(0, tempSuperBlock); //0th block is the super block.
        SysLib.cout("Superblock synchronized\n");
    }

    //Dequeue the top block from the free list
    public int getFreeBlock() {
        if (freeList != -1){ // No more free blocks left
            int retVal = freeList; //Save old value to return  
            byte[] data = new byte[Disk.blockSize]; 
            SysLib.rawread(retVal, data); 
            freeList = SysLib.bytes2int(data, 0); 
            SysLib.int2bytes(0, data, 0);
            SysLib.rawwrite(retVal, data);
            return retVal; 
        }
        return -1; 
    }

    //Enqueue a given block to the front of the free list
    public boolean returnBlock(int blockNum) {
        if (blockNum >= 0){
            byte[] data = new byte[Disk.blockSize];
            SysLib.int2bytes(freeList, data, 0); 
            SysLib.rawwrite(blockNum, data); 
            freeList = blockNum;
            return true;  
        } //Block number is out of bounds 
        SysLib.cerr("Invalid block number\n");
        return false; 
    }
}