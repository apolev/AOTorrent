package org.aotorrent.common.protocol.peer;

import org.aotorrent.common.Torrent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * User: dnapolov
 * Date: 2/21/14
 * Time: 5:37 PM
 */
public class HandshakeRequest implements PeerRequest {
    public static final String DEFAULT_PROTOCOL_STRING = "BitTorrent protocol";
    public static final byte[] DEFAULT_RESERVED_BITS = {0, 0, 0, 0, 0, 0, 0, 0};
    private static final int BYTE_MAX = 0xFF;
    private final String protocolString;
    private final byte[] reserved;
    private final byte[] infoHash;
    private final byte[] peerId;

    public HandshakeRequest(byte[] handshake) throws UnsupportedEncodingException {
        int protocolStringLength = handshake[0] & BYTE_MAX;
        int index = 1;

        protocolString = new String(handshake, index, protocolStringLength, Torrent.DEFAULT_TORRENT_ENCODING);
        index += protocolStringLength;

        reserved = Arrays.copyOfRange(handshake, index, index + 8);
        index += 8;

        infoHash = Arrays.copyOfRange(handshake, index, Torrent.INFO_HASH_LENGTH + index);
        index += Torrent.INFO_HASH_LENGTH;

        peerId = Arrays.copyOfRange(handshake, index, Torrent.PEER_ID_LENGTH + index);
        index += Torrent.PEER_ID_LENGTH;
    }

    public HandshakeRequest(byte[] infoHash, byte[] peerId) {
        this(DEFAULT_PROTOCOL_STRING, DEFAULT_RESERVED_BITS, infoHash, peerId);
    }

    public HandshakeRequest(String protocolString, byte[] reserved, byte[] infoHash, byte[] peerId) {
        this.protocolString = protocolString;
        this.reserved = reserved;
        this.infoHash = infoHash;
        this.peerId = peerId;
    }

    @Override
    public byte[] toTransmit() throws IOException, UnsupportedEncodingException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(protocolString.length());
        os.write(protocolString.getBytes(Torrent.DEFAULT_TORRENT_ENCODING));
        os.write(reserved);
        os.write(infoHash);
        os.write(peerId);

        return os.toByteArray();
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public boolean isOk(byte[] infoHash) {
        return Arrays.equals(this.getInfoHash(), infoHash);
    }

    @Override
    public String toString() {
        return "HandshakeRequest{" +
                "protocolString='" + protocolString + '\'' +
                ", reserved=" + Arrays.toString(reserved) +
                ", infoHash=" + Arrays.toString(infoHash) +
                ", peerId=" + Arrays.toString(peerId) +
                '}';
    }
}
