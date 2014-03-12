package org.aotorrent.client;

import org.aotorrent.common.Torrent;
import org.aotorrent.common.bencode.InvalidBEncodingException;
import org.apache.log4j.BasicConfigurator;

import java.io.*;

/**
 * User: dnapolov
 * Date: 11/22/13
 * Time: 1:41 PM
 */
public class SingleTorrentClient {
    public static void main(String[] args) {
        BasicConfigurator.configure();
        String filename = args[0];
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(filename));
            Torrent torrent = new Torrent(is, args[1]);

            TorrentEngine torrentEngine = new TorrentEngine(torrent);
            new Thread(torrentEngine).start();

        } catch (FileNotFoundException e) {

        } catch (InvalidBEncodingException e) {

        } catch (IOException e) {

        }

    }
}
