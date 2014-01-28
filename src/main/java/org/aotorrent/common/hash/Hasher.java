package org.aotorrent.common.hash;

import com.google.common.collect.Lists;
import org.aotorrent.common.Torrent;

import java.io.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * User: dnapolov
 * Date: 11/6/13
 * Time: 4:29 PM
 */
public class Hasher {

    public static void main(String[] args) throws Exception, ExecutionException, IOException, InterruptedException {
        final int PIECE_LENGTH = 512 * 1024;
        List<File> files = Lists.newArrayList();

        files.add(new File(args[0]));


        long start = System.currentTimeMillis();
        String hash = getPieces(files, 0, PIECE_LENGTH);
        long overall = System.currentTimeMillis() - start;

        int hashOfHash = 1439726342;

        if (hash.hashCode() != hashOfHash) {
            throw new Exception("hash is not right");
        }


        System.out.println("overall = " + overall);
    }

/*
    public static String getPieces(List<File> files, int pieceLength) throws IOException {
        StringBuilder pieces = new StringBuilder();
        byte[] buffer = new byte[pieceLength];

        int remainder = 0;

        for (File file : files) {
            FileInputStream fIS = new FileInputStream(file);
            BufferedInputStream bIS = new BufferedInputStream(fIS);

            if (remainder > 0) {
                //int available = bIS.
                if (bIS.available() >= (pieceLength - remainder)) {
                    byte[] addition = new byte[pieceLength - remainder];

                    if (bIS.read(addition) != addition.length) {
                        throw new IOException("Some strange error. Read less than expected.");
                    }

                    System.arraycopy(addition, 0, buffer, remainder, (pieceLength - remainder));
                } else {
                    byte[] addition = new byte[bIS.available()];

                    if (bIS.read(addition) != addition.length) {
                        throw new IOException("Some strange error. Read less than expected.");
                    }

                    System.arraycopy(addition, 0, buffer, remainder, addition.length);
                    remainder = remainder + addition.length;
                    continue;
                }
            }

            while (bIS.available() > pieceLength) {
                if (bIS.read(buffer) != pieceLength) {
                    throw new IOException("Some strange error. Read less than expected.");
                }

                byte[] hash = DigestUtils.sha1(buffer);
                pieces.append(new String(hash, Torrent.DEFAULT_TORRENT_ENCODING));
            }


            //TODO check if it is the last chunk
            if (bIS.available() > 0) {
                if (files.indexOf(file) == (files.size() - 1)) {
                    byte[] lastChunk = new byte[bIS.available()];

                    if (bIS.read(lastChunk) != lastChunk.length) {
                        throw new IOException("Some strange error. Read less than expected.");
                    }

                    byte[] hash = DigestUtils.sha1(lastChunk);
                    pieces.append(new String(hash, Torrent.DEFAULT_TORRENT_ENCODING));

                } else {
                    remainder = bIS.available();
                    if (bIS.read(buffer, 0, remainder) != remainder) {
                        throw new IOException("Some strange error. Read less than expected.");
                    }
                }

            }

        }

        return pieces.toString();
    }
*/

    public static String getPieces(Iterable<File> files, long offset, int pieceLength) throws IOException, ExecutionException, InterruptedException, FileNotFoundException, UnsupportedEncodingException {

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<byte[]>> piecesList = Lists.newArrayList();

        int bytesRemain = pieceLength;
        int counter = 0;

        byte[] buffer = new byte[pieceLength];

        BufferedInputStream bIS = null;

        try {
            for (File file : files) {
                FileInputStream fIS = new FileInputStream(file);
                bIS = new BufferedInputStream(fIS);

                if (counter == 0) {
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

                            HasherThread ht = new HasherThread(buffer);
                            piecesList.add(executor.submit(ht));
                        }

                        if (bIS.available() < bytesRemain) {
                            buffer = new byte[pieceLength];
                            int read = bIS.read(buffer);
                            bytesRemain = bytesRemain - read;
                        }
                    } else {

                        if (bIS.available() >= bytesRemain) {
                            addition = new byte[bytesRemain];
                            if (bIS.read(addition) != bytesRemain) {
                                throw new IOException("Some strange error. Read less than expected.");
                            }
                            System.arraycopy(addition, 0, buffer, pieceLength - bytesRemain, addition.length);

                            HasherThread ht = new HasherThread(buffer);
                            piecesList.add(executor.submit(ht));

                            bytesRemain = pieceLength;
                        } else {
                            addition = new byte[bIS.available()];
                            int read = bIS.read(buffer);
                            bytesRemain = bytesRemain - read;
                            System.arraycopy(addition, 0, buffer, pieceLength - bytesRemain, addition.length);
                        }
                    }

                }

                counter++;
            }

            StringBuilder sb = new StringBuilder(piecesList.size() * pieceLength);

            for (Future<byte[]> piece : piecesList) {
                sb.append(new String(piece.get(), Torrent.DEFAULT_TORRENT_ENCODING));
            }

            executor.shutdown();

            return sb.toString();
        } finally {
            if (bIS != null) {
                bIS.close();
            }
        }

    }
}
