package org.aotorrent.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.aotorrent.common.bencode.BEncodeParser;
import org.aotorrent.common.bencode.BEncodeValue;
import org.aotorrent.common.bencode.BEncodeWriter;
import org.aotorrent.common.bencode.InvalidBEncodingException;
import org.aotorrent.common.hash.StaticHashMaker;
import org.aotorrent.common.storage.FileStorage;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * User: dnapolov
 * Date: 11/6/13
 * Time: 2:26 PM
 */
public class Torrent {
    private static final Logger LOGGER = LoggerFactory.getLogger(Torrent.class);
    private static final String DEFAULT_COMMENT = "no comments";
    private static final String DEFAULT_CREATED_BY = "AOTorrent";
    public static final String DEFAULT_TORRENT_ENCODING = "ISO-8859-1";
    public static final int DEFAULT_PIECE_LENGTH = 512 * 1024;
    public static final int INFO_HASH_LENGTH = 20;
    public static final int PEER_ID_LENGTH = 20;

    private final List<List<String>> announce;

    private final Date creationDate = new Date();
    private final String comment;
    private final String createdBy;
    private final String encoding = DEFAULT_TORRENT_ENCODING;
    private String downloadPath = "./";
    private int pieceLength = DEFAULT_PIECE_LENGTH;
    private final String pieces;  //TODO should be array of bytes
    private final boolean singleFile = false;
    private final File root;
    private final List<TorrentFile> files;
    private final byte[] infoHash;
    private final long size;
    private final FileStorage fileStorage;

    public static Torrent create(String announce, String fileName) throws IOException, InvalidBEncodingException, ExecutionException, InterruptedException, FileNotFoundException, UnsupportedEncodingException {
        List<String> announcesIn = Lists.newArrayList(announce);
        List<List<String>> announcesOut = Lists.newArrayList();
        announcesOut.add(announcesIn);

        File file = new File(fileName);
        List<File> fileNames = Lists.newArrayList(file);

        return new Torrent(announcesOut, file.getParentFile(), fileNames, null, null);
    }

    private Torrent(@NotNull final List<List<String>> announce, File root, @NotNull Iterable<File> files, @Nullable String comment, @Nullable String createdBy) throws IOException, InvalidBEncodingException, ExecutionException, InterruptedException, FileNotFoundException, UnsupportedEncodingException {
        this.announce = announce;
        this.root = root;
        List<TorrentFile> torrentFiles = Lists.newArrayList();
        long allFilesSize = 0;


        for (File fileName : files) {
            TorrentFile torrentFile = new TorrentFile(fileName);
            torrentFiles.add(torrentFile);
            allFilesSize += torrentFile.getLength();
        }

        this.size = allFilesSize;
        this.pieces = StaticHashMaker.getPieces(files, 0, pieceLength);

        this.files = torrentFiles;

        this.comment = (comment != null) ? comment : DEFAULT_COMMENT;
        this.createdBy = (createdBy != null) ? createdBy : DEFAULT_CREATED_BY;
        this.infoHash = craftInfoHash();
        this.fileStorage = new FileStorage(torrentFiles, pieceLength, new File("."));
    }

    public Torrent(@NotNull final InputStream is, String downloadPath) throws IOException, InvalidBEncodingException {
        this.downloadPath = downloadPath;
        Map<String, BEncodeValue> parsed = BEncodeParser.parse(is);
        String announceURL = parsed.get("announce").getString();
        announce = Lists.<List<String>>newArrayList(Lists.newArrayList(announceURL));
        comment = String.valueOf(parsed.get("comment"));
        createdBy = String.valueOf(parsed.get("created by"));
        Map<String, BEncodeValue> info = parsed.get("info").getMap();
        root = new File(info.get("name").getString());
        pieces = info.get("pieces").getString();
        pieceLength = (int) info.get("piece length").getLong();

        files = Lists.newArrayList();
        long allFilesSize = 0;

        if (info.get("files") != null) {
            for (BEncodeValue file : info.get("files").getList()) {
                Map<String, BEncodeValue> valueMap = file.getMap();

                TorrentFile tf = new TorrentFile(
                        downloadPath + StringUtils.join(valueMap.get("path").getList(), File.separator),
                        valueMap.get("length").getLong(),
                        "");

                files.add(tf);

                allFilesSize += tf.getLength();
            }
            this.size = allFilesSize;
        } else {
            TorrentFile tf = new TorrentFile(
                    info.get("name").getString(),
                    info.get("length").getLong(),
                    "");
            files.add(tf);
            this.size = info.get("length").getLong();
        }


        infoHash = getHash(info);

        this.fileStorage = new FileStorage(files, pieceLength, new File("."));
    }

    public void save(OutputStream outputStream) throws IOException, InvalidBEncodingException, UnsupportedEncodingException {
        Map<String, BEncodeValue> torrentMap = Maps.newHashMap();
        torrentMap.put("announce", new BEncodeValue(announce.get(0).get(0)));
        //TODO announceList
        torrentMap.put("comment", new BEncodeValue(comment));
        torrentMap.put("creation date", new BEncodeValue(creationDate.getTime() / 1000));
        torrentMap.put("created by", new BEncodeValue(createdBy));


        if (singleFile) {
            //TODO
        } else {
            Map<String, BEncodeValue> info = generateInfo();
            torrentMap.put("info", new BEncodeValue(info));
        }

        BEncodeWriter.writeOut(outputStream, torrentMap);
    }

    private Map<String, BEncodeValue> generateInfo() {
        Map<String, BEncodeValue> info = Maps.newHashMap();
        String name = (root != null) ? root.getName() : files.get(0).getPath();  //TODO proper name creation
        info.put("name", new BEncodeValue(name));
        info.put("piece length", new BEncodeValue(DEFAULT_PIECE_LENGTH));

        info.put("pieces", new BEncodeValue(pieces));

        List<BEncodeValue> filesList = Lists.newArrayList();

        for (TorrentFile file : files) {
            Map<String, BEncodeValue> fileInfo = Maps.newHashMap();

            fileInfo.put("length", new BEncodeValue(file.getLength()));
            fileInfo.put("md5sum", new BEncodeValue(file.getMd5sum()));
            List<String> pathElements = Arrays.asList(file.getPath().split("/"));
            List<BEncodeValue> pathElementsValue = Lists.newArrayList();
            for (String pieceOfPath : pathElements) {
                pathElementsValue.add(new BEncodeValue(pieceOfPath));
            }
            fileInfo.put("path", new BEncodeValue(pathElementsValue));

            filesList.add(new BEncodeValue(fileInfo));
        }
        info.put("files", new BEncodeValue(filesList));
        return info;
    }

    private byte[] craftInfoHash() throws IOException, InvalidBEncodingException {
        Map<String, BEncodeValue> info = generateInfo();

        return getHash(info);
    }

    public Collection<String> getTrackers() {
        List<String> trackerUrls = Lists.newArrayList();

        for (List<String> list : announce) {
            for (String trackerUrl : list) {
                trackerUrls.add(trackerUrl);
            }
        }

        return trackerUrls;
    }

    private byte[] getHash(Map<String, BEncodeValue> info) throws IOException, InvalidBEncodingException, UnsupportedEncodingException {

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        BEncodeWriter.writeOut(os, info);

        return DigestUtils.sha1(os.toByteArray());
    }

    public byte[] getInfoHash() {
        return infoHash;
    }


    public long getSize() {
        return size;
    }

    public String getPieces() {
        return pieces;
    }

    public FileStorage getFileStorage() {
        return fileStorage;
    }

    @Override
    public String toString() {
        return "Torrent{" +
                "announce=" + announce +
                ", creationDate=" + creationDate +
                ", comment='" + comment + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", root=" + root +
                ", files=" + files +
                '}';
    }

    public int getPieceLength() {
        return pieceLength;
    }
}
