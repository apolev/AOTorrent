package org.aotorrent.common.connection;

import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import org.aotorrent.client.TorrentEngine;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Random;
import java.util.Set;

public class UDPTrackerConnection extends AbstractTrackerConnection {

    private static final int TRACKER_CONNECTION_TIMEOUT = 10000;
    private static final int DEFAULT_INPUT_BUFFER_LENGTH = 1500;
    private static final int MIN_ANNOUNCE_REPLY_SIZE = 20;
    private static final long DEFAULT_CONNECTION_ID = 0x41727101980l;
    private static final int MIN_CONNECT_REPLY_SIZE = 16;
    private static final int MAX_PORT_VALUE = 0xFFFF;

    public UDPTrackerConnection(TorrentEngine torrentEngine, String url, byte[] infoHash, byte[] peerId, InetAddress ip, int port) {
        super(torrentEngine, url, infoHash, peerId, ip, port);
    }

    @Override
    protected void obtainPeers() {

        final Random random = new Random(System.currentTimeMillis());

        try (DatagramSocket clientSocket = new DatagramSocket()) {
            //TODO proper URL parsing
            int hostPortDelimiter = getUrl().indexOf(':', 6);

            String host = getUrl().substring(6, hostPortDelimiter);
            int port = Integer.parseInt(getUrl().substring(hostPortDelimiter + 1, getUrl().indexOf('/', hostPortDelimiter)));
            InetAddress ipAddress = InetAddress.getByName(host);

            int transactionId = random.nextInt();
            final byte[] connectRequest = connectRequest(transactionId);
            DatagramPacket sendConnection = new DatagramPacket(connectRequest, connectRequest.length, ipAddress, port);
            clientSocket.send(sendConnection);

            final long connectionId = connectReply(transactionId, clientSocket);

            transactionId = random.nextInt();

            final byte[] announceRequest = announceRequest(connectionId, transactionId);

            DatagramPacket announceConnection = new DatagramPacket(announceRequest, announceRequest.length, ipAddress, port);
            clientSocket.send(announceConnection);

            setPeers(announceReply(connectionId, transactionId, clientSocket));


        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] connectRequest(final int transactionId) {
        final long connectionId = DEFAULT_CONNECTION_ID;
        final int action = 0;

        ByteBuffer bb = ByteBuffer.allocate(Ints.BYTES * 2 + Longs.BYTES);
        bb.putLong(connectionId);
        bb.putInt(action);
        bb.putInt(transactionId);

        return bb.array();
    }

    private long connectReply(int transactionId, DatagramSocket clientSocket) throws IOException {
        final long startTime = System.currentTimeMillis();

        int receivedTransactionId = 0;
        long receivedConnectionId = 0;

        while (receivedTransactionId != transactionId && receivedConnectionId == 0) {
            if (System.currentTimeMillis() - startTime > TRACKER_CONNECTION_TIMEOUT) {
                throw new IOException("Timeout");
            }
            byte[] receiveData = new byte[MIN_CONNECT_REPLY_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);

            if (receivePacket.getLength() >= MIN_CONNECT_REPLY_SIZE) {
                ByteBuffer bb = ByteBuffer.wrap(receivePacket.getData());
                bb.getInt();
                receivedTransactionId = bb.getInt();
                receivedConnectionId = bb.getLong();
            }
        }

        return receivedConnectionId;
    }

    private byte[] announceRequest(long connectionId, int transactionId) {
        final int action = 1;

        ByteBuffer bb = ByteBuffer.allocate(Ints.BYTES * 6 + Longs.BYTES * 4 + Shorts.BYTES + getInfoHash().length + getPeerId().length);
        bb.putLong(connectionId);
        bb.putInt(action);
        bb.putInt(transactionId);
        bb.put(getInfoHash());
        bb.put(getPeerId());
        bb.putLong(getDownloaded());
        bb.putLong(getLeft());
        bb.putLong(getUploaded());
        bb.putInt(2); //event = 0: none; 1: completed; 2: started; 3: stopped
        bb.putInt(0); //IPaddress 0 - default
        bb.putInt(0); //key - WTF
        bb.putInt(NUM_WANT); //numwant - default
        bb.putShort((short) getPort());

        return bb.array();
    }

    private Set<InetSocketAddress> announceReply(long connectionId, int transactionId, DatagramSocket clientSocket) throws IOException {
        final long startTime = System.currentTimeMillis();

        int receivedTransactionId;

        while (true) {
            if (System.currentTimeMillis() - startTime > TRACKER_CONNECTION_TIMEOUT) {
                throw new IOException("Timeout");
            }

            byte[] receiveData = new byte[DEFAULT_INPUT_BUFFER_LENGTH];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);

            if (receivePacket.getLength() >= MIN_ANNOUNCE_REPLY_SIZE) {
                ByteBuffer bb = ByteBuffer.wrap(receivePacket.getData());
                int action = bb.getInt();
                if (action != 1) {
                    continue;
                }
                receivedTransactionId = bb.getInt();
                if (receivedTransactionId != transactionId) {
                    continue;
                }
                int interval = bb.getInt();
                setNextRequest(new Date(System.currentTimeMillis() + (interval * 1000)));
                setSeeders(bb.getInt());
                setLeechers(bb.getInt());

                Set<InetSocketAddress> peers = Sets.newHashSet();

                for (int i = 0; i < ((receivePacket.getLength() - MIN_ANNOUNCE_REPLY_SIZE) / 6); i++) {
                    int ip = bb.getInt();
                    short port = bb.getShort();
                    InetAddress inetAddress = InetAddress.getByAddress(Ints.toByteArray(ip));
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, (MAX_PORT_VALUE & port));
                    peers.add(inetSocketAddress);
                }

                return peers;
            }
        }
    }

}
