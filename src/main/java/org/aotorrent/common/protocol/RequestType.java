package org.aotorrent.common.protocol;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * User: dnapolov
 * Date: 2/25/14
 * Time: 6:36 PM
 */
public enum RequestType {
    KEEP_ALIVE(-1), CHOKE(0), UNCHOKE(1), INTERESTED(2), NOT_INTERESTED(3), HAVE(4), BIT_FIELD(5), REQUEST(6), PIECE(7), CANCEL(8), PORT(9);

    final int requestCode;

    RequestType(int i) {
        requestCode = i;
    }

    // Mapping difficulty to difficulty id
    private static final Map<Integer, RequestType> map = Maps.newHashMap();

    static {
        for (RequestType requestType : RequestType.values())
            map.put(requestType.requestCode, requestType);
    }

    public static RequestType from(int requestCode) {
        return map.get(requestCode);
    }
}
