package org.aotorrent.common.protocol;

import com.google.common.collect.Sets;
import org.aotorrent.common.bencode.InvalidBEncodingException;
import org.aotorrent.common.bencode.Parser;
import org.aotorrent.common.bencode.Value;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class TrackerResponse {


    //    failure reason: If present, then no other keys may be present. The value is a human-readable error message as to why the request failed (string).
    private final String failureReason;
    //    warning message: (new, optional) Similar to failure reason, but the response still gets processed normally. The warning message is shown just like an error.
    private final String warningMessage;
    //    interval: Interval in seconds that the client should wait between sending regular requests to the tracker
    private final int interval;
    //    min interval: (optional) Minimum announce interval. If present clients must not reannounce more frequently than this.
    private final int minInterval;
    //    tracker id: A string that the client should send back on its next announcements. If absent and a previous announce sent a tracker id, do not discard the old value; keep using it.
    private final String trackerId;
    //    complete: number of peers with the entire file, i.e. seeders (integer)
    private final int complete;
    //    incomplete: number of non-seeder peers, aka "leechers" (integer)
    private final int incomplete;
    //    peers: (dictionary model) The value is a list of dictionaries, each with the following keys:
    private final Set<InetSocketAddress> peers = Sets.newHashSet();
    //    peer id: peer's self-selected ID, as described above for the tracker request (string)
    //    ip: peer's IP address either IPv6 (hexed) or IPv4 (dotted quad) or DNS name (string)
    //    port: peer's port number (integer)
    //    peers: (binary model) Instead of using the dictionary model described above, the peers value may be a string consisting of multiples of 6 bytes. First 4 bytes are the IP address and last 2 bytes are the port number. All in network (big endian) notation.


    public TrackerResponse(InputStream data) throws IOException, InvalidBEncodingException {
        Map<String, Value> responseMap = Parser.parse(data);
        if (!responseMap.containsKey("failure reason")) {
            failureReason = null;
            warningMessage = (responseMap.containsKey("warning message")) ? responseMap.get("warning message").getString() : null;
            interval = (int) responseMap.get("interval").getLong();
            minInterval = (responseMap.containsKey("min interval")) ? (int) responseMap.get("min interval").getLong() : interval;
            trackerId = (responseMap.containsKey("tracker id")) ? responseMap.get("tracker id").getString() : null;
            complete = (responseMap.containsKey("complete")) ? (int) responseMap.get("complete").getLong() : 0;
            incomplete = (responseMap.containsKey("incomplete")) ? (int) responseMap.get("incomplete").getLong() : 0;

            if (responseMap.get("peers").getValueClass().equals("String")) {
                byte[] encodedPeers = responseMap.get("peers").getString().getBytes("ISO-8859-1");

                if ((encodedPeers.length % 6) > 0) {
                    throw new IllegalStateException("peers has strange length");  //TODO make good exception
                }

                for (int i = 0; i < (encodedPeers.length / 6); i++) {
                    byte[] rawIP = Arrays.copyOfRange(encodedPeers, i * 6, i * 6 + 4);
                    InetAddress ip = InetAddress.getByAddress(rawIP);

                    byte[] rawPort = Arrays.copyOfRange(encodedPeers, i * 6 + 4, i * 6 + 6);
                    int port = ((rawPort[0] << 8) & 0x0000ff00) | (rawPort[1] & 0x000000ff);
                    peers.add(new InetSocketAddress(ip, port));
                }
            }
        } else {
            failureReason = responseMap.get("failure reason").getString();
            warningMessage = null;
            interval = 0;
            minInterval = 0;
            trackerId = null;
            complete = 0;
            incomplete = 0;

        }
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public int getInterval() {
        return interval;
    }

    public int getMinInterval() {
        return minInterval;
    }

    public String getTrackerId() {
        return trackerId;
    }

    public int getComplete() {
        return complete;
    }

    public int getIncomplete() {
        return incomplete;
    }

    public Set<InetSocketAddress> getPeers() {
        return peers;
    }

    public boolean isFailed() {
        return failureReason != null;
    }
}
