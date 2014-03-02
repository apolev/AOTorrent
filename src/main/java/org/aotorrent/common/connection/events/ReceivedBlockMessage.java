package org.aotorrent.common.connection.events;

import org.aotorrent.common.Piece;
import org.aotorrent.common.connection.PeerConnection;
import org.jetbrains.annotations.NotNull;

/**
 * User: dnapolov
 * Date: 2/28/14
 * Time: 7:06 PM
 */
public class ReceivedBlockMessage implements ConnectionMessage {
    @NotNull
    private final Piece piece;

    public ReceivedBlockMessage(@NotNull Piece piece) {
        this.piece = piece;
    }

    @NotNull
    public Piece getPiece() {
        return piece;
    }

    @Override
    public void processMessage(PeerConnection peerConnection) {
        peerConnection.processMessage(this);
    }
}
