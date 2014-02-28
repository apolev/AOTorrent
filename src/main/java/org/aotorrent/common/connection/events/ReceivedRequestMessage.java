package org.aotorrent.common.connection.events;

import org.aotorrent.common.connection.PeerConnection;

/**
 * User: dnapolov
 * Date: 2/28/14
 * Time: 8:17 PM
 */
public class ReceivedRequestMessage implements ConnectionMessage {
    private final int index;
    private final int begin;
    private final int length;

    public ReceivedRequestMessage(int index, int begin, int length) {
        this.index = index;
        this.begin = begin;
        this.length = length;
    }

    @Override
    public void processMessage(PeerConnection peerConnection) {
        peerConnection.sendBlock(index, begin, length);
    }
}
