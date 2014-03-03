package org.aotorrent.common.connection.events;

import org.aotorrent.common.connection.PeerConnection;

/**
 * Created by dmitry on 3/3/14.
 */
public class ReceivedHaveMessage implements ConnectionMessage {
    @Override
    public void processMessage(PeerConnection peerConnection) {
        peerConnection.processMessage(this);
    }
}
