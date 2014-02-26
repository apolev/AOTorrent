package org.aotorrent.common.protocol.peer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by dmitry on 2/25/14.
 */
public class ChokeRequest {
    public byte[] toTransmit() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4 + 1);

        bb.putInt(1);
        bb.put((byte) RequestType.CHOKE.requestCode);

        return bb.array();
    }
}
