package org.aotorrent.common.connection.events;

import org.aotorrent.common.connection.PeerConnection;

/**
 * User: dnapolov
 * Date: 2/28/14
 * Time: 5:53 PM
 */
public interface ConnectionMessage {
    public void processMessage(PeerConnection peerConnection);
}
