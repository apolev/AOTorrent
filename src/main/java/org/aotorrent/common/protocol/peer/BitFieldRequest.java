package org.aotorrent.common.protocol.peer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * User: dnapolov
 * Date: 2/25/14
 * Time: 6:38 PM
 */
public class BitFieldRequest {
    private final BitSet bitField;

    public BitFieldRequest(BitSet bitField) {
        this.bitField = bitField;
    }

    public BitFieldRequest(byte[] message, int bitFieldSize) {
        BitSet bitFieldBuffer = fromByteArray(message);
        bitField = new BitSet(bitFieldSize);
        bitField.or(bitFieldBuffer);
    }

    public byte[] toTransmit() throws IOException {
        int messageSize = (bitField.length() + 7) / 8 + 1;
        final ByteBuffer byteBuffer = ByteBuffer.allocate(messageSize + 4).putInt(messageSize);

        byteBuffer.put((byte) RequestType.BIT_FIELD.requestCode);

        byte[] buffer = new byte[messageSize];
        for (int i = 0; i < bitField.length(); i++) {
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

}
