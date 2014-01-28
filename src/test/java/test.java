import org.aotorrent.common.Torrent;
import org.aotorrent.common.bencode.InvalidBEncodingException;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * User: dnapolov
 * Date: 11/6/13
 * Time: 6:56 PM
 */
public class test {
    public static void main(String[] args) throws Exception {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream("torrent.torrent");
            Torrent torrent = Torrent.create("http://yo.yo/yo", "1.exe");
            System.out.println("torrent = " + torrent + "\n\n\n\n");
            torrent.save(fileOutputStream);
        } catch (FileNotFoundException e) {
            throw new Exception(e);
        } catch (IOException e) {
            throw new Exception(e);
        } catch (InvalidBEncodingException e) {
            throw new Exception(e);
        } catch (InterruptedException e) {
            throw new Exception(e);
        } catch (ExecutionException e) {
            throw new Exception(e);
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
            }
        }
    }
}
