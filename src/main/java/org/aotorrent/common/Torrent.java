package org.aotorrent.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.aotorrent.common.hash.Hasher;
import org.apache.commons.codec.digest.DigestUtils;
import org.bencode.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * User: dnapolov
 * Date: 11/6/13
 * Time: 2:26 PM
 */
public class Torrent {
    private static final Logger logger = LoggerFactory.getLogger(Torrent.class);
    private static final String DEFAULT_COMMENT = "no comments";
    private static final String DEFAULT_CREATED_BY = "Created by AOTorrent";
    public static final String DEFAULT_TORRENT_ENCODING = "ISO-8859-1";
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

        TorrentFile(String fileName) throws IOException {
            File file = new File(fileName);
            length = file.length();
            md5sum = new String(DigestUtils.md5(new FileInputStream(file)), DEFAULT_TORRENT_ENCODING);
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

    public void save(OutputStream outputStream) {
        Map<String, Value> torrentMap = Maps.newHashMap();
        torrentMap.put("announce", new Value(announce.get(0).get(0)));
        //TODO announceList
        torrentMap.put("comment", new Value(comment));
        torrentMap.put("creation date", new Value(creationDate.getTime()));
        torrentMap.put("created by", new Value(createdBy));

        if (singleFile) {
            //TODO
        } else {
            //TODO
        }
    }

}
