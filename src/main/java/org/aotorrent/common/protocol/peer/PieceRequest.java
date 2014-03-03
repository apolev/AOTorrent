package org.aotorrent.common.protocol.peer;

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
        ByteBuffer bb = ByteBuffer.allocate(4 + 1 + 4 + 4 + block.length);

        bb.putInt(1 + 4 + 4 + block.length);
        bb.put((byte) RequestType.PIECE.requestCode);
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
