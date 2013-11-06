package org.aotorrent.common.hash;

import org.aotorrent.common.Torrent;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * User: dnapolov
 * Date: 11/6/13
 * Time: 4:29 PM
 */
public class Hasher {

    public static String getPieces(List<String> fileNames, int pieceLength) throws IOException {
        StringBuilder pieces = new StringBuilder();
        byte[] buffer = new byte[pieceLength];

        int remainder = 0;

        for (String fileName : fileNames) {
            FileInputStream fIS = new FileInputStream(fileName);
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
                if (fileNames.indexOf(fileName) == (fileNames.size() - 1)) {
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
}
