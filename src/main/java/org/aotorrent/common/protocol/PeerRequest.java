package org.aotorrent.common.protocol;

import com.google.common.collect.Maps;

import java.util.Map;


/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class PeerRequest {
    private static final int BYTE_MAX = 0xFF;
    private final RequestType requestType;

    public PeerRequest(byte[] request) {
        final int requestLength = request[0] & BYTE_MAX;
        int index = 1;

        int requestTypeNum = request[index] & BYTE_MAX;
        requestType = RequestType.from(requestTypeNum);
        index++;

        switch (requestType) {
            case HAVE:
                break;
            case BIT_FIELD:
                break;
            case REQUEST:
                break;
            case PIECE:
                break;
            case CANCEL:
                break;
            case PORT:
                break;
            default:
                break;
        }


    }
    //TODO

    enum RequestType {
        KEEP_ALIVE(-1), CHOKE(0), UNCHOKE(1), INTERESTED(2), NOT_INTERESTED(3), HAVE(4), BIT_FIELD(5), REQUEST(6), PIECE(7), CANCEL(8), PORT(9);

        final int requestCode;

        RequestType(int i) {
            requestCode = i;
        }

        // Mapping difficulty to difficulty id
        private static final Map<Integer, RequestType> _map = Maps.newHashMap();

        static {
            for (RequestType requestType : RequestType.values())
                _map.put(requestType.requestCode, requestType);
        }

        public static RequestType from(int requestCode) {
            return _map.get(requestCode);
        }
    }
}

