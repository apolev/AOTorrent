package org.aotorrent.common.connection;

import org.aotorrent.client.TorrentEngine;
import org.aotorrent.common.Piece;
import org.aotorrent.common.connection.events.ConnectionMessage;
import org.aotorrent.common.connection.events.ReceivedBlockMessage;
import org.aotorrent.common.connection.events.ReceivedRequestMessage;
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

    private boolean handshakeIsOver = false;

    @NotNull
    private final InetSocketAddress socketAddress;
    @NotNull
    private final TorrentEngine torrentEngine;
    @Nullable
    private Socket socket;
    @NotNull
    private final BitSet bitField;
    @Nullable
    private RequestRequest requestPending = null;
    @Nullable
    private IncomingMessagesHandler messagesHandler = null;
    @Nullable
    BufferedOutputStream outputStream = null;
    @Nullable
    private Piece downloadingPiece;
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

            setHandshakeIsOver();

            final BitSet torrentEngineBitField = torrentEngine.getBitField();

            if (torrentEngineBitField.cardinality() > 0) {
                BitFieldRequest bitFieldRequest = new BitFieldRequest(torrentEngineBitField);
                outputStream.write(bitFieldRequest.toTransmit());
                outputStream.flush();
            }

            messagesHandler = new IncomingMessagesHandler(inputStream);

            new Thread(messagesHandler).start();

            while (isRunning) {
                try {
                    final ConnectionMessage event = incomingMessages.take();
                    event.processMessage(this);
                } catch (InterruptedException e) {
                    LOGGER.debug("Interrupted!");
                }
            }

        } catch (IOException e) {
            LOGGER.error("Creating peer connection error: ", e);
        } catch (PeerProtocolException e) {
            LOGGER.error("Peer acting not good: ", e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    LOGGER.error("Closing peer connection error: ", e);
                }
            }
        }

    }


    public void sendBlock(int index, int begin, int length) {
        final Piece piece = torrentEngine.getPiece(index);
        if (piece != null) {
            try {
                final byte[] bytes = piece.read(begin, length);
                PieceRequest pieceRequest = new PieceRequest(requestPending.getIndex(), requestPending.getBegin(), bytes);
                outputStream.write(pieceRequest.toTransmit());
                outputStream.flush();
                requestPending = null;
            } catch (IOException e) {
                LOGGER.error("file read error", e);
            }

        }

    }

    private boolean iHaveSomethingToDownload() {
        BitSet copy = (BitSet) bitField.clone();

        copy.and(torrentEngine.getBitField());

        return (copy.cardinality() > 0) && (!peerChoking.get());
    }

    private void requestRequest(Piece piece) throws IOException {

        if (!interested.getAndSet(true)) {
            interested();
        }

        int blockIndex = piece.getNextEmptyBlockIndex();
        RequestRequest request = new RequestRequest(piece.getIndex(), blockIndex * Piece.DEFAULT_BLOCK_LENGTH, Piece.DEFAULT_BLOCK_LENGTH);
        if (outputStream != null) {
            outputStream.write(request.toTransmit());
            outputStream.flush();
        }

    }

    private void handshake(InputStream inputStream, OutputStream outputStream) throws IOException, PeerProtocolException {
        final HandshakeRequest peerHandshake = new HandshakeRequest(torrentEngine.getTorrent().getInfoHash(), torrentEngine.getPeerId());

        outputStream.write(peerHandshake.toTransmit());
        outputStream.flush();

        final int protocolStringLength = inputStream.read();

        byte[] handshakeReply = new byte[1 + protocolStringLength + 8 + 20 + 20];

        handshakeReply[0] = (byte) protocolStringLength;

        inputStream.read(handshakeReply, 1, handshakeReply.length - 1);

        final HandshakeRequest peerHandshakeReply = new HandshakeRequest(handshakeReply);

        if (!peerHandshakeReply.isOk(torrentEngine.getTorrent())) {
            throw new PeerProtocolException("Handshake failed");
        }
    }

    public boolean isHandshakeIsOver() {
        return handshakeIsOver;
    }

    public void setHandshakeIsOver() {
        this.handshakeIsOver = true;
    }

    public void choke() {
        try {
            ChokeRequest chokeRequest = new ChokeRequest();

            assert outputStream != null;
            outputStream.write(chokeRequest.toTransmit());

            choking.set(true);
        } catch (IOException e) {
            LOGGER.error("can't send choke request", e);
        }
    }

    public void unchoke() {
        try {
            UnchokeRequest unchokeRequest = new UnchokeRequest();

            assert outputStream != null;
            outputStream.write(unchokeRequest.toTransmit());

            choking.set(false);
        } catch (IOException e) {
            LOGGER.error("can't send unchoke request", e);
        }
    }

    public void interested() {
        try {
            InterestedRequest interestedRequest = new InterestedRequest();

            assert outputStream != null;
            outputStream.write(interestedRequest.toTransmit());

            interested.set(true);
        } catch (IOException e) {
            LOGGER.error("can't send interested request", e);
        }
    }

    public void notInterested() {
        try {
            NotInterestedRequest notInterestedRequest = new NotInterestedRequest();

            assert outputStream != null;
            outputStream.write(notInterestedRequest.toTransmit());

            interested.set(false);
        } catch (IOException e) {
            LOGGER.error("can't send interested request", e);
        }
    }

    @Nullable
    public Piece proceedDownload(final Piece piece) {
        try {
            if (!peerChoking.get()) {
                if (piece.isComplete()) {
                    final Piece nextPiece = torrentEngine.getNextPiece(bitField);
                    if (nextPiece != null) {
                        requestRequest(nextPiece);
                        return nextPiece;
                    } else {
                        isRunning = false;
                    }
                } else {
                    requestRequest(piece);
                    return piece;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Request Sending Failed", e);
        }
        return null;
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
                while (isRunning && bIS.read(messageLengthBytes) == 4) {

                    int messageLength = ByteBuffer.allocate(4).put(messageLengthBytes).getInt(0) - 1; //Length without 1-byte messageType

                    if (messageLength != 0) {

                        byte[] message = new byte[messageLength];

                        RequestType requestType = RequestType.from(bIS.read());

                        bIS.read(message);

                        switch (requestType) {
                            case CHOKE:
                                peerChoking.set(true);
                                break;
                            case UNCHOKE:
                                peerChoking.set(false);
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

                    setHandshakeIsOver();
                    PeerConnection.this.notify();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void receivedHave(byte[] message) {
            final HaveRequest haveRequest = new HaveRequest(message);
            bitField.set(haveRequest.getIndex());
        }

        private void receivedBitField(byte[] message) {
            BitFieldRequest bitFieldRequest = new BitFieldRequest(message, torrentEngine.getPieceCount());
            bitField.or(bitFieldRequest.getBitField());
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
