package org.aotorrent.common.connection.events;

import org.aotorrent.common.connection.PeerConnection;

/**
 * User: dnapolov
 * Date: 2/28/14
 * Time: 7:59 PM
 */
public class UnchokeMessage implements ConnectionMessage {
    @Override
    public void processMessage(PeerConnection peerConnection) {
        peerConnection.processMessage(this);
    }
}
