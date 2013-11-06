import org.aotorrent.common.Torrent;

import java.io.IOException;

/**
 * User: dnapolov
 * Date: 11/6/13
 * Time: 6:56 PM
 */
public class test {
    public static void main(String[] args) {
        try {
            Torrent torrent = Torrent.create("http://yo.yo/yo", "test.csv");
            System.out.println("torrent = " + torrent);

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
