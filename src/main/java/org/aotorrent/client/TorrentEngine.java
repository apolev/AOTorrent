package org.aotorrent.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.aotorrent.common.Piece;
import org.aotorrent.common.Torrent;
import org.aotorrent.common.connection.PeerConnection;
import org.aotorrent.common.connection.TrackerConnection;
import org.aotorrent.common.connection.events.SendHaveMessage;
import org.aotorrent.common.connection.events.StopMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class TorrentEngine implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TorrentEngine.class);

    private static final int DEFAULT_PORT = 6968;

    private Set<TrackerConnection> trackerConnections = Sets.newLinkedHashSet();
    private Map<InetSocketAddress, PeerConnection> peerConnections = Maps.newHashMap();
    private List<Piece> pieces;
    private final Set<Piece> inProgress = Sets.newHashSet();
    private ExecutorService trackerConnectionThreads;
    private ExecutorService peersThreads;
    private byte[] peerId = "-AO0001-000000000000".getBytes();      //TODO need to give right peerid
    private final Torrent torrent;

    public TorrentEngine(Torrent torrent) throws UnsupportedEncodingException {
        this.torrent = torrent;
    }

    private void initTrackers(InetAddress ip, int port) {

        List<URL> trackers = torrent.getTrackers();

        trackerConnectionThreads = Executors.newFixedThreadPool(trackers.size());

        peersThreads = Executors.newFixedThreadPool(5);

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
                PeerConnection peerConnection = new PeerConnection(peer, this);
                peerConnections.put(peer, peerConnection);
                peersThreads.submit(peerConnection);
            }
        }
    }

    private List<Piece> createPieces() throws UnsupportedEncodingException {
        List<Piece> pieceList = Lists.newArrayList();

        int pieceCount = (int) Math.ceil((double) torrent.getSize() / torrent.getPieceLength());
        for (int i = 0; i < pieceCount; i++) {
            byte[] hash = Arrays.copyOfRange(torrent.getPieces().getBytes(Torrent.DEFAULT_TORRENT_ENCODING), i * Torrent.INFO_HASH_LENGTH, (i + 1) * Torrent.INFO_HASH_LENGTH);
            Piece piece = new Piece(i, torrent.getPieceLength(), hash, torrent.getFileStorage());
            pieceList.add(piece);
        }
        return pieceList;
    }

    @Override
    public void run() {
        try {
            this.pieces = createPieces();
            initTrackers(Inet4Address.getLocalHost(), DEFAULT_PORT);

            while (!isTorrentDone()) {
                Thread.sleep(1000);
            }

            for (PeerConnection peerConnection : peerConnections.values()) {
                peerConnection.processMessage(new StopMessage());
            }

            peersThreads.shutdown();
            trackerConnectionThreads.shutdown();

        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Unsupported Encoding", e);
        } catch (UnknownHostException e) {
            LOGGER.error("Can't determine own ip address", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

    @Nullable
    public Piece getNextPiece(@NotNull BitSet bitField) { //TODO increasePeerCount()
        Set<Piece> sorted = Sets.newTreeSet(pieces);
        Iterator<Piece> iterator = sorted.iterator();

        for (int i = 0; i < sorted.size(); i++) {
            Piece piece = iterator.next();

            synchronized (inProgress) {
                if (bitField.get(piece.getIndex()) && !piece.isComplete() && !inProgress.contains(piece)) {
                    inProgress.add(piece);
                    return piece;
                }
            }
        }
        return null;
    }

    public boolean isUsefulPeer(@NotNull BitSet bitField) {
        BitSet currentBitField = this.getBitField();

        for (int i = 0; i < pieces.size(); i++) {
            if (!currentBitField.get(i) && bitField.get(i)) {
                return true;
            }
        }

        return false;
    }

    private boolean isTorrentDone() {
        final BitSet bitField = getBitField();

        return bitField.cardinality() == pieces.size();
    }

    @Nullable
    public Piece getPiece(int index) {
        return pieces.get(index);

    }

    public void setPieceDone(@NotNull Piece piece) {
        synchronized (inProgress) {
            inProgress.remove(piece);
        }
        for (PeerConnection peerConnection : peerConnections.values()) {
            peerConnection.addIncomingMessage(new SendHaveMessage(piece.getIndex()));
        }
    }

    public void setPieceDone(@NotNull Collection<Piece> pieces) {
        if (!pieces.isEmpty()) {
            synchronized (inProgress) {
                inProgress.removeAll(pieces);
            }
        }
    }

    public byte[] getInfoHash() {
        return torrent.getInfoHash();
    }
}
