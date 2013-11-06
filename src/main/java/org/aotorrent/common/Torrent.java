package org.aotorrent.common;

import com.google.common.collect.Lists;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.aotorrent.common.hash.Hasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * User: dnapolov
 * Date: 11/6/13
 * Time: 2:26 PM
 */
public class Torrent {
    private static final Logger logger = LoggerFactory.getLogger(Torrent.class);
    private static final String DEFAULT_COMMENT = "no comments";
    private static final String DEFAULT_CREATED_BY = "Created by AOTorrent";
    private static final String DEFAULT_TORRENT_ENCODING = "";
    private static final int DEFAULT_PIECE_LENGTH = 512 * 1024;

    private final List<List<String>> announce;
    private final Date creationDate = new Date();
    private final String comment;
    private final String createdBy;
    private final String encoding = DEFAULT_TORRENT_ENCODING;
    private final int pieceLength = DEFAULT_PIECE_LENGTH;
    private final String pieces;
    private final boolean singleFile = false;
    private final List<TorrentFile> files;


    private class TorrentFile {
        private final long length;
        private final String md5sum;
        private final String path;

        TorrentFile(String fileName) {
            length = 0;
            md5sum = "";
            path = fileName;
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
    }

    public static Torrent create(String announce, String fileName) throws IOException {
        List<String> announcesIn = Lists.newArrayList(announce);
        List<List<String>> announcesOut = Lists.newArrayList();
        announcesOut.add(announcesIn);

        List<String> fileNames = Lists.newArrayList(fileName);

        return new Torrent(announcesOut, fileNames, null, null);
    }

    public Torrent(@NotNull final List<List<String>> announce, @NotNull List<String> files, @Nullable String comment, @Nullable String createdBy) throws IOException {

        this.announce = announce;

        List<TorrentFile> torrentFiles = Lists.newArrayList();

        for (String fileName : files) {
            TorrentFile torrentFile = new TorrentFile(fileName);
            torrentFiles.add(torrentFile);
        }

        this.pieces = Hasher.getPieces(files, pieceLength);

        this.files = torrentFiles;

        this.comment = (comment != null) ? comment : DEFAULT_COMMENT;
        this.createdBy = (createdBy != null) ? createdBy : DEFAULT_CREATED_BY;


    }

}
