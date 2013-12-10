package org.aotorrent.client;

import org.aotorrent.common.Torrent;
import org.aotorrent.common.bencode.InvalidBEncodingException;

import java.io.*;

/**
 * User: dnapolov
 * Date: 11/22/13
 * Time: 1:41 PM
 */
public class SingleTorrentClient {
    public static void main(String[] args) {
        String filename = "torrent.torrent"; //args[1];
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(filename));
            Torrent torrent = new Torrent(is);
            TorrentEngine torrentEngine = new TorrentEngine(torrent);

        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InvalidBEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }
}
