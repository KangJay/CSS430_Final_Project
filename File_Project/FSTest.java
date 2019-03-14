class FSTest extends Thread{
   
    public FSTest(){
        SysLib.cout("Running Ji Kand William Eng's custom testing for the ThreadOS FileSystem!\n");
    }

    public void run(){
        //format
        /*
        if (format_test()){
            SysLib.cout("Format tests have all passed :)\n");
        }*/
        //open
        if (open_test()){
            SysLib.cout("Open tests have all passed :)\n");
            SysLib.cout("=========================================================\n\n");
        }
        //read
        if (read_test()){
            SysLib.cout("Read tests have all passed :)\n");
            SysLib.cout("=========================================================\n\n");
        }
        //write
        if (write_test()){
            SysLib.cout("Write tests have all passed :)\n");
            SysLib.cout("=========================================================\n\n");
        }

        if (close_test()){

        }
        //seek
        if (seek_test()){
            SysLib.cout("Seek tests have all passed :)\n");
            SysLib.cout("=========================================================\n\n");
        }
        //close
        if (close_test()){
            SysLib.cout("Close tests have all passed :)\n");
            SysLib.cout("=========================================================\n\n");
        }
        //fsize
        if (fsize_test()){
            SysLib.cout("Fsize tests have all passed :)\n");
            SysLib.cout("=========================================================\n\n");
        }
        //del
        if (del_test()){
            SysLib.cout("Delete tests have all passed :)\n");
            SysLib.cout("=========================================================\n\n");
        }
        SysLib.cout("\nTests done running!\n");
        SysLib.exit();
    }

    int totalBlocks;
    int inodeBlocks;
    int freeList; 
    int fd;
    int size; 

    public boolean format_test(){
        SysLib.cout("=========================================================\n");
        SysLib.cout("Running format tests on various inputs. May take a while... Sorry...\n");
        byte[] superblock = new byte[512]; //Reuse this buffer for the rest of the testing
        SysLib.cout("\nFormatting with 16 files...\n");
        SysLib.format(16); 
        SysLib.rawread(0, superblock);
        totalBlocks = SysLib.bytes2int( superblock, 0 );
        inodeBlocks = SysLib.bytes2int( superblock, 4 );
        freeList = SysLib.bytes2int( superblock, 8 );
        if ( totalBlocks != 1000 ) {
            SysLib.cout( "totalBlocks = " + totalBlocks + " (wrong)\n" );
            return false;
        }
        if ( inodeBlocks != 16 && inodeBlocks != 16 / 16 ) {
            SysLib.cout( "inodeBlocks = " + inodeBlocks + " (wrong)\n" );
            return false;
        }
        if ( freeList != 1 + 16 / 16 && freeList != 1 + 16 / 16 + 1 ) {
            SysLib.cout( "freeList = " + freeList + " (wrong)\n" );
            return false;
        }
        SysLib.cout("Formatting with 16 files done successfully... :)\n");
        
        SysLib.cout("Formatting with 28 files, not cleanly divisible by 16\n");
        SysLib.format(28);
        SysLib.rawread( 0, superblock ); 
        totalBlocks = SysLib.bytes2int( superblock, 0 );
        inodeBlocks = SysLib.bytes2int( superblock, 4 );
        freeList = SysLib.bytes2int( superblock, 8 );
        if ( totalBlocks != 1000 ) {
            SysLib.cout( "totalBlocks = " + totalBlocks + " (wrong)\n" );
            return false;
        }
        if ( inodeBlocks !=  28) {
            SysLib.cout( "inodeBlocks = " + inodeBlocks + " (wrong)\n" );
            return false;
        }
        if ( freeList != 2 + 28 / 16) { //Wastes one block so next index
            SysLib.cout( "freeList = " + freeList + " (wrong)\n" );
            return false;
        }
        SysLib.cout("Formatting with 28 files done successfully... :)\n");
        SysLib.cout("\n\n");
        return true; 
    }

    public boolean open_test(){ //0, 1, 2 dedicated to stdin, out, err
        SysLib.cout("=========================================================\n");
        SysLib.cout("Running Open tests...\n\n");
        SysLib.cout("Opening a file with 'w'. File descriptor should be equal to 3\n");
        fd = SysLib.open("Helloooo", "w");
        if ( fd != 3 ) {
            SysLib.cout( "fd = " + fd + " (wrong)\n" );
            return false;
        }
        SysLib.cout("Successfully opened the file with 'w'\n\n");

        SysLib.cout("Opening a file with 'w+', File descriptor should be equal to 4\n");
        fd = SysLib.open("Bloop", "w+");  
        if ( fd != 4 ) {
            SysLib.cout( "fd = " + fd + " (wrong)\n" );
            return false;
        }
        SysLib.cout("Successfully opened the file with 'w+'\n\n");
        SysLib.cout("Opening a file with 'a', File descriptor should be equal to 5\n");
        fd = SysLib.open("hewohr", "a");
        if ( fd != 5 ) {
            SysLib.cout( "fd = " + fd + " (wrong)\n" );
            return false;
        }
        SysLib.cout("Successfully opened the file with 'a'\n\n");
        SysLib.cout("Opening an existing file with 'r'. \nFile descriptor should be equal to 3 (Closed the first file earlier...)\n");
        SysLib.close(3);
        fd = SysLib.open("Helloooo", "r");
        if ( fd != 3 ) {
            SysLib.cout( "fd = " + fd + " (wrong)\n" );
            return false;
        }
        SysLib.cout("Successfully read from an existing file with 'r'\n\n");
        SysLib.cout("Attempting to read from a file that doesn't exist. \nFile descriptor should be some error value (Negative)\n");
        fd = SysLib.open("BunchofRandomnonsense", "r");
        if (fd > 0){
            SysLib.cout( "fd = " + fd + " (wrong)\n" );
            return false;
        }
        fd = 4;
        SysLib.cout("Couldn't read from a nonexisting file... Which is expected :)\n\n");
        return true;
    }

    byte[] buffer; 

    private void popBuffer(int size){
        buffer = new byte[size]; 
        for (byte i = 0; i < size; i++){
            buffer[i] = i;
        }
    }
    public boolean write_test(){ //hewohr (4) and bloop (5)
        SysLib.cout("=========================================================\n");
        SysLib.cout("Testing writing to some files...\n\n");
        popBuffer(16); 
        SysLib.cout("Writing 16 bytes to a file...\n");
        System.out.println(buffer.length);
        size = SysLib.write(4, buffer);
        if (size != 16){
            SysLib.cout( "size = " + size + " (wrong)\n" );
            return false;
        }
        SysLib.cout("File written to successfully!\n\n");
        SysLib.cout("Writing 256 bytes to a file...\n");
        popBuffer(256); 
        
        if ((size = SysLib.write(4, buffer)) != 256){
            SysLib.cout( "size = " + size + " (wrong)\n" );
            return false;
        }
        SysLib.cout("File written to successfully!\n\n");
        return true;
    }

    public boolean read_test(){ 
        SysLib.cout("=========================================================\n");
        

        return false;
    }

    //Test closing 4 and 5 (open)
    public boolean close_test(){
        SysLib.cout("=========================================================\n");
        SysLib.cout("Running close tests...\n\n");

        return false;
    }


    public boolean seek_test(){
        SysLib.cout("=========================================================\n");
        

        return false;
    }


    public boolean fsize_test(){
        SysLib.cout("=========================================================\n");
        

        return false;
    }


    public boolean del_test(){
        SysLib.cout("=========================================================\n");
        

        return false; 
    }
}