package org.aotorrent.common.hash;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/**
 * User: dnapolov
 * Date: 1/28/14
 * Time: 4:18 PM
 */
public class HasherThread implements Callable<byte[]> {

    private final byte[] data;
    private final Semaphore semaphore;

    public HasherThread(byte[] data, Semaphore semaphore) {
        this.data = data;
        this.semaphore = semaphore;
    }

    @Override
    public byte[] call() throws Exception {
        try {
            final byte[] bytes = DigestUtils.sha1(data);
            return bytes;
        } finally {
            semaphore.release();
        }
    }
}
