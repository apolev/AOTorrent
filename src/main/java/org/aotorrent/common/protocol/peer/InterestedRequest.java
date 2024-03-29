package org.aotorrent.common.protocol.peer;

import com.google.common.primitives.Ints;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by dmitry on 2/25/14.
 */
public class InterestedRequest implements PeerRequest {
    @Override
    public byte[] toTransmit() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(Ints.BYTES + 1);

        bb.putInt(1);
        bb.put((byte) RequestType.INTERESTED.getRequestCode());

        return bb.array();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
