package org.aotorrent.common.connection.events;

import org.aotorrent.common.connection.PeerConnection;

/**
 * Created by dmitry on 3/4/14.
 */
public class SendHaveMessage implements ConnectionMessage {

    private final int pieceIndex;

    public SendHaveMessage(int pieceIndex) {
        this.pieceIndex = pieceIndex;
    }

    @Override
    public void processMessage(PeerConnection peerConnection) {
        peerConnection.processMessage(this);
    }

    public int getPieceIndex() {
        return pieceIndex;
    }
}
