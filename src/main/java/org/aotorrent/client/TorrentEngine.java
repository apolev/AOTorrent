package org.aotorrent.client;

import com.google.common.collect.Sets;
import org.aotorrent.common.Piece;
import org.aotorrent.common.Torrent;
import org.aotorrent.common.connection.PeerConnection;
import org.aotorrent.common.connection.TrackerConnection;
import org.aotorrent.common.storage.FileStorage;

import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class TorrentEngine {
    private static final int DEFAULT_PORT = 6967;

    private Set<TrackerConnection> trackerConnections = Sets.newLinkedHashSet();

    private Set<PeerConnection> peerConnections = Sets.newHashSet();

    private final List<Piece> pieces;

    private ExecutorService trackerConnectionThreads;

    private byte[] peerId = "-AO0001-000000000000".getBytes();      //TODO need to give right peerid

    private final Torrent torrent;

    private final FileStorage storage;

    private TorrentEngine(Torrent torrent) {
        this.torrent = torrent; //TODO
        this.pieces = torrent.createPieces();
        this.storage = new FileStorage(torrent, new File("."));
    }

    private void initTrackers(InetAddress ip, int port) {

        Set<URL> trackers = torrent.getTrackers();

        trackerConnectionThreads = Executors.newFixedThreadPool(trackers.size());

        for (URL trackerUrl : trackers) {
            TrackerConnection trackerConnection = new TrackerConnection(trackerUrl, torrent.getInfoHash(), peerId, ip, port);
            trackerConnectionThreads.submit(trackerConnection);
            trackerConnections.add(trackerConnection);
        }
    }

    public static TorrentEngine createTorrentEngine(Torrent torrent) {
        TorrentEngine torrentEngine = new TorrentEngine(torrent);
        torrentEngine.initTrackers(null, DEFAULT_PORT);

        return torrentEngine;
    }
}
