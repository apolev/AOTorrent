package org.aotorrent.common.connection;

import org.aotorrent.client.TorrentEngine;
import org.aotorrent.common.Piece;
import org.aotorrent.common.connection.events.*;
import org.aotorrent.common.protocol.peer.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class PeerConnection implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeerConnection.class);

    private AtomicBoolean choking = new AtomicBoolean(true);
    private AtomicBoolean interested = new AtomicBoolean(false);
    private AtomicBoolean peerChoking = new AtomicBoolean(true);
    private AtomicBoolean peerInterested = new AtomicBoolean(false);

    private boolean isRunning = true;

    private boolean isDownloading = false;

    private boolean handshakeDone = false;

    @NotNull
    private final InetSocketAddress socketAddress;
    @NotNull
    private final TorrentEngine torrentEngine;
    @Nullable
    private Socket socket;
    @NotNull
    private final BitSet bitField;
    @Nullable
    private IncomingMessagesHandler messagesHandler = null;
    @Nullable
    private Thread messagesHandlerThread = null;
    @Nullable
    private BufferedOutputStream outputStream = null;
    @NotNull
    private LinkedBlockingQueue<ConnectionMessage> incomingMessages = new LinkedBlockingQueue<ConnectionMessage>();

    public PeerConnection(@NotNull InetSocketAddress socketAddress, @NotNull TorrentEngine torrentEngine) {
        this.socketAddress = socketAddress;
        this.torrentEngine = torrentEngine;
        this.bitField = new BitSet(torrentEngine.getPieceCount());
    }

    @Override
    public void run() {

        try {
            socket = new Socket(socketAddress.getAddress(), socketAddress.getPort());

            BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());
            outputStream = new BufferedOutputStream(socket.getOutputStream());

            handshake(inputStream, outputStream);

            setHandshakeDone();

            final BitSet torrentEngineBitField = torrentEngine.getBitField();

            final PeerRequest bitFieldRequest = new BitFieldRequest(torrentEngineBitField, torrentEngine.getPieceCount());
            sendToPeer(bitFieldRequest);

            messagesHandler = new IncomingMessagesHandler(inputStream);

            messagesHandlerThread = new Thread(messagesHandler);
            messagesHandlerThread.start();

            while (isRunning) {
                try {
                    final ConnectionMessage event = incomingMessages.take();
                    event.processMessage(this);
                } catch (InterruptedException e) {
                    LOGGER.debug("Interrupted!");
                }
            }

        } catch (IOException e) {
            LOGGER.error("Creating peer connection error to host: " + this.socketAddress.toString(), e);
        } catch (PeerProtocolException e) {
            LOGGER.error("Peer acting not good: ", e);
        } finally {
            LOGGER.debug("Closing connection to peer " + socketAddress);
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    LOGGER.error("Closing peer connection error: ", e);
                }
            }
        }

    }

    private void sendToPeer(PeerRequest request) throws IOException {
        if (outputStream != null) {
            LOGGER.debug("Sending " + request + " request.");
            outputStream.write(request.toTransmit());
            outputStream.flush();
        } else {
            LOGGER.error("Cant send " + request + " request. There is no connection.");
        }
    }


    private void sendBlock(int index, int begin, int length) {
        final Piece piece = torrentEngine.getPiece(index);
        if (piece != null) {
            try {
                final byte[] bytes = piece.read(begin, length);
                PeerRequest pieceRequest = new PieceRequest(index, begin, bytes);
                sendToPeer(pieceRequest);
            } catch (IOException e) {
                LOGGER.error("file read error", e);
            }

        }

    }

    private void requestRequests(Piece piece) throws IOException { // requesting all blocks for this piece
        //int blockIndex = piece.getNextEmptyBlockIndex();
        for (int i = 0; i < (piece.getBlockCount()); i++) {
            PeerRequest request = new RequestRequest(piece.getIndex(), i * Piece.DEFAULT_BLOCK_LENGTH, Piece.DEFAULT_BLOCK_LENGTH);
            sendToPeer(request);
        }

    }

    private void handshake(InputStream inputStream, OutputStream outputStream) throws IOException, PeerProtocolException {
        final PeerRequest peerHandshake = new HandshakeRequest(torrentEngine.getTorrent().getInfoHash(), torrentEngine.getPeerId());

        sendToPeer(peerHandshake);

        final int protocolStringLength = inputStream.read();

        byte[] handshakeReply = new byte[1 + protocolStringLength + 8 + 20 + 20];

        handshakeReply[0] = (byte) protocolStringLength;

        inputStream.read(handshakeReply, 1, handshakeReply.length - 1);

        final HandshakeRequest peerHandshakeReply = new HandshakeRequest(handshakeReply);

        if (!peerHandshakeReply.isOk(torrentEngine.getTorrent())) {
            throw new PeerProtocolException("Handshake failed");
        }
    }

    public boolean isHandshakeDone() {
        return handshakeDone;
    }

    private void setHandshakeDone() {
        this.handshakeDone = true;
    }

    private void choke() {
        try {
            PeerRequest chokeRequest = new ChokeRequest();
            sendToPeer(chokeRequest);

            choking.set(true);
        } catch (IOException e) {
            LOGGER.error("can't send choke request", e);
        }
    }

    private void unchoke() {
        try {
            PeerRequest unchokeRequest = new UnchokeRequest();
            sendToPeer(unchokeRequest);

            choking.set(false);
        } catch (IOException e) {
            LOGGER.error("can't send unchoke request", e);
        }
    }

    private void interested() {
        try {
            PeerRequest interestedRequest = new InterestedRequest();
            sendToPeer(interestedRequest);

            interested.set(true);
        } catch (IOException e) {
            LOGGER.error("can't send interested request", e);
        }
    }

    private void notInterested() {
        try {
            PeerRequest notInterestedRequest = new NotInterestedRequest();
            sendToPeer(notInterestedRequest);

            interested.set(false);
        } catch (IOException e) {
            LOGGER.error("can't send not interested request", e);
        }
    }

    private void startDownload() {
        final Piece piece = torrentEngine.getNextPiece(bitField);
        if (piece != null) {
            proceedDownload(piece);
        }
    }

    @Nullable
    private Piece proceedDownload(final Piece piece) {
        try {
            if (!peerChoking.get()) {

                if (!interested.getAndSet(true)) {
                    interested();
                }

                if (piece.isComplete()) {
                    final Piece nextPiece = torrentEngine.getNextPiece(bitField);
                    if (nextPiece != null) {
                        requestRequests(nextPiece);
                        isDownloading = true;
                        return nextPiece;
                    }
                } else if (piece.isClear()) {
                    requestRequests(piece);
                    isDownloading = true;
                    return piece;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Request Sending Failed", e);
        }
        isDownloading = false;
        return null;
    }

    public void processMessage(ReceivedRequestMessage message) {
        sendBlock(message.getIndex(), message.getBegin(), message.getLength());
    }

    public void processMessage(ReceivedBlockMessage message) {
        proceedDownload(message.getPiece());
    }

    public void processMessage(UnchokeMessage message) {
        peerChoking.set(false);
        startDownload();
    }

    public void processMessage(ChokeMessage message) {
        //choke();
    }

    public void processMessage(ReceivedBitFieldMessage receivedBitFieldMessage) {
//        startDownload();
    }

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public void processMessage(StopMessage stopMessage) {
        isRunning = false;
        messagesHandlerThread.interrupt();
    }

    private class IncomingMessagesHandler implements Runnable {

        private final BufferedInputStream bIS;

        private IncomingMessagesHandler(BufferedInputStream bIS) {
            this.bIS = bIS;
        }

        @Override
        public void run() {
            byte[] messageLengthBytes = new byte[4];
            try {
                while (isRunning) {

                    messageLengthBytes = readFromIS(4);

                    int messageLength = ByteBuffer.allocate(4).put(messageLengthBytes).getInt(0) - 1; //Length without 1-byte messageType

                    if (messageLength >= 0) {

                        RequestType requestType = RequestType.from(bIS.read());

                        LOGGER.debug("Request " + requestType + " received from " + socketAddress);

                        byte[] message = readFromIS(messageLength);

                        switch (requestType) {
                            case CHOKE:
                                peerChoking.set(true);
                                break;
                            case UNCHOKE:
                                receivedUnchoke();
                                break;
                            case INTERESTED:
                                peerInterested.set(true);
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
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                LOGGER.debug("Closing connection to " + socketAddress + "(incoming thread)");
            }
        }

        private byte[] readFromIS(int length) throws IOException {
            int index = 0;

            byte[] result = new byte[length];

            while (index < length) {

                int amount = bIS.read(result, index, length - index);

                index += amount;
            }
            return result;
        }

        private void receivedUnchoke() {
            peerChoking.set(false);
            PeerConnection.this.incomingMessages.add(new UnchokeMessage());
        }

        private void receivedHave(byte[] message) {
            final HaveRequest haveRequest = new HaveRequest(message);
            bitField.set(haveRequest.getIndex());
        }

        private void receivedBitField(byte[] message) {
            BitFieldRequest bitFieldRequest = new BitFieldRequest(message, torrentEngine.getPieceCount());
            bitField.or(bitFieldRequest.getBitField());
            PeerConnection.this.incomingMessages.add(new ReceivedBitFieldMessage());
        }

        private void receivedRequest(byte[] message) {
            if (!choking.get()) {
                final RequestRequest request = new RequestRequest(message);
                final ReceivedRequestMessage receivedRequestMessage = new ReceivedRequestMessage(request.getIndex(), request.getBegin(), request.getLength());
                PeerConnection.this.incomingMessages.add(receivedRequestMessage);
            }
        }

        private void receivedPiece(byte[] message) {
            final PieceRequest pieceRequest = new PieceRequest(message);
            final Piece piece = torrentEngine.getPiece(pieceRequest.getIndex());
            if (piece != null) {
                piece.write(pieceRequest.getBlock(), pieceRequest.getBegin());
                PeerConnection.this.incomingMessages.add(new ReceivedBlockMessage(piece));
            } else {
                LOGGER.error("Received piece not exist");
            }
        }

        private void receivedCancel(byte[] message) {
        }

        private void receivedPort(byte[] message) {
        }

    }
}
