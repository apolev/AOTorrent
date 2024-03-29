package org.aotorrent.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.aotorrent.common.Piece;
import org.aotorrent.common.Torrent;
import org.aotorrent.common.connection.AbstractTrackerConnection;
import org.aotorrent.common.connection.PeerConnection;
import org.aotorrent.common.connection.events.SendHaveMessage;
import org.aotorrent.common.connection.events.StopMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class TorrentEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(TorrentEngine.class);

    private final InetSocketAddress address;
    private Set<AbstractTrackerConnection> trackerConnections = Sets.newLinkedHashSet();
    private Map<SocketAddress, PeerConnection> peerConnections = Maps.newHashMap();
    private List<Piece> pieces;
    private final Set<Piece> inProgress = Sets.newHashSet();
    private ExecutorService trackerConnectionThreads;
    private ExecutorService peersThreads;
    private byte[] peerId = "-AO0001-000000000000".getBytes();      //TODO need to give right peerID
    private final Torrent torrent;

    TorrentEngine(Torrent torrent, InetSocketAddress address) {
        this.torrent = torrent;
        this.address = address;
    }

    private void initTrackers() {

        Collection<String> trackers = torrent.getTrackers();

        trackerConnectionThreads = Executors.newFixedThreadPool(trackers.size());

        peersThreads = Executors.newFixedThreadPool(5);

        for (String trackerUrl : trackers) {
            AbstractTrackerConnection trackerConnection = AbstractTrackerConnection.createConnection(this, trackerUrl, torrent.getInfoHash(), peerId, address.getAddress(), address.getPort());
            if (trackerConnection != null) {
                trackerConnectionThreads.submit(trackerConnection);
                trackerConnections.add(trackerConnection);
            }
        }
    }

    public void mergePeers(Collection<InetSocketAddress> peers) {
        synchronized (this) {
            peers.removeAll(peerConnections.keySet());

            for (InetSocketAddress peer : peers) {
                if (peer.equals(address)) {
                    continue;
                }

                PeerConnection peerConnection = new PeerConnection(peer, this);
                peerConnections.put(peer, peerConnection);
                peersThreads.submit(peerConnection);
            }
        }
    }

    private List<Piece> createPieces() throws UnsupportedEncodingException {
        List<Piece> pieceList = Lists.newArrayList();

        int pieceCount = (int) Math.ceil((double) torrent.getSize() / torrent.getPieceLength());
        for (int i = 0; i < pieceCount - 1; i++) {
            byte[] hash = Arrays.copyOfRange(torrent.getPieces().getBytes(Torrent.DEFAULT_TORRENT_ENCODING), i * Torrent.INFO_HASH_LENGTH, (i + 1) * Torrent.INFO_HASH_LENGTH);
            Piece piece = new Piece(i, torrent.getPieceLength(), hash, torrent.getFileStorage());
            pieceList.add(piece);
        }

        byte[] hash = Arrays.copyOfRange(torrent.getPieces().getBytes(Torrent.DEFAULT_TORRENT_ENCODING), (pieceCount - 1) * Torrent.INFO_HASH_LENGTH, (pieceCount) * Torrent.INFO_HASH_LENGTH);
        int lastPieceLength = (int) (torrent.getSize() % torrent.getPieceLength());
        if (lastPieceLength == 0) {
            lastPieceLength = torrent.getPieceLength();
        }
        Piece piece = new Piece((pieceCount - 1), lastPieceLength, hash, torrent.getFileStorage());

        pieceList.add(piece);

        return Collections.unmodifiableList(pieceList);
    }

    void init() throws UnsupportedEncodingException {
        this.pieces = createPieces();
        initTrackers();
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
        List<Piece> sorted = Lists.newArrayList(pieces);
        Collections.sort(sorted);

        for (Piece piece : sorted) {
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

    public void shutdown() {
        for (PeerConnection peerConnection : peerConnections.values()) {
            peerConnection.addIncomingMessage(new StopMessage());
        }

        for (AbstractTrackerConnection trackerConnection : trackerConnections) {
            trackerConnection.setShutdown(true);
        }

        peersThreads.shutdown();
        peersThreads.shutdownNow();
        trackerConnectionThreads.shutdown();
        trackerConnectionThreads.shutdownNow();
    }

    public void addIncomingConnection(Socket incomingSocket) {
        PeerConnection peerConnection = new PeerConnection(incomingSocket, this);
        peerConnections.put(incomingSocket.getRemoteSocketAddress(), peerConnection);
        peersThreads.submit(peerConnection);

    }

    public void removePeerConnection(SocketAddress socketAddress) {
        if (peerConnections.containsKey(socketAddress)) {
            peerConnections.remove(socketAddress);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        //sb.append("Torrent: ").append(torrent).append(" ");
        sb.append("Known peers: ").append(peerConnections.size()).append(" ");
        sb.append("Connected peers: ").append(((ThreadPoolExecutor) peersThreads).getActiveCount()).append(" ");
        sb.append("Pieces(Downloaded/Total(PieceSize)): ").append(getBitField().cardinality()).append('/').append(pieces.size()).append("(").append(torrent.getPieceLength()).append(")").append(" ");
        sb.append("Size(Downloaded/Total): ").append(getBitField().cardinality() * torrent.getPieceLength() / 1024).append('/').append(torrent.getSize() / 1024).append(" ");
        sb.append("(").append(getBitField().cardinality() * 100 / pieces.size()).append("%)");
        return String.valueOf(sb);
    }
}
