package org.aotorrent.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.aotorrent.common.Piece;
import org.aotorrent.common.Torrent;
import org.aotorrent.common.connection.PeerConnection;
import org.aotorrent.common.connection.TrackerConnection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class TorrentEngine implements Runnable {
    private static final int DEFAULT_PORT = 6967;

    private Set<TrackerConnection> trackerConnections = Sets.newLinkedHashSet();
    private Map<InetSocketAddress, PeerConnection> peerConnections = Maps.newHashMap();
    private final List<Piece> pieces;
    private ExecutorService trackerConnectionThreads;
    private ExecutorService peersThreads;
    private byte[] peerId = "-AO0001-000000000000".getBytes();      //TODO need to give right peerid
    private final Torrent torrent;

    public TorrentEngine(Torrent torrent) throws UnsupportedEncodingException {
        this.torrent = torrent;

        this.pieces = createPieces();

    }

    private void initTrackers(InetAddress ip, int port) {

        Set<URL> trackers = torrent.getTrackers();

        trackerConnectionThreads = Executors.newFixedThreadPool(trackers.size());

        for (URL trackerUrl : trackers) {
            TrackerConnection trackerConnection = new TrackerConnection(this, trackerUrl, torrent.getInfoHash(), peerId, ip, port);
            trackerConnectionThreads.submit(trackerConnection);
            trackerConnections.add(trackerConnection);
        }
    }

    public void mergePeers(Set<InetSocketAddress> peers) {
        synchronized (this) {
            peers.removeAll(peerConnections.keySet());

            for (InetSocketAddress peer : peers) {
                PeerConnection peerConnection = null;
                try {
                    peerConnection = new PeerConnection(peer, this);
                    peerConnections.put(peer, peerConnection);
                    peersThreads.submit(peerConnection);
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

            }
        }
    }

    public List<Piece> createPieces() throws UnsupportedEncodingException {
        List<Piece> pieceList = Lists.newArrayList();

        int pieceCount = (int) Math.ceil((double) torrent.getSize() / torrent.DEFAULT_PIECE_LENGTH);
        for (int i = 0; i < pieceCount; i++) {
            byte[] hash = Arrays.copyOfRange(torrent.getPieces().getBytes(torrent.DEFAULT_TORRENT_ENCODING), i * torrent.DEFAULT_PIECE_LENGTH, (i + 1) * torrent.DEFAULT_PIECE_LENGTH - 1);
            Piece piece = new Piece(i, torrent.DEFAULT_PIECE_LENGTH, hash, torrent.getFileStorage());
            pieceList.add(piece);
        }
        return pieceList;
    }

    @Override
    public void run() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
