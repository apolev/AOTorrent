package org.aotorrent.common.connection;

import org.aotorrent.client.TorrentEngine;
import org.aotorrent.common.Piece;
import org.aotorrent.common.protocol.peer.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class PeerConnection implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeerConnection.class);

    private boolean choking = true;
    private boolean interested = false;
    private boolean peerChoking = true;
    private boolean peerInterested = false;

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

    public PeerConnection(@NotNull InetSocketAddress socketAddress, @NotNull TorrentEngine torrentEngine) {
        this.socketAddress = socketAddress;
        this.torrentEngine = torrentEngine;
        this.bitField = new BitSet(torrentEngine.getPieceCount());
    }

    @Override
    public void run() {

        try {
            socket = new Socket(socketAddress.getAddress(), socketAddress.getPort());

            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            handshake(inputStream, outputStream);

            final BitSet torrentEngineBitField = torrentEngine.getBitField();

            if (torrentEngineBitField.cardinality() > 0) {
                BitFieldRequest bitFieldRequest = new BitFieldRequest(torrentEngineBitField);
                outputStream.write(bitFieldRequest.toTransmit());
            }

            //TODO interest

            Piece piece = null;

            while ((piece = torrentEngine.getNextPiece(bitField)) != null) {
                downloadPiece(piece);
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

    private void downloadPiece(Piece piece) {
        while (!piece.isComplete()) {
            int blockIndex = piece.getNextEmptyBlockIndex();


        }
    }

    private void handshake(InputStream inputStream, OutputStream outputStream) throws IOException, PeerProtocolException {
        final HandshakeRequest peerHandshake = new HandshakeRequest(torrentEngine.getTorrent().getInfoHash(), torrentEngine.getPeerId());

        outputStream.write(peerHandshake.toTransmit());

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

    private class incomingMessagesHandler implements Runnable {

        private final BufferedInputStream bIS;

        private incomingMessagesHandler(BufferedInputStream bIS) {
            this.bIS = bIS;
        }

        @Override
        public void run() {
            byte[] messageLengthBytes = new byte[4];
            try {
                while (isRunning && 4 == bIS.read(messageLengthBytes)) {

                    int messageLength = ByteBuffer.allocate(4).put(messageLengthBytes).getInt(0) - 1; //Length without 1-byte messageType

                    if (messageLength != 0) {

                        byte[] message = new byte[messageLength];

                        RequestType requestType = RequestType.from(bIS.read());

                        bIS.read(message);

                        switch (requestType) {
                            case CHOKE:
                                peerChoking = true;
                                break;
                            case UNCHOKE:
                                peerChoking = false;
                                break;
                            case INTERESTED:
                                peerInterested = true;
                                break;
                            case NOT_INTERESTED:
                                peerInterested = false;
                                break;
                            case HAVE:
                                receivedHave(message);
                                break;
                            case BIT_FIELD:
                                if (!isHandshakeIsOver()) {
                                    receivedBitField(message);
                                    setHandshakeIsOver();
                                }
                                break;
                            case REQUEST:
                                if (!choking) {
                                    receivedRequest(message);
                                }
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
                        }
                    }

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
            final RequestRequest requestRequest = new RequestRequest(message);
            requestPending = requestRequest;
        }

        private void receivedPiece(byte[] message) {
            final PieceRequest pieceRequest = new PieceRequest(message);
            final Piece piece = torrentEngine.getPiece(pieceRequest.getIndex());
            if (piece != null) {
                piece.write(pieceRequest.getBlock(), pieceRequest.getBegin());
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
