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
    private static final boolean COMPACT = true;
    private static final int REQUEST_INTERVAL = (300 * 1000);

    public HTTPTrackerConnection(TorrentEngine torrentEngine, String url, byte[] infoHash, byte[] peerId, InetAddress ip, int port) {
        super(torrentEngine, url, infoHash, peerId, ip, port);
    }

    @Override
    protected void obtainPeers() throws MalformedURLException {
        HTTPTrackerRequest request = new HTTPTrackerRequest(new URL(getUrl()), getInfoHash(), getPeerId(), getPort(), getUploaded(), getDownloaded(), getLeft(), COMPACT, isNoPeerId(), NUM_WANT, getTrackerId());
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) request.createRequest().openConnection();
            connection.connect();

            InputStream reply = connection.getInputStream();
            HTTPTrackerResponse response;
            try (BufferedInputStream bis = new BufferedInputStream(reply)) {

                response = new HTTPTrackerResponse(bis);

                if (response.isFailed()) {
                    setNextRequest(new Date(System.currentTimeMillis() + REQUEST_INTERVAL));
                } else {
                    setPeers(response.getPeers());
                    setTrackerId(response.getTrackerId());
                    setSeeders(response.getComplete());
                    setLeechers(response.getIncomplete());
                    setNextRequest(new Date(System.currentTimeMillis() + (response.getInterval() * 1000)));
                }
            } catch (InvalidBEncodingException e) {
                synchronized (this) {
                    setNextRequest(new Date(System.currentTimeMillis() + REQUEST_INTERVAL));
                }
            }
        } catch (IOException e) {
            synchronized (this) {
                setNextRequest(new Date(System.currentTimeMillis() + REQUEST_INTERVAL));
            }
        } catch (URISyntaxException e) {
            throw new MalformedURLException(e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
