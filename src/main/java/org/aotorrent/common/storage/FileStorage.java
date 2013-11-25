package org.aotorrent.common.storage;

import org.aotorrent.common.Torrent;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * User: dnapolov
 * Date: 11/25/13
 * Time: 6:17 PM
 */
public class FileStorage {

    private final Torrent torrent;
    private final File path;

    public FileStorage(Torrent torrent, File path) {
        this.torrent = torrent;
        this.path = path;
    }

    public void store(int pieceIndex, ByteBuffer byteBuffer) {

    }
}
