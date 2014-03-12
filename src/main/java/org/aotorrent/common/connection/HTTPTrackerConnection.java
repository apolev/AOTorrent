package org.aotorrent.common.connection;

import org.aotorrent.client.TorrentEngine;
import org.aotorrent.common.bencode.InvalidBEncodingException;
import org.aotorrent.common.protocol.tracker.HTTPTrackerRequest;
import org.aotorrent.common.protocol.tracker.HTTPTrackerResponse;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Date;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class HTTPTrackerConnection extends AbstractTrackerConnection {
    private final boolean compact = true;
    private final int numWant = 50;

    public HTTPTrackerConnection(TorrentEngine torrentEngine, String url, byte[] infoHash, byte[] peerId, InetAddress ip, int port) {
        super(torrentEngine, url, infoHash, peerId, ip, port);
    }

    @Override
    protected void getPeers() throws MalformedURLException {
        HTTPTrackerRequest request = new HTTPTrackerRequest(new URL(url), infoHash, peerId, port, uploaded, downloaded, left, compact, noPeerId, numWant, trackerId);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) request.createRequest().openConnection();
            connection.connect();

            InputStream reply = connection.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(reply);
            HTTPTrackerResponse response = new HTTPTrackerResponse(bis);

            if (response.isFailed()) {
                nextRequest = new Date(System.currentTimeMillis() + (300 * 1000));
            } else {
                peers = response.getPeers();
                trackerId = response.getTrackerId();
                seeders = response.getComplete();
                leechers = response.getIncomplete();
                nextRequest = new Date(System.currentTimeMillis() + (response.getInterval() * 1000));
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
}
