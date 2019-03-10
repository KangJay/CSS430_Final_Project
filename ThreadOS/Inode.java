/**

*/


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

    private final static int UNUSED = 0;
    private final static int USED = 1; 
    private final static int READ = 2; 
    private final static int WRITE = 3; 
    private final static int DELETE = 4;

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

    /**
	
    */
    public int toDisk(short iNumber){
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

        for (int i = 0; i < directSize; i++){
        	SysLib.short2bytes(direct[i], data, offset);
        	offset += 2; 
        }
        SysLib.short2bytes(indirect, data, offset);
        SysLib.rawwrite(blockNum, data);
        return 0; 
    }
}