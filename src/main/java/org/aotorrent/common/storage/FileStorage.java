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
            this.pieceOffset = pieceOffset;
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

            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();

            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file.getCanonicalPath(), "rw")) {
                try (FileChannel fileChannel = randomAccessFile.getChannel()) {
                    fileChannel.position(storageFile.getFileOffset());
                    fileChannel.write(buf);
                }
            }

        }
    }

    public byte[] read(int pieceIndex, int offset, int length) throws IOException {

        Collection<StorageFilesInfo> storageFiles = getAffectedFiles(pieceIndex);
        int index = 0;
        ByteBuffer buf = ByteBuffer.allocate(length);

        for (StorageFilesInfo storageFile : storageFiles) {

            if (storageFile.getLength() < (offset - index)) {
                index += storageFile.getLength();
            } else if (index >= offset && index <= (offset + length)) {
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(path.getCanonicalPath() + File.separator + storageFile.getFile().getCanonicalPath(), "rw")) {
                    try (FileChannel fileChannel = randomAccessFile.getChannel()) {
                        int actuallyRead = fileChannel.read(buf, (index - offset));
                        index += actuallyRead;
                    }
                }
            } else if (storageFile.getLength() >= (offset - index) && index < offset && index <= (offset + length)) {
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(path.getCanonicalPath() + File.separator + storageFile.getFile().getCanonicalPath(), "rw")) {
                    try (FileChannel fileChannel = randomAccessFile.getChannel()) {
                        fileChannel.position(offset - index);
                        int actuallyRead = fileChannel.read(buf);
                        index += actuallyRead;
                    }
                }
            }
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
