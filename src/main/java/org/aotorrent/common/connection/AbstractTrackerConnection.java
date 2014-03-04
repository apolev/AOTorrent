package org.aotorrent.common.connection;

import org.aotorrent.client.TorrentEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Date;
import java.util.Set;

/**
 * Created by dmitry on 3/5/14.
 */
public abstract class AbstractTrackerConnection implements Runnable {
    protected static final Logger LOGGER = LoggerFactory.getLogger(HTTPTrackerConnection.class);
    protected final TorrentEngine torrentEngine;
    protected final byte[] infoHash;
    protected final byte[] peerId;
    protected final int port;
    protected final InetAddress ip;
    protected final boolean noPeerId = true;
    protected URL url;
    protected boolean shutdown = false;
    protected long uploaded = 0;
    protected long downloaded = 0;
    protected long left = 0;
    protected long seeders = 0;
    protected long leechers = 0;
    protected String event = "started";
    protected String trackerId = null;
    protected Date nextRequest = null;
    protected Set<InetSocketAddress> peers;

    public AbstractTrackerConnection(URL url, byte[] peerId, InetAddress ip, byte[] infoHash, int port, TorrentEngine torrentEngine) {
        this.url = url;
        this.peerId = peerId;
        this.ip = ip;
        this.infoHash = infoHash;
        this.port = port;
        this.torrentEngine = torrentEngine;
    }

    @Override
    public void run() {
        while (!shutdown && !Thread.interrupted()) {
            try {
                if (nextRequest != null && nextRequest.after(new Date())) {
                    Thread.sleep(nextRequest.getTime() - new Date().getTime());
                } else {
                    getPeers();
                }
            } catch (InterruptedException e) {
                LOGGER.debug("Interrupted!");
            }
        }
    }

    protected abstract void getPeers();

    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }
}
