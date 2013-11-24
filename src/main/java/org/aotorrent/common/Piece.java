package org.aotorrent.common;

import java.util.Map;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class Piece {
    public static int DEFAULT_BLOCK_LENGTH = 16384;
    //TODO make piece without data and file-knowing, separate iece and data storage
    private final Map<Long, Piece2File> piece2FileMap;
    private byte[] data;
    private boolean[] finished;

    public Piece(Map<Long, Piece2File> piece2FileMap) {
        this.piece2FileMap = piece2FileMap;
        data = new byte[Torrent.DEFAULT_PIECE_LENGTH];
    }

    public Piece(int length, Map<Long, Piece2File> piece2FileMap) {
        this.piece2FileMap = piece2FileMap;
        data = new byte[length]; //TODO
    }

    public void write(byte[] data, int offset) {
        if (data.length % DEFAULT_BLOCK_LENGTH > 0) {
            return;
        }
        System.arraycopy(data, 0, this.data, offset * DEFAULT_BLOCK_LENGTH, data.length);//TODO
    }

}
