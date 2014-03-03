package org.aotorrent.common;

import org.aotorrent.common.storage.FileStorage;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class Piece implements Comparable<Piece> {

    public static int DEFAULT_BLOCK_LENGTH = 16384;
    public static final int PIECE_HASH_LENGTH = 20;

    private final int index;
    private final int pieceLength;
    private final FileStorage storage;
    private final byte[] hash;

    private ByteBuffer buffer;

    private BitSet blockComplete;
    private boolean complete = false;

    private int peerCount = 0;

    public Piece(int index, int pieceLength, byte[] hash, FileStorage storage) {
        this.index = index;
        this.pieceLength = pieceLength;
        blockComplete = new BitSet(pieceLength / DEFAULT_BLOCK_LENGTH);
        this.hash = hash;
        this.storage = storage;
    }

    public void write(byte[] data, int offset) {
        if ((data.length + offset) > pieceLength) {
            return; //TODO exception
        }

        if (data.length < DEFAULT_BLOCK_LENGTH) {
            return; //TODO Exception
        }

        if (buffer == null) {
            buffer = ByteBuffer.allocate(pieceLength);
        }

        buffer.position(offset);
        buffer.put(data);

        int dataBlocks = data.length / DEFAULT_BLOCK_LENGTH;
        int blockOffset = offset / DEFAULT_BLOCK_LENGTH;

        if (buffer == null) {
            buffer = ByteBuffer.allocate(pieceLength);
            buffer.flip();
        }

        for (int i = 0; i < dataBlocks; i++) {
            blockComplete.set(i + blockOffset);
        }

        checkIsComplete();
    }

    public byte[] read(int offset, int length) throws IOException {
        return storage.read(index, offset, length);
    }

    private void checkIsComplete() { //TODO make this in separate thread
        if (isAllBlocksComplete()) {
            byte[] pieceHash = DigestUtils.sha1(buffer.array());

            if (Arrays.equals(pieceHash, hash)) {
                try {
                    storage.store(index, buffer);
                    complete = true;
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            } else {
                blockComplete.clear();
            }
            buffer = null;
        }
    }

    private boolean isAllBlocksComplete() {
        return blockComplete.cardinality() == (pieceLength / DEFAULT_BLOCK_LENGTH);
    }

    public int getPeerCount() {
        return peerCount;
    }

    public void increasePeerCount() {
        this.peerCount++;
    }

    public boolean isComplete() {
        return complete;
    }

    @Override
    public int compareTo(Piece otherPiece) {
        if (isComplete() == otherPiece.isComplete()) {
            if (peerCount == otherPiece.peerCount) {
                return Arrays.hashCode(hash) - Arrays.hashCode(otherPiece.hash);
            }
            return peerCount - otherPiece.peerCount;
        } else {
            if (isComplete()) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    public int getNextEmptyBlockIndex() {
        final int clearBit = blockComplete.nextClearBit(0);
        return (clearBit <= getBlockCount()) ? clearBit : -1;
    }

    public int getBlockCount() {
        return (pieceLength / DEFAULT_BLOCK_LENGTH);
    }

    public boolean isClear() {
        return blockComplete.isEmpty();
    }

    public int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Piece piece = (Piece) o;

        if (index != piece.index) return false;
        if (pieceLength != piece.pieceLength) return false;
        if (!Arrays.equals(hash, piece.hash)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + pieceLength;
        result = 31 * result + Arrays.hashCode(hash);
        return result;
    }
}
