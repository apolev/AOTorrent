package org.aotorrent.common.protocol.peer;

import com.google.common.primitives.Ints;

import java.nio.ByteBuffer;

/**
 * Created by dmitry on 2/25/14.
 */
public class ChokeRequest implements PeerRequest {
    @Override
    public byte[] toTransmit() {
        ByteBuffer bb = ByteBuffer.allocate(Ints.BYTES + 1);

        bb.putInt(1);
        bb.put((byte) RequestType.CHOKE.getRequestCode());

        return bb.array();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
