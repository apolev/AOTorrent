package org.aotorrent.common;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * User: dnapolov
 * Date: 11/26/13
 * Time: 1:01 PM
 */
public class TorrentFile {

    private final String path;

    private final long length;

    private final String md5sum;


    TorrentFile(File file) throws IOException {
        length = file.length();
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            md5sum = new String(DigestUtils.md5(fileInputStream), Torrent.DEFAULT_TORRENT_ENCODING);
        }
        path = file.getPath();
    }

    TorrentFile(String path, long length, String md5sum) {
        this.path = path;
        this.length = length;
        this.md5sum = md5sum;
    }

    public long getLength() {
        return length;
    }

    public String getMd5sum() {
        return md5sum;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "TorrentFile{" +
                "path='" + path + '\'' +
                ", length=" + length +
                '}';
    }
}
