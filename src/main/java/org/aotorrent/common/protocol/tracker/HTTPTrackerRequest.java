package org.aotorrent.common.protocol.tracker;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */

import org.apache.commons.codec.net.URLCodec;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class HTTPTrackerRequest {
    private final URL url;
    private final byte[] infoHash;
    private final byte[] peerId;
    private final int port;
    private long uploaded;
    private long downloaded;
    private long left;
    private final boolean compact;
    private final boolean noPeerId;
    private RequestEvent event = RequestEvent.STARTED;
    private final int numWant;
    private String trackerId;

    public HTTPTrackerRequest(URL url, byte[] infoHash, byte[] peerId, int port, long uploaded, long downloaded, long left, boolean compact, boolean noPeerId, int numWant, String trackerId) {
        this.url = url;
        this.infoHash = infoHash;
        this.peerId = peerId;
        this.port = port;
        this.uploaded = uploaded;
        this.downloaded = downloaded;
        this.left = left;
        this.compact = compact;
        this.noPeerId = noPeerId;
        this.numWant = numWant;
        this.trackerId = trackerId;
    }

    public URL createRequest() throws MalformedURLException, URISyntaxException {
        String filePart = url.getFile();
        Character firstSeparator = (filePart.contains("?")) ? '&' : '?';

        String requestString = url.toString()
                + firstSeparator
                + "info_hash="
                + new String(URLCodec.encodeUrl(null, infoHash)).replace("+", "%20")
                + '&' + "peer_id=" + new String(URLCodec.encodeUrl(null, peerId))
                + '&' + "port=" + port
                + '&' + "uploaded=" + uploaded
                + '&' + "downloaded=" + downloaded
                + '&' + "left=" + left
                + '&' + "compact=" + ((compact) ? "1" : "0")
                + '&' + "event=" + event
                + '&' + "numwant=" + numWant
                + '&' + "trackerid=" + trackerId;

        return new URL(requestString);
    }

    private enum RequestEvent {
        STARTED("started");

        private String event;

        RequestEvent(String event) {
            this.event = event;
        }

        @Override
        public String toString() {
            return event;
        }
    }
}
