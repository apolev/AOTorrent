package org.aotorrent.common.connection.events;

import org.aotorrent.common.connection.PeerConnection;

import java.io.IOException;

/**
 * Created by dmitry on 3/14/14.
 */
public class PeerInterestedMessage implements ConnectionMessage {
    @Override
    public void processMessage(PeerConnection peerConnection) throws IOException {
        peerConnection.processMessage(this);
    }
}
