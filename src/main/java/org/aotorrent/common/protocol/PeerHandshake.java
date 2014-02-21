package org.aotorrent.common.protocol;

import org.aotorrent.common.Torrent;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * User: dnapolov
 * Date: 2/21/14
 * Time: 5:37 PM
 */
public class PeerHandshake {
    private static final int BYTE_MAX = 0xFF;
    private final String protocolString;
    private final boolean[] reserved;
    private final byte[] infoHash;
    private final byte[] peerId;

    public PeerHandshake(byte[] handshake) throws UnsupportedEncodingException {
        int protocolStringLength = handshake[0] & BYTE_MAX;
        int index = 1;

        protocolString = new String(handshake, index, protocolStringLength, Torrent.DEFAULT_TORRENT_ENCODING);
        index += protocolStringLength;

        reserved = bits(handshake[index]);
        index++;

        infoHash = Arrays.copyOfRange(handshake, index, Torrent.INFO_HASH_LENGTH + index);
        index += Torrent.INFO_HASH_LENGTH;

        peerId = Arrays.copyOfRange(handshake, index, Torrent.PEER_ID_LENGTH + index);
        index += Torrent.PEER_ID_LENGTH;
    }

    public PeerHandshake(String protocolString, boolean[] reserved, byte[] infoHash, byte[] peerId) {
        this.protocolString = protocolString;
        this.reserved = reserved;
        this.infoHash = infoHash;
        this.peerId = peerId;
    }

    public static int getByteMax() {
        return BYTE_MAX;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public byte[] getPeerId() {
        return peerId;
    }

    public String getProtocolString() {
        return protocolString;
    }

    public boolean[] getReserved() {
        return reserved;
    }

    private static boolean[] bits(final byte b) {
        return new boolean[]{
                (b & 1) != 0,
                (b & 2) != 0,
                (b & 4) != 0,
                (b & 8) != 0,
                (b & 0x10) != 0,
                (b & 0x20) != 0,
                (b & 0x40) != 0,
                (b & 0x80) != 0
        };
    }
}
