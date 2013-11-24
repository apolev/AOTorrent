package org.aotorrent.common.connection;

import org.aotorrent.common.bencode.InvalidBEncodingException;
import org.aotorrent.common.protocol.TrackerRequest;
import org.aotorrent.common.protocol.TrackerResponse;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Map;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class TrackerConnection implements Runnable {
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
    private final int numwant = 50;
    private String trackerId = null;
    private Date nextRequest = null;
    private boolean shutdown = false;

    private Map<InetAddress, Integer> peers;

    public TrackerConnection(URL url, byte[] infoHash, byte[] peerId, InetAddress ip, int port) {
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
        TrackerRequest request = new TrackerRequest(url, infoHash, peerId, port, uploaded, downloaded, left, compact, noPeerId, numwant, trackerId);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) request.createRequest().openConnection();
            connection.connect();

            InputStream reply = connection.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(reply);
            TrackerResponse response = new TrackerResponse(bis);
            synchronized (this) {
                if (response.isFailed()) {
                    nextRequest = new Date(System.currentTimeMillis() + (300 * 1000));
                } else {
                    peers = response.getPeers();
                    trackerId = response.getTrackerId();
                    seeders = response.getComplete();
                    leechers = response.getIncomplete();
                    nextRequest = new Date(System.currentTimeMillis() + (response.getInterval() * 1000));
                    //TODO Semaphore to TrackerEngine that i might have a new peers
                }
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
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
