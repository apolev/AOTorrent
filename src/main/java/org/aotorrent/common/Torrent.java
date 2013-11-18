package org.aotorrent.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.aotorrent.common.bencode.InvalidBEncodingException;
import org.aotorrent.common.bencode.Value;
import org.aotorrent.common.bencode.Writer;
import org.aotorrent.common.hash.Hasher;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * User: dnapolov
 * Date: 11/6/13
 * Time: 2:26 PM
 */
public class Torrent {
    private static final Logger logger = LoggerFactory.getLogger(Torrent.class);
    private static final String DEFAULT_COMMENT = "no comments";
    private static final String DEFAULT_CREATED_BY = "AOTorrent";
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
    private final File root;
    private final List<TorrentFile> files;
    private final byte[] infoHash;

    private class TorrentFile {


        private final long length;
        private final String md5sum;
        private final String path;

        TorrentFile(File fileName) throws IOException {
            length = fileName.length();
            md5sum = new String(DigestUtils.md5(new FileInputStream(fileName)), DEFAULT_TORRENT_ENCODING);
            path = fileName.getPath();
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

        File file = new File(fileName);
        List<File> fileNames = Lists.newArrayList(file);

        return new Torrent(announcesOut, file.getParentFile(), fileNames, null, null);
    }

    public Torrent(@NotNull final List<List<String>> announce, File root, @NotNull List<File> files, @Nullable String comment, @Nullable String createdBy) throws IOException {

        this.announce = announce;
        this.root = root;
        List<TorrentFile> torrentFiles = Lists.newArrayList();

        for (File fileName : files) {
            TorrentFile torrentFile = new TorrentFile(fileName);
            torrentFiles.add(torrentFile);
        }

        this.pieces = Hasher.getPieces(files, pieceLength);

        this.files = torrentFiles;

        this.comment = (comment != null) ? comment : DEFAULT_COMMENT;
        this.createdBy = (createdBy != null) ? createdBy : DEFAULT_CREATED_BY;
        this.infoHash = getInfoHash();

    }

    public void save(OutputStream outputStream) throws IOException, InvalidBEncodingException {
        Map<String, Value> torrentMap = Maps.newHashMap();
        torrentMap.put("announce", new Value(announce.get(0).get(0)));
        //TODO announceList
        torrentMap.put("comment", new Value(comment));
        torrentMap.put("creation date", new Value(creationDate.getTime() / 1000));
        torrentMap.put("created by", new Value(createdBy));


        if (singleFile) {
            //TODO
        } else {
            Map<String, Value> info = generateInfo();
            torrentMap.put("info", new Value(info));
        }

        Writer.writeOut(outputStream, torrentMap);
    }

    private Map<String, Value> generateInfo() {
        Map<String, Value> info = Maps.newHashMap();
        String name = (root != null) ? root.getName() : files.get(0).getPath();  //TODO proper name creation
        info.put("name", new Value(name));
        info.put("piece length", new Value(DEFAULT_PIECE_LENGTH));

        info.put("pieces", new Value(pieces));

        List<Value> filesList = Lists.newArrayList();

        for (TorrentFile file : files) {
            Map<String, Value> fileInfo = Maps.newHashMap();

            fileInfo.put("length", new Value(file.getLength()));
            fileInfo.put("md5sum", new Value(file.getMd5sum()));
            List<String> splittedPath = Arrays.asList(file.getPath().split("/"));
            List<Value> splittedPathValue = Lists.newArrayList();
            for (String pieceOfPath : splittedPath) {
                splittedPathValue.add(new Value(pieceOfPath));
            }
            fileInfo.put("path", new Value(splittedPathValue));

            filesList.add(new Value(fileInfo));
        }
        info.put("files", new Value(filesList));
        return info;
    }

    public byte[] getInfoHash() {
        Map<String, Value> info = generateInfo();

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            Writer.writeOut(os, info);
            return os.toByteArray();
        } catch (IOException | InvalidBEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Set<URL> getTrackers() {
        Set<URL> trackerUrls = Sets.newLinkedHashSet();

        for (List<String> list : announce) {
            for (String trackerUrl : list) {
                try {
                    URL url = new URL(trackerUrl);
                    trackerUrls.add(url);
                } catch (MalformedURLException e) {
                    //Ignoring this url
                }

            }
        }

        return trackerUrls;
    }


}
