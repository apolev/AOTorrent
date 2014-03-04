package org.aotorrent.common.protocol.peer;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * User: dnapolov
 * Date: 2/25/14
 * Time: 6:36 PM
 */
public enum RequestType {
    KEEP_ALIVE(-1), CHOKE(0), UNCHOKE(1), INTERESTED(2), NOT_INTERESTED(3), HAVE(4), BIT_FIELD(5), REQUEST(6), PIECE(7), CANCEL(8), PORT(9);

    private final int requestCode;

    RequestType(int i) {
        requestCode = i;
    }

    public int getRequestCode() {
        return requestCode;
    }

    // Mapping difficulty to difficulty id
    private static final Map<Integer, RequestType> MAP = Maps.newHashMap();

    static {
        for (RequestType requestType : RequestType.values()) {
            MAP.put(requestType.getRequestCode(), requestType);
        }
    }

    public static RequestType from(int requestCode) {
        return MAP.get(requestCode);
    }
}
