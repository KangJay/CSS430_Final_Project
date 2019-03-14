class FSTest extends Thread{
   
    public FSTest(){
        SysLib.cout("Running Ji Kang and William Eng's custom testing for the ThreadOS FileSystem!\n");
    }

    public void run(){
        //format
        
        if (format_test()){
            SysLib.cout("Format tests have all passed :)\n");
        } else {
            SysLib.cout("Format - FAILED\n");
        }
        SysLib.cout("=========================================================\n\n");
        //open
        SysLib.cout("Formatting block to 64 for remainder of testing...\n");
        SysLib.format(64);
        SysLib.cout("\n");
        if (open_test()){
            SysLib.cout("Open tests have all passed :)\n");
        } else {
            SysLib.cout("Open - FAILED\n");
        }
        SysLib.cout("=========================================================\n\n");
        //write
        if (write_test()){
            SysLib.cout("Write tests have all passed :)\n");
        } else {
            SysLib.cout("Write - FAILED\n");
        }
        SysLib.cout("=========================================================\n\n");
        if (close_test()){
            SysLib.cout("Close tests have all passed :)\n");
        } else {
            SysLib.cout("Close - FAILED\n");
        }
        SysLib.cout("=========================================================\n\n");
        //read
        if (read_test()){
            SysLib.cout("Read tests have all passed :)\n");
        } else {
            SysLib.cout("Read - FAILED\n");
        }
        SysLib.cout("=========================================================\n\n");
        //seek
        if (seek_test()){
            SysLib.cout("Seek tests have all passed :)\n");
        } else {
            SysLib.cout("Seek - FAILED\n");
        }
        SysLib.cout("=========================================================\n\n");
        //fsize
        if (fsize_test()){
            SysLib.cout("Fsize tests have all passed :)\n");
        } else {
            SysLib.cout("Fsize - FAILED\n");
        }
        SysLib.cout("=========================================================\n\n");
        //del
        if (del_test()){
            SysLib.cout("Delete tests have all passed :)\n");
        } else {
            SysLib.cout("Delete - FAILED\n");
        }
        SysLib.cout("=========================================================\n\n");
        SysLib.cout("\nTests done running! :3\n");
        SysLib.exit();
    }

    int totalBlocks;
    int inodeBlocks;
    int freeList; 
    int fd;
    int size; 

    public boolean format_test(){
        SysLib.cout("=====================FORMAT_TEST============================\n");
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
        SysLib.cout("====================OPEN_TEST============================\n");
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
        SysLib.close(fd);
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
        for (int i = 0; i < size; i++){
            buffer[i] = (byte)(i % 128); //Overflow
        }
    }
    public boolean write_test(){ //hewohr (4) and bloop (5)
        SysLib.cout("======================WRITE_TEST========================\n");
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
        popBuffer(314);
        SysLib.cout("Writing 314 bytes to a file... Happy pi day\n");
        if ((size = SysLib.write(4, buffer)) != 314){
            SysLib.cout( "size = " + size + " (wrong)\n" );
            return false;
        }
        SysLib.cout("File written to successfully!\n\n");
        return true;
    }

    //Test closing 4 and 5 (open)
    public boolean close_test(){
        SysLib.cout("=======================CLOSE_TEST=====================\n");
        SysLib.cout("Running close tests...\n\n");
        SysLib.cout("Closing previously opened files.. FDs 3 and 4\n\n");
        SysLib.close(4); 
        size = SysLib.write( 4, new byte[1]);
        if ( size > 0 ) {
            SysLib.cout( "writable even after closing the file\n" );
            return false;
        }
        SysLib.close(5);
        SysLib.cout("File with FD = 4 successfully closed. \n\n");
        size = SysLib.write( 5, new byte[20]);
        if ( size > 0 ) {
            SysLib.cout( "writable even after closing the file\n" );
            return false;
        }
        SysLib.cout("File with FD = 5 successfully closed. \n\n");
        return true;
    }

    public boolean read_test(){ 
        SysLib.cout("======================READ_TEST=======================\n");
        SysLib.cout("Testing reading from some files...\n\n");
        fd = SysLib.open("Bloop", "r");
        byte[] tmpBuf = new byte[16];
        popBuffer(16);
        SysLib.cout("Reading 16 bytes from a file\n");
        if ((size = SysLib.read(fd, tmpBuf)) != 16){
            SysLib.cout( "size = " + size + " (wrong)\n" );
            return false;
        }
        for (int i = 0; i < 16; i++){
            if (tmpBuf[i] != buffer[i] ) {
                SysLib.cout( "buf[" + i + "] = " + tmpBuf[i] + " (wrong)\n" );
                SysLib.close( fd );
                return false;
            }
        }
        SysLib.cout("File read from successfully and consistency check cleared!\n\n");
        
        SysLib.cout("Reading 256 bytes from a file...\n");
        tmpBuf = new byte[256];
        popBuffer(256); 
        
        if ((size = SysLib.read(fd, tmpBuf)) != 256){
            SysLib.cout( "size = " + size + " (wrong)\n" );
            return false;
        }
        for (int i = 0; i < 256; i++){
            if (tmpBuf[i] != buffer[i] ) {
                SysLib.cout( "buf[" + i + "] = " + tmpBuf[i] + " (wrong)\n" );
                SysLib.close( fd );
                return false;
            }
        }
        SysLib.cout("File read from successfully and consistency check cleared!\n\n");

   
        SysLib.cout("Reading 314 bytes from a file... Happy pi day\n");
        popBuffer(314);
        tmpBuf = new byte[314];
        if ((size = SysLib.read(fd, tmpBuf)) != 314){
            SysLib.cout( "size = " + size + " (wrong)\n" );
            return false;
        }
        for (int i = 0; i < 314; i++){
            if (tmpBuf[i] != buffer[i] ) {
                SysLib.cout( "buf[" + i + "] = " + tmpBuf[i] + " (wrong)\n" );
                SysLib.close( fd );
                return false;
            }
        }
        SysLib.cout("File read from successfully and consistency check cleared!\n\n");

        return true;
    }


    public boolean seek_test(){
        SysLib.cout("======================SEEK_TEST=======================\n");
        SysLib.cout("Testing seek boundaries\n");
        fd = SysLib.open("Bloop", "r");

        SysLib.cout("Testing setting seek pointer to a negative...\n");
        int position = SysLib.seek(fd, -5, 0);
        if (position != 0){
            SysLib.cout("seek(fd, -5, 0) = " + position + " (wrong)\n");
            SysLib.cout("Should be clamped at 0\n");
            SysLib.close(fd);
        }
        SysLib.cout("Passed! Seekpointer = " + position + "\n");
        SysLib.cout("Running consistency checks on reading from an offset of 25 from beginning of the file\n");
        position = SysLib.seek(fd, 25, 0);
        byte[] tmpBuf = new byte[5]; 
        size = SysLib.read(fd, tmpBuf); 
        for (int i = 0; i < 5; i++){
            if (tmpBuf[i] != i + 9){
                SysLib.cout("Read consistency failed...\n");
                SysLib.cout("tmpBuf[" + i + "] should be: " + (i + 9) + " but was: " + tmpBuf[i] + "\n");
                SysLib.close(fd);
                return false;
            }
        }
        SysLib.cout("Consistency checks passed!\n\n");

        SysLib.cout("Checking capping seekpointer passed file boundary... should be the size of the file\n");
        SysLib.cout("\nSeeking file pointer to 9999999 from whence = 0. Definitely past file boundary.\n");
        position = SysLib.seek(fd, 99999999, 0);
        if (position == 99999999){
            SysLib.cout("Invalid seek pointer index... Went to 99999999 yikes. Clamp at file size\n");
            SysLib.close(fd);
            return false;
        }
        SysLib.cout("Seek pointer = EOF which is = " + position + "\n");
        SysLib.cout("Passed EOF clamping test!\n\n");
        return true;
    }


    public boolean fsize_test(){
        SysLib.cout("=====================FSIZE_TEST========================\n");
        SysLib.cout("Previous test showed the file size of FD = 4 was 586. We'll test that :)\n");
        size = SysLib.fsize(fd);
        if (size != 586){
            SysLib.cout("Size invalid... Should be 586. Was " + size + "\n");
        }
        SysLib.close(fd);
        SysLib.cout("Correct size!\n\n");
        return true;
    }


    public boolean del_test(){
        SysLib.cout("=====================DELETE TEST=======================\n");
        SysLib.cout("Testing delete\n");
        SysLib.cout("Creating and deleting file 'ugh' :'(\n'");
        fd = SysLib.open("ugh", "w+");
        if (fd < 0){
            SysLib.cout("Couldn't successfully create file...\n");
            return false; 
        }
        SysLib.cout("Created file successfully... now closing + deleting\n");
        SysLib.close(fd);
        SysLib.delete("ugh");
        fd = SysLib.open("ugh", "r");
        if (fd > 0){
            SysLib.cout("File still exists... WRONG\n");
            SysLib.close(fd);
            return false; 
        }
        SysLib.cout("Deleted successfully!\n\n");
        return true; 
    }
}