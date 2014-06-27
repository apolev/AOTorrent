package org.aotorrent.client;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.aotorrent.common.Torrent;
import org.aotorrent.common.bencode.InvalidBEncodingException;
import org.aotorrent.common.protocol.peer.HandshakeRequest;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Set;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    3/13/14.
 */
public class TorrentClient implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TorrentClient.class);

    @NotNull
    private final Set<TorrentEngine> torrentEngines = Sets.newHashSet();
    @NotNull
    private final Map<String, String> torrentFileToDownloadPath;
    @NotNull
    private final InetSocketAddress localSocketAddress;
    private boolean isRunning = true;


    public TorrentClient(@NotNull String torrentFile, @NotNull String downloadPath, @NotNull InetSocketAddress localSocketAddress) {
        this.localSocketAddress = localSocketAddress;
        torrentFileToDownloadPath = Maps.newHashMap();
        torrentFileToDownloadPath.put(torrentFile, downloadPath);
    }

    public TorrentClient(@NotNull Map<String, String> torrentFileToDownloadPath, @NotNull InetSocketAddress localSocketAddress) {
        this.torrentFileToDownloadPath = Maps.newHashMap(torrentFileToDownloadPath);
        this.localSocketAddress = localSocketAddress;
    }

    @Override
    public void run() {

        try (ServerSocket serverSocket = new ServerSocket(localSocketAddress.getPort())) {


            for (Map.Entry<String, String> torrentToPath : torrentFileToDownloadPath.entrySet()) {
                final String torrentPath = torrentToPath.getKey();
                final String downloadPath = torrentToPath.getValue();

                try {
                    Torrent torrent = new Torrent(torrentPath, downloadPath);

                    TorrentEngine torrentEngine = new TorrentEngine(torrent, localSocketAddress);
                    torrentEngine.init();

                    torrentEngines.add(torrentEngine);
                } catch (IOException | InvalidBEncodingException e) {
                    LOGGER.error("Torrent failed to initialize for " + torrentPath + " because of " + e.getMessage(), e);
                }

            }

            while (isRunning) {
                final Socket socket = serverSocket.accept();
                LOGGER.debug("New incoming connection from " + socket.getRemoteSocketAddress());
                new Thread(new IncomingConnectionsDispatcher(socket)).start();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to open socket on port " + localSocketAddress.getPort() + " because of " + e.getMessage(), e);
        }

    }


    @Nullable
    private TorrentEngine getEngineForInfoHash(byte[] infoHash) {
        for (TorrentEngine torrentEngine : torrentEngines) {
            if (ArrayUtils.isEquals(torrentEngine.getInfoHash(), infoHash)) {
                return torrentEngine;
            }
        }
        return null;
    }

    private class IncomingConnectionsDispatcher implements Runnable {

        private final Socket socket;

        private IncomingConnectionsDispatcher(Socket socket) {

            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream())) {
                HandshakeRequest handshakeRequest;

                handshakeRequest = receiveHandshake(inputStream);

                final TorrentEngine torrentEngine = getEngineForInfoHash(handshakeRequest.getInfoHash());

                if (torrentEngine != null) {
                    torrentEngine.addIncomingConnection(socket);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to open connection to " + socket.getRemoteSocketAddress() + " because of " + e.getMessage(), e);
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @NotNull
        private HandshakeRequest receiveHandshake(@NotNull BufferedInputStream inputStream) throws IOException {

            final int protocolStringLength = inputStream.read();
            final byte[] bytes = new byte[protocolStringLength + 8 + Torrent.INFO_HASH_LENGTH + Torrent.PEER_ID_LENGTH];
            final int read = readFromIS(inputStream, bytes);

            if (read < bytes.length) {
                throw new IOException("Handshake protocol error");
            }

            final byte[] handshakeReply = ArrayUtils.addAll(new byte[]{(byte) protocolStringLength}, bytes);
            return new HandshakeRequest(handshakeReply);
        }

        private int readFromIS(@NotNull BufferedInputStream inputStream, @NotNull byte[] buffer) throws IOException {
            int index = 0;

            while (index < buffer.length) {

                int amount = inputStream.read(buffer, index, buffer.length - index);


                index += amount;
            }
            return index;
        }

    }

    @Override
    public String toString() {
        return "TorrentClient{" +
                "torrentEngines=" + torrentEngines +
                '}';
    }
}
