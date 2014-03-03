package org.aotorrent.common.protocol.peer;

import java.nio.ByteBuffer;

/**
 * Created by dmitry on 2/25/14.
 */
public class ChokeRequest implements PeerRequest {
    @Override
    public byte[] toTransmit() {
        ByteBuffer bb = ByteBuffer.allocate(4 + 1);

        bb.putInt(1);
        bb.put((byte) RequestType.CHOKE.requestCode);

        return bb.array();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
