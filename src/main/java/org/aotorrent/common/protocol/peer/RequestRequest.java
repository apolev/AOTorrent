package org.aotorrent.common.protocol.peer;

import com.google.common.primitives.Ints;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by dmitry on 2/25/14.
 */
public class RequestRequest implements PeerRequest {
    private final int index;
    private final int begin;
    private final int length;

    public RequestRequest(byte[] message) {
        ByteBuffer bb = ByteBuffer.wrap(message);
        index = bb.getInt();
        begin = bb.getInt();
        length = bb.getInt();
    }

    public RequestRequest(int index, int begin, int length) {
        this.index = index;
        this.begin = begin;
        this.length = length;
    }

    @Override
    public byte[] toTransmit() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(Ints.BYTES * 4 + 1);

        bb.putInt(Ints.BYTES * 3 + 1);
        bb.put((byte) RequestType.REQUEST.getRequestCode());
        bb.putInt(index);
        bb.putInt(begin);
        bb.putInt(length);

        return bb.array();
    }

    public int getIndex() {
        return index;
    }

    public int getBegin() {
        return begin;
    }

    public int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "RequestRequest{" +
                "index=" + index +
                ", begin=" + begin +
                ", length=" + length +
                '}';
    }
}
