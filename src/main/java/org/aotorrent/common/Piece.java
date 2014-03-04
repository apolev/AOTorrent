package org.aotorrent.common;

import org.aotorrent.common.storage.FileStorage;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(Piece.class);
    public static final int DEFAULT_BLOCK_LENGTH = 16384;
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
        blockComplete = new BitSet();
        this.hash = hash;
        this.storage = storage;
    }

    public void write(byte[] data, int offset) {
        if ((data.length + offset) > pieceLength) {
            LOGGER.error("Received block is bigger than piece");
            return; //TODO exception
        }

        /*if (data.length < DEFAULT_BLOCK_LENGTH) {
            return; //TODO Exception
        }*/

        if (buffer == null) {
            buffer = ByteBuffer.allocate(pieceLength);
        }

        buffer.position(offset);
        buffer.put(data);

        int dataBlocks = (int) Math.ceil((double) data.length / (double) DEFAULT_BLOCK_LENGTH);
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

    public byte[] read(int offset, int length) throws IOException, FileNotFoundException {
        return storage.read(index, offset, length);
    }

    private void checkIsComplete() { //TODO make this in separate thread
        try {
            if (isAllBlocksComplete()) {
                byte[] pieceHash = DigestUtils.sha1(buffer.array());

                if (Arrays.equals(pieceHash, hash)) {
                    storage.store(index, buffer);
                    complete = true;
                } else {
                    blockComplete.clear();
                }
                buffer = null;
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("Can't save piece", e);
        } catch (IOException e) {
            LOGGER.error("Can't save piece", e);
        }
    }

    private boolean isAllBlocksComplete() {
        return blockComplete.cardinality() == getBlockCount();
    }

    public int getPeerCount() {
        return peerCount;
    }

    public boolean isComplete() {
        return complete;
    }

    @Override
    public int compareTo(Piece otherPiece) {
        if (isComplete() == otherPiece.isComplete()) {
            if (peerCount == otherPiece.getPeerCount()) {
                return Arrays.hashCode(hash) - Arrays.hashCode(otherPiece.getHash());
            }
            return peerCount - otherPiece.getPeerCount();
        } else {
            if (isComplete()) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    public int getBlockCount() {
        return (int) Math.ceil((double) pieceLength / (double) DEFAULT_BLOCK_LENGTH);
    }

    public int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Piece piece = (Piece) o;

        if (index != piece.getIndex()) {
            return false;
        }
        if (pieceLength != piece.getPieceLength()) {
            return false;
        }
        return Arrays.equals(hash, piece.getHash());
    }

    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + pieceLength;
        result = 31 * result + Arrays.hashCode(hash);
        return result;
    }

    public byte[] getHash() {
        return hash;
    }

    public int getPieceLength() {
        return pieceLength;
    }
}
