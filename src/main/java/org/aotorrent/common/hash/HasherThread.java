package org.aotorrent.common.hash;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.concurrent.Callable;

/**
 * User: dnapolov
 * Date: 1/28/14
 * Time: 4:18 PM
 */
public class HasherThread implements Callable<byte[]> {

    private byte[] data;

    public HasherThread(byte[] data) {
        this.data = data;
    }

    @Override
    public byte[] call() throws Exception {
        return DigestUtils.sha1(data);
    }
}
