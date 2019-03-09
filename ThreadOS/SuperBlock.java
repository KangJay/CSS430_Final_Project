/**

*/

public class Superblock
{
    private final int DEFAULT_INODE_BLOCKS = 64;
    //Locations within the super block that shows the details of the disk
    //Offset in multiples of 4 --> 4 bytes in a int
    private final int totalBlocksLoc = 0;
    private final int totalInodeLoc = 4;
    private final int freeListLoc = 8;

    public int totalBlocks;
    public int totalInods;
    public int freeList;

    // Disk.blockSize = 512. Static variable
    public SuperBlock( int diskSize )
    {
        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.rawread(0, superBlock); // Reads the first block from Disk into the superblock

        //Get the total from the offset locations within the superblock
        totalBlocks = SysLib.bytes2int(superblock, totalBlocksLoc);
        totalInodes = SysLib.bytes2int(superBlock, totalInodeLoc);
        freeList = SysLib.bytes2int(superBlock, freeListLoc);

        if (totalBlocks == diskSize && totalInodes > 0 && freeList >= 2)
        {
            //The disk's contents are valid
            return;
        }
        else
        {
            // Need to format/re-format the disk
            totalBlocks = diskSize;
            format(DEFAULT_INODE_BLOCKS); //Implement format
        }
    }

    //Need to figure this one out.
    public void format(int numInodes)
    {
    	//New format
    	byte[] newSuperBlock = new byte[Disk.blockSize];
    	SysLib.int2bytes(totalBlocks, newSuperBlock, totalBlocksLoc);
    	SysLib.int2bytes(numInodes, newSuperblock, totalInodeLoc);
    	//Find the first free block then call int2bytes into the buffer with freeListLoc

    }

    public void sync()
    {
        /* In order to sync the contents of the superblock to disk, it is necessary to convert all
        necessary data to byte format within a temporary byte array, which then gets written to disk*/
        
        byte[] tempSuperBlock = new byte[Disk.blockSize] //should be 512 (required size for rawread/rawwrite)
        /* 	totalBlocks = SysLib.bytes2int(superblock, totalBlocksLoc);
        	totalInodes = SysLib.bytes2int(superBlock, totalInodeLoc);
        	freeList = SysLib.bytes2int(superBlock, freeListLoc); 		*/
        
        //public static void int2bytes( int i, byte[] b, int offset )
        SysLib.int2bytes(totalBlocks, tempSuperBlock, totalBlocksLoc);
        SysLib.int2bytes(totalInodes, tempSuperBlock, totalInodeLoc);
        SysLib.int2bytes(freeList, tempSuperBlock, freeListLoc);
        //tempSuperBlock has the updated contents of the current file system's state.
        SysLib.rawwrite(0, tempSuperBlock); //0th block is the super block.
    }
}