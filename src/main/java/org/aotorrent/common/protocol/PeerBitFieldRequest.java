package org.aotorrent.common.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * User: dnapolov
 * Date: 2/25/14
 * Time: 6:38 PM
 */
public class PeerBitFieldRequest {
    private BitSet bitField;

    public PeerBitFieldRequest(BitSet bitField) {
        this.bitField = bitField;
    }

    public BitSet getBitField() {
        return bitField;
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
}
