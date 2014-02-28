package org.aotorrent.common.protocol.peer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by dmitry on 2/25/14.
 */
public class RequestRequest {
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

    public byte[] toTransmit() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4 + 1 + 12);

        bb.putInt(13);
        bb.put((byte) RequestType.REQUEST.requestCode);
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
}