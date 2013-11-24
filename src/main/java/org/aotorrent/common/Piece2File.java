package org.aotorrent.common;

import java.io.File;

public class Piece2File {

    long fileShift;
    long length;
    File file;

    public Piece2File(long fileShift, long length, File file) {
        this.fileShift = fileShift;
        this.length = length;
        this.file = file;
    }
}