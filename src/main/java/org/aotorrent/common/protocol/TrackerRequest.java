package org.aotorrent.common.protocol;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */

import org.apache.commons.codec.net.URLCodec;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class TrackerRequest {
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

    public TrackerRequest(URL url, byte[] infoHash, byte[] peerId, int port, long uploaded, long downloaded, long left, boolean compact, boolean noPeerId, int numWant, String trackerId) {
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

        StringBuilder requestString = new StringBuilder(url.toString())
                .append(firstSeparator)
                .append("info_hash=").append(new String(URLCodec.encodeUrl(null, infoHash)).replace("+", "%20")).append('&')
                .append("peer_id=").append(new String(URLCodec.encodeUrl(null, peerId))).append('&')
                .append("port=").append(port).append('&')
                .append("uploaded=").append(uploaded).append('&')
                .append("downloaded=").append(downloaded).append('&')
                .append("left=").append(left).append('&')
                .append("compact=").append((compact) ? "1" : "0").append('&')
                .append("event=").append(event).append('&')
                .append("numwant=").append(numWant).append('&')
                .append("trackerid=").append(trackerId);


        return new URL(requestString.toString());
    }

    private enum RequestEvent {
        STARTED("started"),
        STOPPED("stopped"),
        COMPLETED("completed");

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
