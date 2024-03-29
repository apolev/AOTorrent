package org.aotorrent.common.protocol.peer;

import com.google.common.primitives.Ints;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by dmitry on 2/25/14.
 */
public class PieceRequest implements PeerRequest {

    private final int index;
    private final int begin;
    private final byte[] block;

    public PieceRequest(byte[] message) {
        ByteBuffer bb = ByteBuffer.wrap(message);

        index = bb.getInt();
        begin = bb.getInt();
        block = new byte[message.length - 4 - 4];
        bb.get(block);
    }

    public PieceRequest(int index, int begin, byte[] block) {
        this.index = index;
        this.begin = begin;
        this.block = block;
    }

    @Override
    public byte[] toTransmit() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(Ints.BYTES * 3 + 1 + block.length);

        bb.putInt(Ints.BYTES * 2 + 1 + block.length);
        bb.put((byte) RequestType.PIECE.getRequestCode());
        bb.putInt(index);
        bb.putInt(begin);
        bb.put(block);

        return bb.array();
    }

    public int getIndex() {
        return index;
    }

    public int getBegin() {
        return begin;
    }

    public byte[] getBlock() {
        return block;
    }

    @Override
    public String toString() {
        return "PieceRequest{" +
                "index=" + index +
                ", begin=" + begin +
                ", blockSize=" + block.length +
                '}';
    }
}
