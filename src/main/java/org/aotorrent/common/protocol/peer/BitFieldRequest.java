package org.aotorrent.common.protocol.peer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * User: dnapolov
 * Date: 2/25/14
 * Time: 6:38 PM
 */
public class BitFieldRequest implements PeerRequest {
    private final BitSet bitField;
    private final int size;

    public BitFieldRequest(BitSet bitField, int size) {
        this.bitField = bitField;
        this.size = size;
    }

    public BitFieldRequest(byte[] message, int bitFieldSize) {
        BitSet bitFieldBuffer = fromByteArray(message);
        bitField = new BitSet(bitFieldSize);
        bitField.or(bitFieldBuffer);
        size = message.length * 8;
    }

    @Override
    public byte[] toTransmit() throws IOException {
        int messageSize = (size + 7) / 8 + 1;
        final ByteBuffer byteBuffer = ByteBuffer.allocate(4 + messageSize).putInt(messageSize);

        byteBuffer.put((byte) RequestType.BIT_FIELD.getRequestCode());

        byte[] buffer = new byte[messageSize - 1];
        for (int i = 0; i < size; i++) {
            if (bitField.get(i)) {
                buffer[buffer.length - i / 8 - 1] |= 1 << (i % 8);
            }
        }

        byteBuffer.put(buffer);
        return byteBuffer.array();
    }

    public BitSet getBitField() {
        return bitField;
    }

    // for java 1.6 compatibility
    public static BitSet fromByteArray(byte[] bytes) {
        BitSet bits = new BitSet();
        for (int i = 0; i < bytes.length * 8; i++) {
            if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
                bits.set(i);
            }
        }
        return bits;
    }

    @Override
    public String toString() {
        return "BitFieldRequest{" +
                "bitField=" + bitField +
                '}';
    }
}
