
class FileSystem{

    private Superblock superblock;
    private Directory directory; 
    private FileTable filetable; 

    private final int SEEK_SET = 0; 
    private final int SEEK_CUR = 1; 
    private final int SEEK_END = 2;

    public FileSystem(int diskBlocks){
        //Create a new supoerblock and format disk with 64 inodes in default. 
        superBlock = new SuperBlock(diskblocks);

        //Create directory and register "/" in directory entry 0
        directory = new Directory(superblock.totalInodes);
        
        //File table is created and store directory in the file table
        filetable = new FileTable(directory); 

    }

    public void sync(){
        
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
        return true; ; 
    }

    public FileTableEntry open(String filename, String mode){
        FileTableEntry perProcTbEnt = filetable.falloc(filename, mode);
        if ((mode.equals("w")) && !deallocAllBlocks(perProcTbEnt)){
            return null; 
        }
        return perProcTbEnt; 
    }

    public boolean close(FileTableEntry ftEnt){
        synchronized(ftEnt){
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

    }

    public int write(FileTableEntry ftEnt, byte[] buffer){

    }

    public boolean delete(String filename){

    }

    public int seek(FileTableEntry ftEnt, int offset, int whence){

    }

    private boolean deallocAllBlocks(FileTableEntry ftEnt){
        if (ftEnt.inode.count != 1){
            return false; 
        }
        byte[] data = inode.
    }

}