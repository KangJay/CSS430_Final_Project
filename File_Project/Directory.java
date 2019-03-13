
public class Directory {

    private static int maxChars = 30;
    private int[] fsize;       //Each element stores a different file size
    private char fnames[][];    //Each element stores a different file name

    public Directory(int maxInumber){   //Directory constructor
        fsize = new int[maxInumber];   //MaxInumber = max number of files allowed in the system
        for (int i = 0; i < maxInumber; i++){
            fsize[i] = 0; //All file sizes initialized to 0 bytes
        }
        fnames = new char[maxInumber][maxChars];
        String root = "/";  // Entry inode 0 is root ("/")
        fsize[0] = root.length();  // fsize[0] is the size of "/"
        root.getChars(0, fsize[0], fnames[0], 0); //fnames[0] includes "/"
    }

    public void bytes2directory(byte data[]){
        //Assumes data[] received directory information from disk
        if (data == null || data.length == 0) {
             SysLib.cerr("Invalid data to directory formatting\n");
            return;
        }
        //Initializes the Directory instance with this data[]
        //Increment by 4. Each inode/file entry's length variable is the first 4 bytes of the object. 
        int offset = 0;
        for (int i = 0; i < fsize.length; i++, offset += 4){
            fsize[i] = SysLib.bytes2int(data, offset);
        }
        for (int i = 0; i < fnames.length; i++, offset += 60){
            String filename = new String(data, offset, 60); 
            filename.getChars(0, fsize[i], fnames[i], 0); //String contents get 'char array'd' into fnames[i]
        }
    }

    public byte[] directory2bytes() {
        //Directory needs to hold the lengths of the files + names in bytes
        //length is in ints so = 4 * length bytes
        //maxChars is 30 chars, each 2 bytes, with fnames.length amount. 
        int directorySize = (fsize.length * 4) + (fnames.length * maxChars * 2);
        byte[] directory = new byte[directorySize];
        //Write out the sizes
        int offset = 0;
        for (int i = 0; i < fsize.length; i++, offset += 4){
            SysLib.int2bytes(fsize[i], directory, offset);
        } 
        //Write out the file names
        //https://docs.oracle.com/javase/7/docs/api/java/lang/String.html
        for (int i = 0; i < fnames.length; i++){
            String temp = new String(fnames[i], 0, fsize[i]); //Copy fnames chars into a string of length fsize[i]
            byte[] data = temp.getBytes(); //Convert string into byte form. 
            //https://www.geeksforgeeks.org/system-arraycopy-in-java/
            System.arraycopy(data, 0, directory, offset, data.length);
            offset += 60; 
        }
        return directory;
    }

    public short ialloc(String filename) {
        for (short i = 0; i < fsize.length; i++){ //int --> short = lossy conversion
            if (fsize[i] == 0) { //Empty slot
                fsize[i] = Math.min(maxChars, filename.length());
                filename.getChars(0, fsize[i], fnames[i], 0);
                return i; //Return index number
            }
        }
        return -1; // No free slots
    }

    public boolean ifree(short iNumber){
        if (iNumber < 0){
            SysLib.cerr("Directory.ifree iNumber value: " + iNumber + " is invalid!\n");
            return false; 
        }
        if (fsize[iNumber] > 0) { //If the file exists at this index, clear it
            fsize[iNumber] = 0;
            return true;
        }
        return false; 
    }

    public short namei(String filename) { //Map name to i-number
        for (short i = 0; i < fsize.length; i++){
            if (fsize[i] == filename.length()){
                String temp = new String(fnames[i], 0, fsize[i]);
                if (temp.equals(filename)){
                    return i;
                }
            }
        }   
        return -1;
    }
}