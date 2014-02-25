package org.aotorrent.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.aotorrent.common.Piece;
import org.aotorrent.common.Torrent;
import org.aotorrent.common.connection.PeerConnection;
import org.aotorrent.common.connection.TrackerConnection;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;
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

        List<URL> trackers = torrent.getTrackers();

        trackerConnectionThreads = Executors.newFixedThreadPool(trackers.size());

        for (URL trackerUrl : trackers) {
            TrackerConnection trackerConnection = new TrackerConnection(this, trackerUrl, torrent.getInfoHash(), peerId, ip, port);
            trackerConnectionThreads.submit(trackerConnection);
            trackerConnections.add(trackerConnection);
        }
    }

    public void mergePeers(Collection<InetSocketAddress> peers) {
        synchronized (this) {
            peers.removeAll(peerConnections.keySet());

            for (InetSocketAddress peer : peers) {
                PeerConnection peerConnection = null;

                peerConnection = new PeerConnection(peer, this);
                peerConnections.put(peer, peerConnection);
                peersThreads.submit(peerConnection);
            }
        }
    }

    private List<Piece> createPieces() throws UnsupportedEncodingException {
        List<Piece> pieceList = Lists.newArrayList();

        int pieceCount = (int) Math.ceil((double) torrent.getSize() / Torrent.DEFAULT_PIECE_LENGTH);
        for (int i = 0; i < pieceCount; i++) {
            byte[] hash = Arrays.copyOfRange(torrent.getPieces().getBytes(Torrent.DEFAULT_TORRENT_ENCODING), i * Torrent.DEFAULT_PIECE_LENGTH, (i + 1) * Torrent.DEFAULT_PIECE_LENGTH - 1);
            Piece piece = new Piece(i, Torrent.DEFAULT_PIECE_LENGTH, hash, torrent.getFileStorage());
            pieceList.add(piece);
        }
        return pieceList;
    }

    @Override
    public void run() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Torrent getTorrent() {
        return torrent;
    }

    public byte[] getPeerId() {
        return peerId;
    }

    public BitSet getBitField() {
        BitSet bitField = new BitSet(pieces.size());

        int index = 0;
        for (Piece piece : pieces) {
            bitField.set(index, piece.isComplete());
            index++;
        }
        return bitField;
    }

    public int getPieceCount() {
        return pieces.size();
    }

    public Piece getNextPiece(BitSet bitField) { //TODO increasePeerCount()
        Set<Piece> sorted = Sets.newTreeSet(pieces);
        Iterator<Piece> iterator = sorted.iterator();

        for (int i = 0; i < sorted.size(); i++) {
            Piece piece = iterator.next();
            if (bitField.get(i) && !piece.isComplete()) {
                return piece;
            }

        }
        return null;
    }

    @Nullable
    public Piece getPiece(int index) {
        return pieces.get(index);

    }
}
