package org.aotorrent.common.storage;

import com.google.common.collect.Lists;
import org.aotorrent.common.TorrentFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.List;

/**
 * User: dnapolov
 * Date: 11/25/13
 * Time: 6:17 PM
 */
public class FileStorage {

    private final List<TorrentFile> files;
    private final int pieceLength;
    private final File path;

    private class StorageFilesInfo {

        private final File file;

        private final long pieceOffset;

        private final long fileOffset;

        private final long length;

        private StorageFilesInfo(File file, long pieceOffset, long fileOffset, long length) {
            this.file = file;
            this.pieceOffset = pieceOffset; //starting index of piece
            this.fileOffset = fileOffset;
            this.length = length;
        }

        public File getFile() {
            return file;
        }

        public long getPieceOffset() {
            return pieceOffset;
        }

        public long getFileOffset() {
            return fileOffset;
        }

        public long getLength() {
            return length;
        }
    }

    public FileStorage(List<TorrentFile> files, int pieceLength, File path) {
        this.files = files;
        this.pieceLength = pieceLength;
        this.path = path;
    }

    public void store(int pieceIndex, ByteBuffer byteBuffer) throws IOException {
        Collection<StorageFilesInfo> storageFiles = getAffectedFiles(pieceIndex);

        for (StorageFilesInfo storageFile : storageFiles) {

            ByteBuffer buf = ByteBuffer.wrap(byteBuffer.array(), (int) storageFile.getPieceOffset(), (int) storageFile.getLength());

            final File file = storageFile.getFile();

            if (file.getParentFile() != null) {
                //noinspection ResultOfMethodCallIgnored
                file.getParentFile().mkdirs();
            }

            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file.getCanonicalPath(), "rw")) {
                try (FileChannel fileChannel = randomAccessFile.getChannel()) {
                    fileChannel.position(storageFile.getFileOffset());
                    fileChannel.write(buf);
                }
            }
        }
    }

    public byte[] read(int pieceIndex, int length) throws IOException {

        //long pieceStartPosition = pieceIndex * pieceLength; //Global in-torrent position index
        //long pieceEndPosition = pieceStartPosition + pieceLength;

        Collection<StorageFilesInfo> storageFiles = getAffectedFiles(pieceIndex);
        ByteBuffer buf = ByteBuffer.allocate(length);

        for (StorageFilesInfo storageFile : storageFiles) {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(storageFile.getFile(), "r")) {
                try (FileChannel fileChannel = randomAccessFile.getChannel()) {
                    ByteBuffer local = ByteBuffer.allocate((int) storageFile.getLength());
                    fileChannel.read(local, storageFile.getFileOffset());
                    local.flip();
                    buf.put(local);
                }
            }
/*
            if (storageFile.getFileOffset() <= pieceStartPosition && storageFile.getLength() + storageFile.getFileOffset() == pieceEndPosition) { //File occupies all piece
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(storageFile.getFile(), "rw")) {
                    try (FileChannel fileChannel = randomAccessFile.getChannel()) {
                        fileChannel.position(pieceStartPosition - storageFile.getFileOffset());
                        fileChannel.read(buf, pieceLength);
                    }
                }
            } else if (storageFile.getFileOffset() > pieceStartPosition && storageFile.getLength() + storageFile.getFileOffset() == pieceEndPosition) { //File lays in the end of piece
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(storageFile.getFile(), "rw")) {
                    try (FileChannel fileChannel = randomAccessFile.getChannel()) {
                        fileChannel.position(storageFile.getFileOffset() - pieceStartPosition);
                        fileChannel.read(buf, pieceEndPosition - storageFile.getFileOffset());
                    }
                }
            } else if (storageFile.getFileOffset() <= pieceStartPosition && storageFile.getLength() + storageFile.getFileOffset() <= pieceEndPosition) { //File starts or exactly fit into the piece
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(storageFile.getFile(), "rw")) {
                    try (FileChannel fileChannel = randomAccessFile.getChannel()) {
                        fileChannel.position(pieceStartPosition - storageFile.getFileOffset());
                        fileChannel.read(buf, storageFile.getLength() - (pieceStartPosition - storageFile.getFileOffset()));
                    }
                }
            } else if (storageFile.getFileOffset() > pieceStartPosition && storageFile.getLength() + storageFile.getFileOffset() < pieceEndPosition) { //File lays in piece
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(storageFile.getFile(), "rw")) {
                    try (FileChannel fileChannel = randomAccessFile.getChannel()) {
                        fileChannel.read(buf, storageFile.getLength());
                    }
                }
            }*/
        }
        return buf.array();
    }

    private Collection<StorageFilesInfo> getAffectedFiles(int pieceIndex) {
        long pieceStartPosition = pieceIndex * pieceLength;
        long pieceEndPosition = pieceStartPosition + pieceLength;
        long fileStartPosition = 0;
        long fileEndPosition;
        int fileIndex = 0;

        List<StorageFilesInfo> storageFilesInfos = Lists.newArrayList();

        while (fileIndex < files.size() && fileStartPosition < pieceEndPosition) {
            TorrentFile file = files.get(fileIndex);
            fileEndPosition = fileStartPosition + file.getLength();

            StorageFilesInfo sfi = null;

            if ((fileStartPosition <= pieceStartPosition) && (fileEndPosition > pieceStartPosition)) { // file starts earlier than piece, but included in it
                if (fileEndPosition >= pieceEndPosition) { // file takes whole piece
                    sfi = new StorageFilesInfo(new File(file.getPath()), 0, pieceStartPosition - fileStartPosition, pieceLength);
                } else if (fileEndPosition < pieceEndPosition) { // file takes first part of piece
                    sfi = new StorageFilesInfo(new File(file.getPath()), 0, pieceStartPosition - fileStartPosition, fileEndPosition - pieceStartPosition);
                }
            } else if ((fileStartPosition > pieceStartPosition) && (fileEndPosition < pieceEndPosition)) { // whole file is in piece
                sfi = new StorageFilesInfo(new File(file.getPath()), fileStartPosition - pieceStartPosition, 0, file.getLength());
            } else if ((fileStartPosition > pieceStartPosition) && (fileEndPosition > pieceEndPosition)) { // file ends the piece
                sfi = new StorageFilesInfo(new File(file.getPath()), fileStartPosition - pieceStartPosition, 0, pieceEndPosition - fileStartPosition);
            }

            if (sfi != null) {
                storageFilesInfos.add(sfi);
            }

            fileStartPosition = fileEndPosition;
            fileIndex++;
        }
        return storageFilesInfos;
    }
}
