package org.aotorrent.common.bencode;

/**
 * Project: bencode-lib
 * User:    dmitry
 * Date:    11/5/13
 */
public class InvalidBEncodingException extends Exception {
    public InvalidBEncodingException(String e) {
        super(e);
    }
}
