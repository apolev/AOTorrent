package org.aotorrent.common.protocol.peer;

import com.google.common.primitives.Ints;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by dmitry on 2/25/14.
 */
public class HaveRequest implements PeerRequest {
    private final int index;

    public HaveRequest(byte[] message) {
        final ByteBuffer bb = ByteBuffer.allocate(message.length).put(message);
        bb.flip();

        index = bb.getInt();

    }

    public HaveRequest(int index) {
        this.index = index;
    }

    @Override
    public byte[] toTransmit() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(Ints.BYTES * 2 + 1);

        bb.putInt(5);
        bb.put((byte) RequestType.HAVE.getRequestCode());
        bb.putInt(index);

        return bb.array();
    }

    public int getIndex() {
        return index;
    }
}
