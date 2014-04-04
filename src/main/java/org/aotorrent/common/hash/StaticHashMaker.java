package org.aotorrent.common.hash;

import com.google.common.collect.Lists;
import org.aotorrent.common.Piece;
import org.aotorrent.common.Torrent;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * User: dnapolov
 * Date: 11/6/13
 * Time: 4:29 PM
 */
public class StaticHashMaker {

    private StaticHashMaker() {
    }

    public static String getPieces(Iterable<File> files, long offset, int pieceLength) throws IOException, ExecutionException, InterruptedException, FileNotFoundException, UnsupportedEncodingException {

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Semaphore semaphore = new Semaphore(Runtime.getRuntime().availableProcessors());

        List<Future<byte[]>> piecesList = Lists.newArrayList();

        int bytesRemain = pieceLength;
        int counter = 0;

        byte[] buffer = new byte[pieceLength];

        for (File file : files) {

            try (BufferedInputStream bIS = new BufferedInputStream(new FileInputStream(file))) {
                if (counter == 0) {
                    //noinspection ResultOfMethodCallIgnored
                    bIS.skip(offset);
                }

                while (bIS.available() > 0) {
                    byte[] addition;
                    if (bytesRemain == pieceLength) { //newPiece

                        while (bIS.available() >= bytesRemain) {
                            buffer = new byte[pieceLength];
                            if (bIS.read(buffer) != pieceLength) {
                                throw new IOException("Some strange error. Read less than expected.");
                            }
                            semaphore.acquire();
                            Callable<byte[]> ht = new HashMakerThread(buffer, semaphore);
                            piecesList.add(executor.submit(ht));
                        }

                        if (bIS.available() < bytesRemain) {
                            buffer = new byte[pieceLength];
                            int read = bIS.read(buffer);
                            bytesRemain -= read;
                        }
                    } else {

                        if (bIS.available() >= bytesRemain) {
                            addition = new byte[bytesRemain];
                            if (bIS.read(addition) != bytesRemain) {
                                throw new IOException("Some strange error. Read less than expected.");
                            }
                            System.arraycopy(addition, 0, buffer, pieceLength - bytesRemain, addition.length);

                            semaphore.acquire();
                            Callable<byte[]> ht = new HashMakerThread(buffer, semaphore);
                            piecesList.add(executor.submit(ht));

                            bytesRemain = pieceLength;
                        } else {
                            addition = new byte[bIS.available()];
                            int read = bIS.read(buffer);
                            bytesRemain -= read;
                            System.arraycopy(addition, 0, buffer, pieceLength - bytesRemain, addition.length);
                        }
                    }
                }
            }
            counter++;
        }

        StringBuilder sb = new StringBuilder(piecesList.size() * Piece.PIECE_HASH_LENGTH);

        for (Future<byte[]> piece : piecesList) {
            sb.append(new String(piece.get(), Torrent.DEFAULT_TORRENT_ENCODING));
        }
        executor.shutdown();
        return sb.toString();
    }

    private static class HashMakerThread implements Callable<byte[]> {

        private final byte[] data;
        private final Semaphore semaphore;

        private HashMakerThread(byte[] data, Semaphore semaphore) {
            this.data = data;
            this.semaphore = semaphore;
        }

        @Override
        public byte[] call() throws Exception {
            try {
                return DigestUtils.sha1(data);
            } finally {
                semaphore.release();
            }
        }
    }
}
