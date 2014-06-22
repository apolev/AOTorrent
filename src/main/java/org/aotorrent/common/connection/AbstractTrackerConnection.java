package org.aotorrent.common.connection;

import org.aotorrent.client.TorrentEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.Set;

/**
 * Created by dmitry on 3/5/14.
 */
public abstract class AbstractTrackerConnection implements Runnable {
    protected static final int NUM_WANT = 50;
    protected static final Logger LOGGER = LoggerFactory.getLogger(HTTPTrackerConnection.class);
    private final TorrentEngine torrentEngine;
    private final byte[] infoHash;
    private final byte[] peerId;
    private final int port;
    private final InetAddress ip;
    private final boolean noPeerId = true;
    private String url;
    private boolean shutdown = false;
    private long uploaded = 0;
    private long downloaded = 0;
    private long left = 0;
    private long seeders = 0;
    private long leechers = 0;
    private String event = "started";
    private String trackerId = null;
    private Date nextRequest = null;
    private Set<InetSocketAddress> peers;

    public AbstractTrackerConnection(TorrentEngine torrentEngine, String url, byte[] infoHash, byte[] peerId, InetAddress ip, int port) {
        this.url = url;
        this.peerId = peerId;
        this.ip = ip;
        this.infoHash = infoHash;
        this.port = port;
        this.torrentEngine = torrentEngine;
    }

    @Override
    public void run() {
        while (!isShutdown() && !Thread.interrupted()) {
            try {
                if (getNextRequest() != null && getNextRequest().after(new Date())) {
                    Thread.sleep(getNextRequest().getTime() - new Date().getTime());
                } else {
                    obtainPeers();
                    LOGGER.debug("Starting to get peers from " + url);
                    final Set<InetSocketAddress> peers = getPeers();
                    LOGGER.debug("Got peers " + peers);
                    torrentEngine.mergePeers(peers);
                }
            } catch (InterruptedException e) {
                LOGGER.debug("Interrupted!");
            } catch (MalformedURLException e) {
                LOGGER.error("Malformed URL");
            }
        }
    }

    protected abstract void obtainPeers() throws MalformedURLException;

    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }

    @Nullable
    public static AbstractTrackerConnection createConnection(TorrentEngine torrentEngine, String url, byte[] infoHash, byte[] peerId, InetAddress ip, int port) {
        if ("http".equalsIgnoreCase(url.substring(0, 4))) {
            return new HTTPTrackerConnection(torrentEngine, url, infoHash, peerId, ip, port);
        } else if ("udp".equalsIgnoreCase(url.substring(0, 3))) {
            return new UDPTrackerConnection(torrentEngine, url, infoHash, peerId, ip, port);
        }

        return null;
    }

    protected TorrentEngine getTorrentEngine() {
        return torrentEngine;
    }

    protected byte[] getInfoHash() {
        return infoHash;
    }

    protected byte[] getPeerId() {
        return peerId;
    }

    protected int getPort() {
        return port;
    }

    protected boolean isNoPeerId() {
        return noPeerId;
    }

    protected String getTrackerId() {
        return trackerId;
    }

    protected void setTrackerId(String trackerId) {
        this.trackerId = trackerId;
    }

    protected void setLeechers(long leechers) {
        this.leechers = leechers;
    }

    protected void setSeeders(long seeders) {
        this.seeders = seeders;
    }

    protected long getLeft() {
        return left;
    }

    protected long getDownloaded() {
        return downloaded;
    }

    protected long getUploaded() {
        return uploaded;
    }

    protected boolean isShutdown() {
        return shutdown;
    }

    protected String getUrl() {
        return url;
    }

    protected Date getNextRequest() {
        return nextRequest;
    }

    protected void setNextRequest(Date nextRequest) {
        this.nextRequest = nextRequest;
    }

    protected Set<InetSocketAddress> getPeers() {
        return peers;
    }

    protected void setPeers(Set<InetSocketAddress> peers) {
        this.peers = peers;
    }
}
