package org.aotorrent.common.connection;

import org.aotorrent.client.TorrentEngine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class PeerConnection implements Runnable {

    private boolean choking = true;
    private boolean interested = false;
    private boolean peerChoking = true;
    private boolean peerInterested = false;

    private final TorrentEngine torrentEngine;

    private final Socket socket;

    public PeerConnection(InetSocketAddress socketAddress, TorrentEngine torrentEngine) throws IOException {
        this.torrentEngine = torrentEngine;

        socket = new Socket(socketAddress.getAddress(), socketAddress.getPort());
    }

    @Override
    public void run() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    //TODO
}
