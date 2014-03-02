package org.aotorrent.common.protocol.peer;

import java.io.IOException;

/**
 * Created by dmitry on 3/2/14.
 */
public interface PeerRequest {
    byte[] toTransmit() throws IOException;
}
