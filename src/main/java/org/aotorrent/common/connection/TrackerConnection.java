package org.aotorrent.common.connection;

import org.aotorrent.client.TorrentEngine;
import org.aotorrent.common.bencode.InvalidBEncodingException;
import org.aotorrent.common.protocol.TrackerRequest;
import org.aotorrent.common.protocol.TrackerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Date;
import java.util.Set;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class TrackerConnection implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackerConnection.class);
    private final TorrentEngine torrentEngine;
    private URL url;
    private final byte[] infoHash;
    private final byte[] peerId;
    private final int port;
    private long uploaded = 0;
    private long downloaded = 0;
    private long left = 0;
    private long seeders = 0;
    private long leechers = 0;
    private final boolean compact = true;
    private final boolean noPeerId = true;
    private String event = "started";
    private final InetAddress ip;
    private final int numWant = 50;
    private String trackerId = null;
    private Date nextRequest = null;
    private boolean shutdown = false;

    private Set<InetSocketAddress> peers;

    public TrackerConnection(TorrentEngine torrentEngine, URL url, byte[] infoHash, byte[] peerId, InetAddress ip, int port) {
        this.torrentEngine = torrentEngine;
        this.url = url;
        this.infoHash = infoHash;
        this.peerId = peerId;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                if (nextRequest != null && nextRequest.after(new Date())) {
                    Thread.sleep(nextRequest.getTime() - new Date().getTime());
                } else {
                    getPeers();
                }
            } catch (InterruptedException e) {
            }
        }
    }

    private void getPeers() {
        TrackerRequest request = new TrackerRequest(url, infoHash, peerId, port, uploaded, downloaded, left, compact, noPeerId, numWant, trackerId);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) request.createRequest().openConnection();
            connection.connect();

            InputStream reply = connection.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(reply);
            TrackerResponse response = new TrackerResponse(bis);

            if (response.isFailed()) {
                nextRequest = new Date(System.currentTimeMillis() + (300 * 1000));
            } else {
                peers = response.getPeers();
                trackerId = response.getTrackerId();
                seeders = response.getComplete();
                leechers = response.getIncomplete();
                nextRequest = new Date(System.currentTimeMillis() + (response.getInterval() * 1000));
                torrentEngine.mergePeers(peers);
            }

        } catch (IOException e) {
            synchronized (this) {
                nextRequest = new Date(System.currentTimeMillis() + (300 * 1000));
            }
        } catch (InvalidBEncodingException e) {
            synchronized (this) {
                nextRequest = new Date(System.currentTimeMillis() + (300 * 1000));
            }
        } catch (URISyntaxException e) {
            LOGGER.error("URI syntax error", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public Date getNextRequest() {
        return nextRequest;
    }


}
