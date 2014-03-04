package org.aotorrent.common.connection;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.aotorrent.client.TorrentEngine;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Created by dmitry on 3/5/14.
 */
public class UDPTrackerConnection extends AbstractTrackerConnection {
    public UDPTrackerConnection(URL url, byte[] peerId, InetAddress ip, byte[] infoHash, int port, TorrentEngine torrentEngine) {
        super(url, peerId, ip, infoHash, port, torrentEngine);
    }

    @Override
    protected void getPeers() {

        final Random random = new Random(System.currentTimeMillis());

        try {
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress IPAddress = InetAddress.getByName(url.getHost());

            int transactionId = random.nextInt();
            final byte[] connectRequest = connectRequest(transactionId);
            DatagramPacket sendConnection = new DatagramPacket(connectRequest, connectRequest.length, IPAddress, 9876);
            clientSocket.send(sendConnection);

            final long connection_id = connectReply(transactionId, clientSocket);


        } catch (IOException e) {
            e.printStackTrace();
        }
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

            ByteBuffer bb = ByteBuffer.wrap(receiveData);
            int zero = bb.getInt();
            recievedTransactionId = bb.getInt();
            receivedConnectionId = bb.getLong();
        }

        return receivedConnectionId;
    }

    byte[] connectRequest(final int transactionId) {
        final long connectionId = 0x41727101980l;
        final int action = 0;

        ByteBuffer bb = ByteBuffer.allocate(Ints.BYTES * 2 + Longs.BYTES);
        bb.putLong(connectionId);
        bb.putInt(action);
        bb.putInt(transactionId);

        return bb.array();
    }
}
