package org.aotorrent.common.connection;

import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import org.aotorrent.client.TorrentEngine;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Random;
import java.util.Set;

public class UDPTrackerConnection extends AbstractTrackerConnection {
    public UDPTrackerConnection(TorrentEngine torrentEngine, String url, byte[] infoHash, byte[] peerId, InetAddress ip, int port) {
        super(torrentEngine, url, infoHash, peerId, ip, port);
    }

    @Override
    protected void getPeers() {

        final Random random = new Random(System.currentTimeMillis());

        try {
            DatagramSocket clientSocket = new DatagramSocket();

            int hostportDelimiter = url.indexOf(':', 6);

            String host = url.substring(6, hostportDelimiter);
            int port = Integer.parseInt(url.substring(hostportDelimiter + 1, url.indexOf('/', hostportDelimiter)));
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

            announceReply(connectionId, transactionId, clientSocket);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] connectRequest(final int transactionId) {
        final long connectionId = 0x41727101980l;
        final int action = 0;

        ByteBuffer bb = ByteBuffer.allocate(Ints.BYTES * 2 + Longs.BYTES);
        bb.putLong(connectionId);
        bb.putInt(action);
        bb.putInt(transactionId);

        return bb.array();
    }

    private long connectReply(int transactionId, DatagramSocket clientSocket) throws IOException {
        final long startTime = System.currentTimeMillis();

        int recievedTransactionId = 0;
        long receivedConnectionId = 0;

        while (recievedTransactionId != transactionId && receivedConnectionId == 0) {
            if (System.currentTimeMillis() - startTime > 10000) {
                throw new IOException("Timeout");
            }
            byte[] receiveData = new byte[16];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);

            if (receivePacket.getLength() >= 16) {
                ByteBuffer bb = ByteBuffer.wrap(receivePacket.getData());
                int zero = bb.getInt();
                recievedTransactionId = bb.getInt();
                receivedConnectionId = bb.getLong();
            }
        }

        return receivedConnectionId;
    }

    private byte[] announceRequest(long connectionId, int transactionId) {
        final int action = 1;

        ByteBuffer bb = ByteBuffer.allocate(Ints.BYTES * 6 + Longs.BYTES * 4 + Shorts.BYTES + infoHash.length + peerId.length);
        bb.putLong(connectionId);
        bb.putInt(action);
        bb.putInt(transactionId);
        bb.put(infoHash);
        bb.put(peerId);
        bb.putLong(downloaded);
        bb.putLong(left);
        bb.putLong(uploaded);
        bb.putInt(2); //event = 0: none; 1: completed; 2: started; 3: stopped
        bb.putInt(0); //IPaddress 0 - default
        bb.putInt(0); //key - WTF
        bb.putInt(50); //numwant - default
        bb.putShort((short) port);

        return bb.array();
    }

    private void announceReply(long connectionId, int transactionId, DatagramSocket clientSocket) throws IOException {
        final long startTime = System.currentTimeMillis();

        int recievedTransactionId = 0;
        long receivedConnectionId = 0;

        while (true) {
            if (System.currentTimeMillis() - startTime > 10000) {
                throw new IOException("Timeout");
            }

            byte[] receiveData = new byte[1500];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);

            if (receivePacket.getLength() >= 20) {
                ByteBuffer bb = ByteBuffer.wrap(receivePacket.getData());
                int action = bb.getInt();
                if (action != 1) {
                    continue;
                }
                recievedTransactionId = bb.getInt();
                if (recievedTransactionId != transactionId) {
                    continue;
                }
                int interval = bb.getInt();
                nextRequest = new Date(System.currentTimeMillis() + (interval * 1000));
                seeders = bb.getInt();
                leechers = bb.getInt();

                Set<InetSocketAddress> peers = Sets.newHashSet();

                for (int i = 0; i < ((receivePacket.getLength() - 20) / 6); i++) {
                    int ip = bb.getInt();
                    short port = bb.getShort();
                    InetAddress inetAddress = InetAddress.getByAddress(Ints.toByteArray(ip));
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, (0xFFFF & port));
                    peers.add(inetSocketAddress);
                }

                this.peers = peers;
                LOGGER.debug("Received peers :" + peers);
                return;
            }
        }
    }

}
