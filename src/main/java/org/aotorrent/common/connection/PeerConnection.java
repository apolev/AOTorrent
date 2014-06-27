package org.aotorrent.common.connection;

import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import org.aotorrent.client.TorrentEngine;
import org.aotorrent.common.Piece;
import org.aotorrent.common.Torrent;
import org.aotorrent.common.connection.events.*;
import org.aotorrent.common.protocol.peer.*;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class PeerConnection implements Runnable {

    public static final int CONNECTION_TIMEOUT = 20000;
    private static final Logger LOGGER = LoggerFactory.getLogger(PeerConnection.class);
    private final boolean incomingConnection;
    @Nullable
    private final Socket incomingSocket;

    private final AtomicBoolean choking = new AtomicBoolean(true);
    private final AtomicBoolean interested = new AtomicBoolean(false);
    private final AtomicBoolean peerChoking = new AtomicBoolean(true);
    private final AtomicBoolean peerInterested = new AtomicBoolean(false);

    private boolean isRunning = true;

    private boolean isDownloading = false;

    private boolean handshakeDone = false;

    @NotNull
    private final SocketAddress socketAddress;
    @NotNull
    private final TorrentEngine torrentEngine;
    @NotNull
    private final BitSet bitField;
    @Nullable
    private Thread messagesHandlerThread = null;
    @Nullable
    private BufferedOutputStream outputStream = null;
    @NotNull
    private final Set<Piece> piecesInProgress = Sets.newHashSet();
    private final int piecesMax = 1; //TODO We are loosing data blocks if it is greater than 1 on torrents with big piece size(2097152, Need investigation)
    @NotNull
    private final LinkedBlockingQueue<ConnectionMessage> incomingMessages = new LinkedBlockingQueue<>();

    public PeerConnection(@NotNull InetSocketAddress socketAddress, @NotNull TorrentEngine torrentEngine) {
        this.socketAddress = socketAddress;
        this.torrentEngine = torrentEngine;
        this.bitField = new BitSet();
        incomingConnection = false;
        incomingSocket = null;
    }

    public PeerConnection(@NotNull Socket incomingSocket, @NotNull TorrentEngine torrentEngine) {
        this.incomingSocket = incomingSocket;
        this.incomingConnection = true;
        this.socketAddress = incomingSocket.getLocalSocketAddress();
        this.torrentEngine = torrentEngine;
        this.bitField = new BitSet();
    }

    @Override
    public void run() {

        try {
            Socket socket;

            if (!incomingConnection) {
                LOGGER.debug("Connecting to : " + ((InetSocketAddress) socketAddress).getAddress().getHostAddress() + ":" + ((InetSocketAddress) socketAddress).getPort());
                socket = new Socket();
                socket.connect(socketAddress, CONNECTION_TIMEOUT);
                socket.setSoTimeout(CONNECTION_TIMEOUT);
                LOGGER.debug("Connected to : " + ((InetSocketAddress) socketAddress).getAddress().getHostAddress() + ":" + ((InetSocketAddress) socketAddress).getPort() + " ReceiveBufferSize: " + socket.getReceiveBufferSize());
            } else {
                socket = incomingSocket;
            }

            assert socket != null;


            try (BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream())) {

                outputStream = new BufferedOutputStream(socket.getOutputStream());

                try {

                    if (handshake(inputStream)) {
                        setHandshakeDone();
                    } else {
                        LOGGER.error("Handshake failed");
                        return;
                    }

                    final BitSet torrentEngineBitField = torrentEngine.getBitField();

                    final PeerRequest bitFieldRequest = new BitFieldRequest(torrentEngineBitField, torrentEngine.getPieceCount());
                    sendToPeer(bitFieldRequest);

                    IncomingMessagesHandler messagesHandler = new IncomingMessagesHandler(inputStream);

                    messagesHandlerThread = new Thread(messagesHandler);
                    messagesHandlerThread.start();

                    while (isRunning) {
                        ConnectionMessage event = null;
                        try {
                            event = incomingMessages.take();
                            event.processMessage(this);
                        } catch (InterruptedException e) {
                            LOGGER.debug("Interrupted!");
                        } catch (IOException e) {
                            LOGGER.error("Processing event " + event + " error");
                        }
                    }
                } catch (PeerProtocolException e) {
                    LOGGER.error("Peer acting not good: ", e);
                } finally {
                    LOGGER.debug("Closing connection to peer " + socketAddress);
                    if (messagesHandlerThread != null) {
                        try {
                            messagesHandlerThread.interrupt();
                            messagesHandlerThread.join();
                        } catch (InterruptedException e) {
                            LOGGER.error("Error interrupting incoming messages thread", e);
                        }

                    } 
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            LOGGER.error("Closing output stream error", e);
                        }
                    }
                }
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    LOGGER.error("Closing peer connection error: ", e);
                }
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        } catch (IOException e) {
            LOGGER.error("Creating peer connection error to host: " + this.socketAddress.toString(), e);
        }

        torrentEngine.removePeerConnection(socketAddress);
    }

    private void sendToPeer(PeerRequest request) throws IOException {
        if (outputStream != null) {
            outputStream.write(request.toTransmit());
            outputStream.flush();
        } else {
            LOGGER.error("Cant send " + request + " request. There is no connection.");
        }
    }

    private int readFromIS(byte[] buffer, InputStream inputStream) throws IOException {
        if (inputStream != null) {
            int index = 0;

            while (index < buffer.length) {

                int amount = inputStream.read(buffer, index, buffer.length - index);

                index += amount;
            }
            return index;
        }

        return -1;
    }

    private void sendBlock(int index, int begin, int length) {
        final Piece piece = torrentEngine.getPiece(index);
        if (piece != null) {
            try {
                final byte[] bytes = piece.read(begin, length);
                PeerRequest pieceRequest = new PieceRequest(index, begin, bytes);
                sendToPeer(pieceRequest);
            } catch (FileNotFoundException e) {
                LOGGER.error("file not found", e);
            } catch (IOException e) {
                LOGGER.error("file read error", e);
            }

        }

    }

    private void requestRequests() throws IOException {
        while (piecesInProgress.size() < piecesMax) {
            final Piece piece = torrentEngine.getNextPiece(bitField);
            if (piece != null) {
                for (int i = 0; i < (piece.getBlockCount() - 1); i++) {
                    PeerRequest request = new RequestRequest(piece.getIndex(), i * Piece.DEFAULT_BLOCK_LENGTH, Piece.DEFAULT_BLOCK_LENGTH);
                    sendToPeer(request);
                }

                int lastBlockOffset = (piece.getBlockCount() - 1) * Piece.DEFAULT_BLOCK_LENGTH;
                int lastBlockLength = piece.getPieceLength() - (piece.getBlockCount() - 1) * Piece.DEFAULT_BLOCK_LENGTH;

                PeerRequest request = new RequestRequest(piece.getIndex(), lastBlockOffset, lastBlockLength);
                sendToPeer(request);

                piecesInProgress.add(piece);
            } else {
                LOGGER.info("TorrentEngine don't gives us new pieces to download");
                break;
            }
        }
    }

    private boolean handshake(@NotNull InputStream inputStream) throws IOException, PeerProtocolException {
        final PeerRequest peerHandshake = new HandshakeRequest(torrentEngine.getInfoHash(), torrentEngine.getPeerId());

        sendToPeer(peerHandshake);

        if (incomingConnection) {
            return true;
        }

        final int protocolStringLength = inputStream.read();

        final byte[] bytes = new byte[protocolStringLength + 8 + Torrent.INFO_HASH_LENGTH + Torrent.PEER_ID_LENGTH];
        final int read = readFromIS(bytes, inputStream);

        if (read < bytes.length) {
            return false;
        }

        final byte[] handshakeReply = ArrayUtils.addAll(new byte[]{(byte) protocolStringLength}, bytes);

        final HandshakeRequest peerHandshakeReply = new HandshakeRequest(handshakeReply);

        return peerHandshakeReply.isOk(torrentEngine.getInfoHash());

    }

    private void setHandshakeDone() {
        this.handshakeDone = true;
    }

    private void choke() throws IOException {
        PeerRequest chokeRequest = new ChokeRequest();
            sendToPeer(chokeRequest);

            choking.set(true);
    }

    private void unChoke() throws IOException {
        PeerRequest unChokeRequest = new UnChokeRequest();
            sendToPeer(unChokeRequest);

            choking.set(false);
    }

    private void interested() throws IOException {
        PeerRequest interestedRequest = new InterestedRequest();
            sendToPeer(interestedRequest);

            interested.set(true);
    }

    private void notInterested() throws IOException {
        PeerRequest notInterestedRequest = new NotInterestedRequest();
            sendToPeer(notInterestedRequest);

            interested.set(false);
    }

    private void haveMessage(int pieceIndex) throws IOException {
        PeerRequest request = new HaveRequest(pieceIndex);
            sendToPeer(request);
    }

    private void startDownload() {
        if (torrentEngine.isUsefulPeer(bitField)) {
            proceedDownload(null);
        }
    }

    private void proceedDownload(@Nullable final Piece piece) {

        try {

            if (!interested.getAndSet(true)) {
                interested();
            }

            if (!peerChoking.get()) {

                if (piece != null && piece.isComplete()) {
                    torrentEngine.setPieceDone(piece);
                    piecesInProgress.remove(piece);
                }

                requestRequests();
                isDownloading = true;
            } else {
                torrentEngine.setPieceDone(piecesInProgress);
                piecesInProgress.clear();
            }
        } catch (IOException e) {
            LOGGER.error("Request of new blocks failed", e);
        }

        if (piecesInProgress.isEmpty()) {
            LOGGER.info("Nothing else to download.");
            isDownloading = false;
        } else {
            isDownloading = true;
        }
    }


    public void processMessage(ReceivedRequestMessage message) {
        sendBlock(message.getIndex(), message.getBegin(), message.getLength());
    }

    public void processMessage(ReceivedBlockMessage message) {
        proceedDownload(message.getPiece());
    }

    public void processMessage(@SuppressWarnings("unused") UnChokeMessage message) {
        peerChoking.set(false);
        startDownload();
    }

    public void processMessage(@SuppressWarnings("unused") ChokeMessage message) {
        //TODO
    }

    public void processMessage(@SuppressWarnings("unused") ReceivedBitFieldMessage receivedBitFieldMessage) {
        startDownload();
    }

    public void processMessage(@SuppressWarnings("unused") StopMessage stopMessage) {
        LOGGER.info("Stop message processed");
        isRunning = false;
        if (messagesHandlerThread != null) {
            try {
                messagesHandlerThread.interrupt();
                messagesHandlerThread.join();
            } catch (InterruptedException e) {
                LOGGER.error("Error interrupting incoming messages thread", e);
            }
        }
        Thread.currentThread().interrupt();
    }

    public void processMessage(@SuppressWarnings("unused") ReceivedHaveMessage haveMessage) {
        if (piecesInProgress.size() < piecesMax) {
            proceedDownload(null);
        }
    }

    public void processMessage(SendHaveMessage sendHaveMessage) throws IOException {
        haveMessage(sendHaveMessage.getPieceIndex());
    }

    public void addIncomingMessage(ConnectionMessage message) {
        incomingMessages.add(message);
    }

    public void processMessage(@SuppressWarnings("UnusedParameters") PeerInterestedMessage peerInterestedMessage) throws IOException {
        unChoke();
    }

    private class IncomingMessagesHandler implements Runnable {
        @NotNull
        private final InputStream inputStream;

        private IncomingMessagesHandler(@NotNull InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try {
                while (isRunning) {

                    byte[] messageLengthBytes = new byte[Ints.BYTES];

                    final int read = readFromIS(messageLengthBytes, inputStream);

                    if (read != messageLengthBytes.length) {
                        return;//TODO say something
                    }

                    int messageLength = ByteBuffer.allocate(4).put(messageLengthBytes).getInt(0) - 1; //Length without 1-byte messageType

                    if (messageLength >= 0) {

                        RequestType requestType = RequestType.from(inputStream.read());

                        if (requestType != RequestType.PIECE) {
                            LOGGER.debug("Request " + requestType + " received from " + socketAddress);
                        }

                        byte[] message = new byte[messageLength];

                        final int readMessage = readFromIS(message, inputStream);
                        if (readMessage != message.length) {
                            return;//TODO say something
                        }

                        switch (requestType) {
                            case CHOKE:
                                peerChoking.set(true);
                                break;
                            case UNCHOKE:
                                receivedUnChoke();
                                break;
                            case INTERESTED:
                                receivedInterested();
                                break;
                            case NOT_INTERESTED:
                                peerInterested.set(false);
                                break;
                            case HAVE:
                                receivedHave(message);
                                break;
                            case BIT_FIELD:
                                receivedBitField(message);
                                break;
                            case REQUEST:
                                receivedRequest(message);
                                break;
                            case PIECE:
                                receivedPiece(message);
                                break;
                            case CANCEL:
                                receivedCancel(message);
                                break;
                            case PORT:
                                receivedPort(message);
                                break;
                            case KEEP_ALIVE:
                                break;
                        }
                    }

                    setHandshakeDone();
                }

            } catch (SocketException e) {
                LOGGER.debug("Connection reset", e);
            } catch (IOException e) {
                LOGGER.error("IOException on " + socketAddress + "(incoming thread)", e);
            } finally {
                LOGGER.debug("Closing connection to " + socketAddress + "(incoming thread)");
                incomingMessages.add(new StopMessage());
            }

        }

        private void receivedInterested() {
            peerInterested.set(true);
            incomingMessages.add(new PeerInterestedMessage());
        }

        private void receivedUnChoke() {
            peerChoking.set(false);
            incomingMessages.add(new UnChokeMessage());
        }

        private void receivedHave(byte[] message) {
            final HaveRequest haveRequest = new HaveRequest(message);
            bitField.set(haveRequest.getIndex());
            incomingMessages.add(new ReceivedHaveMessage());
        }

        private void receivedBitField(byte[] message) {
            BitFieldRequest bitFieldRequest = new BitFieldRequest(message);
            bitField.or(bitFieldRequest.getBitField());
            incomingMessages.add(new ReceivedBitFieldMessage());
        }

        private void receivedRequest(byte[] message) {
            if (!choking.get()) {
                final RequestRequest request = new RequestRequest(message);
                final ConnectionMessage receivedRequestMessage = new ReceivedRequestMessage(request.getIndex(), request.getBegin(), request.getLength());
                incomingMessages.add(receivedRequestMessage);
            }
        }

        private void receivedPiece(byte[] message) {
            final PieceRequest pieceRequest = new PieceRequest(message);
            final Piece piece = torrentEngine.getPiece(pieceRequest.getIndex());
            if (piece != null) {
                piece.write(pieceRequest.getBlock(), pieceRequest.getBegin());
                incomingMessages.add(new ReceivedBlockMessage(piece));
            } else {
                LOGGER.error("Received piece not exist");
            }
        }

        private void receivedCancel(@SuppressWarnings("unused") byte[] message) {
        }

        private void receivedPort(@SuppressWarnings("unused") byte[] message) {
        }

    }
}
