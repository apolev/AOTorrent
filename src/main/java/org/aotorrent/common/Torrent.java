package org.aotorrent.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.aotorrent.common.bencode.InvalidBEncodingException;
import org.aotorrent.common.bencode.Parser;
import org.aotorrent.common.bencode.Value;
import org.aotorrent.common.bencode.Writer;
import org.aotorrent.common.hash.Hasher;
import org.aotorrent.common.storage.FileStorage;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
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

    private final List<List<String>> announce;

    private final Date creationDate = new Date();
    private final String comment;
    private final String createdBy;
    private final String encoding = DEFAULT_TORRENT_ENCODING;
    private final int pieceLength = DEFAULT_PIECE_LENGTH;
    private final String pieces;  //TODO should be array of bytes
    private final boolean singleFile = false;
    private final File root;
    private final List<TorrentFile> files;
    private final byte[] infoHash;
    private final long size;
    private final FileStorage fileStorage;

    public static Torrent create(String announce, String fileName) throws IOException, InvalidBEncodingException, ExecutionException, InterruptedException {
        List<String> announcesIn = Lists.newArrayList(announce);
        List<List<String>> announcesOut = Lists.newArrayList();
        announcesOut.add(announcesIn);

        File file = new File(fileName);
        List<File> fileNames = Lists.newArrayList(file);

        return new Torrent(announcesOut, file.getParentFile(), fileNames, null, null);
    }

    public Torrent(@NotNull final List<List<String>> announce, File root, @NotNull List<File> files, @Nullable String comment, @Nullable String createdBy) throws IOException, InvalidBEncodingException, ExecutionException, InterruptedException {
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
        this.pieces = Hasher.getPieces(files, 0, pieceLength);

        this.files = torrentFiles;

        this.comment = (comment != null) ? comment : DEFAULT_COMMENT;
        this.createdBy = (createdBy != null) ? createdBy : DEFAULT_CREATED_BY;
        this.infoHash = craftInfoHash();
        this.fileStorage = new FileStorage(torrentFiles, pieceLength, new File("."));
    }

    public Torrent(@NotNull final InputStream is) throws IOException, InvalidBEncodingException {
        Map<String, Value> parsed = Parser.parse(is);
        String announceURL = parsed.get("announce").getString();
        announce = Arrays.asList((List<String>) Arrays.asList(announceURL));
        comment = parsed.get("comment").getString();
        createdBy = parsed.get("created by").getString();
        Map<String, Value> info = parsed.get("info").getMap();
        root = new File(info.get("name").getString());
        pieces = info.get("pieces").getString();

        files = Lists.newArrayList();
        long allFilesSize = 0;

        for (Value file : info.get("files").getList()) {
            Map<String, Value> valueMap = file.getMap();

            TorrentFile tf = new TorrentFile(
                    StringUtils.join(valueMap.get("path").getList(), '\\'),
                    valueMap.get("length").getLong(),
                    "");

            files.add(tf);

            allFilesSize = allFilesSize + tf.getLength();
        }

        this.size = allFilesSize;

        infoHash = getHash(info);

        this.fileStorage = new FileStorage(files, pieceLength, new File("."));
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

    public byte[] craftInfoHash() throws IOException, InvalidBEncodingException {
        Map<String, Value> info = generateInfo();

        return getHash(info);
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

    private byte[] getHash(Map<String, Value> info) throws IOException, InvalidBEncodingException {

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        Writer.writeOut(os, info);

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
}
