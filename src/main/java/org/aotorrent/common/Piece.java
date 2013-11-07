package org.aotorrent.common;

import java.io.File;
import java.util.Map;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class Piece {

    private final Map<Integer, Piece2File> piece2FileMap;
    private byte[] data;

    public Piece(int length) {
        piece2FileMap = null;
        data = new byte[length]; //TODO
    }

    public void write(byte[] data, int offset) {
        System.arraycopy(data, 0, this.data, offset, data.length);//TODO
    }

    private class Piece2File {

        long fileShift;
        long length;
        File file;

        private Piece2File(long fileShift, long length, File file) {
            this.fileShift = fileShift;
            this.length = length;
            this.file = file;
        }
    }
}
