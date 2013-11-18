package org.aotorrent.client;

import com.google.common.collect.Sets;
import org.aotorrent.common.Piece;
import org.aotorrent.common.Torrent;
import org.aotorrent.common.connection.PeerConnection;
import org.aotorrent.common.connection.TrackerConnection;

import java.net.URL;
import java.util.Date;
import java.util.Set;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class TorrentEngine {

    private Set<TrackerConnection> trackerConnections = Sets.newLinkedHashSet();
    private Set<PeerConnection> peerConnections = Sets.newHashSet();
    private Set<Piece> pieces = Sets.newHashSet();
    private byte[] peerId = "-AO0001-000000000000".getBytes();      //TODO need to give right peerid
    private final Torrent torrent;

    public TorrentEngine(Torrent torrent) {
        this.torrent = torrent; //TODO

        Set<URL> trackers = torrent.getTrackers();

        for (URL trackerUrl : trackers) {
            TrackerConnection trackerConnection = new TrackerConnection(trackerUrl, torrent.getInfoHash(), peerId, null, 6969);
            new Thread(trackerConnection).start();
            trackerConnections.add(trackerConnection);
        }
    }

    private class TrackerConnectionsKeeper implements Runnable {

        @Override
        public void run() {
            for (TrackerConnection trackerConnection : trackerConnections) {
                synchronized (trackerConnection) {
                    if (trackerConnection.getNextRequest() != null && trackerConnection.getNextRequest().before(new Date())) {
                        new Thread(trackerConnection).start();
                    }
                }
            }
        }
    }
}
