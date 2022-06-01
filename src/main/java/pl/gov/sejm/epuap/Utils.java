package pl.gov.sejm.epuap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility methods
 *
 */
public class Utils {

    /**
     * Extracts a zip file to a specified directory.
     * @param file a file to extract
     * @param tempDir a temp directory
     * @throws IOException
     */
    public static void extractZip(Path baseDir, File file) throws IOException {
        FileInputStream is = new FileInputStream(file);
        extractZip(baseDir, is);
    }
    
    /**
     * Extracts a zip file to a specified directory.
     * @param stream a stream of bytes to extract
     * @param tempDir a temp directory
     * @throws IOException
     */
    public static void extractZip(Path baseDir, InputStream stream) throws IOException {
    	extractZip(baseDir, stream, Charset.defaultCharset());    	
    }
    
    /**
     * Extracts a zip file to a specified directory.
     * @param stream a stream of bytes to extract
     * @param charset 
     * @param tempDir a temp directory
     * @throws IOException
     */
    public static void extractZip(Path baseDir, InputStream stream, Charset charset) throws IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(stream, charset);
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(baseDir, zipEntry);
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();        
    }
    
    /**
     * Creates a new file based on a zip entry.
     * @param destinationDir a directory where to create a file
     * @param zipEntry a zip entry used to get name of the file
     * @return a file
     * @throws IOException
     */
    private static File newFile(java.nio.file.Path destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir.toFile(), zipEntry.getName());
         
        String destDirPath = destinationDir.toFile().getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
         
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
         
        return destFile;
    }

}
