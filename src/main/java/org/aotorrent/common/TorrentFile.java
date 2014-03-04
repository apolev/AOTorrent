package org.aotorrent.common;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;

/**
 * User: dnapolov
 * Date: 11/26/13
 * Time: 1:01 PM
 */
public class TorrentFile {

    private final String path;

    private final long length;

    private final String md5sum;

    private long allocated = 0;

    TorrentFile(File file) throws IOException, FileNotFoundException, UnsupportedEncodingException {
        length = file.length();
        final FileInputStream fileInputStream = new FileInputStream(file);
        try {
            md5sum = new String(DigestUtils.md5(fileInputStream), Torrent.DEFAULT_TORRENT_ENCODING);
        } finally {
            fileInputStream.close();
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
