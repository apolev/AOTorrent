package org.aotorrent.client;

import org.aotorrent.common.Piece;
import org.aotorrent.common.Torrent;
import org.aotorrent.common.connection.PeerConnection;
import org.aotorrent.common.connection.TrackerConnection;

import java.util.List;

/**
 * Project: AOTorrent
 * User:    dmitry
 * Date:    11/8/13
 */
public class TorrentEngine {

    private List<TrackerConnection> trackerConnections;
    private List<PeerConnection> peerConnections;
    private List<Piece> pieces;

    private final Torrent torrent;

    public TorrentEngine(Torrent torrent) {
        this.torrent = torrent; //TODO
    }


}
