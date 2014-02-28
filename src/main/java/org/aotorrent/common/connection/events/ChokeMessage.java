package org.aotorrent.common.connection.events;

import org.aotorrent.common.connection.PeerConnection;

/**
 * User: dnapolov
 * Date: 2/28/14
 * Time: 7:38 PM
 */
public class ChokeMessage implements ConnectionMessage {
    @Override
    public void processMessage(PeerConnection peerConnection) {
        peerConnection.choke();
    }
}
