package org.aotorrent.client;

import org.aotorrent.common.Torrent;
import org.aotorrent.common.bencode.InvalidBEncodingException;
import org.apache.log4j.BasicConfigurator;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

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

            TorrentClient torrentClient = new TorrentClient(args[0], args[1], new InetSocketAddress("127.0.0.1", 6968));

            final Thread thread = new Thread(torrentClient);

            thread.start();
            while (thread.isAlive()) {
                Thread.sleep(10000);
                System.out.println("torrentClient = " + torrentClient);
            }
            thread.join();


        } catch (InvalidBEncodingException | IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }
}
