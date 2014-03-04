package org.aotorrent.common.hash;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/**
 * User: dnapolov
 * Date: 1/28/14
 * Time: 4:18 PM
 */
public class HashMakerThread implements Callable<byte[]> {

    private final byte[] data;
    private final Semaphore semaphore;

    public HashMakerThread(byte[] data, Semaphore semaphore) {
        this.data = data;
        this.semaphore = semaphore;
    }

    @Override
    public byte[] call() throws Exception {
        try {
            return DigestUtils.sha1(data);
        } finally {
            semaphore.release();
        }
    }
}
