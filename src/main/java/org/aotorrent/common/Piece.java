package org.aotorrent.common;

import org.aotorrent.common.storage.FileStorage;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

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

    private boolean[] blockComplete;
    private boolean complete = false;

    private int peerCount = 0;

    public Piece(int index, int pieceLength, byte[] hash, FileStorage storage) {
        this.index = index;
        this.pieceLength = pieceLength;
        blockComplete = new boolean[pieceLength / DEFAULT_BLOCK_LENGTH];
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

        buffer.put(data, offset, data.length);

        int dataBlocks = data.length / DEFAULT_BLOCK_LENGTH;
        int blockOffset = offset / DEFAULT_BLOCK_LENGTH;

        if (buffer == null) {
            buffer = ByteBuffer.allocate(pieceLength);
            buffer.flip();
        }

        for (int i = 0; i < dataBlocks; i++) {
            blockComplete[i + blockOffset] = true;
        }

        checkIsComplete();
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
                buffer.clear();
                Arrays.fill(blockComplete, Boolean.FALSE);
            }
        }
    }

    private boolean isAllBlocksComplete() {
        for (boolean b : blockComplete) if (!b) return false;
        return true;
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
            return peerCount - otherPiece.peerCount;
        } else {
            if (isComplete()) {
                return 1;
            } else {
                return -1;
            }
        }
    }
}
