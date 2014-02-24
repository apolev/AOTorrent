package org.aotorrent.common.connection;

import org.aotorrent.client.TorrentEngine;
import org.aotorrent.common.Piece;
import org.aotorrent.common.protocol.PeerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
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

    @NotNull
    private final InetSocketAddress socketAddress;
    @NotNull
    private final TorrentEngine torrentEngine;
    @Nullable
    private Socket socket;
    @NotNull
    private final BitSet bitField;

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

            //TODO bitfield

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
        final PeerHandshake peerHandshake = new PeerHandshake(torrentEngine.getTorrent().getInfoHash(), torrentEngine.getPeerId());

        outputStream.write(peerHandshake.toTransmit());

        final int protocolStringLength = inputStream.read();

        byte[] handshakeReply = new byte[1 + protocolStringLength + 8 + 20 + 20];

        handshakeReply[0] = (byte) protocolStringLength;

        inputStream.read(handshakeReply, 1, handshakeReply.length - 1);

        final PeerHandshake peerHandshakeReply = new PeerHandshake(handshakeReply);

        if (!peerHandshake.isOk(torrentEngine.getTorrent())) {
            throw new PeerProtocolException("Handshake failed");
        }
    }
}
