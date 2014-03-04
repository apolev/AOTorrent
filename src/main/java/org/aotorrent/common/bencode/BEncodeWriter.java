package org.aotorrent.common.bencode;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Project: bencode-lib
 * User:    dmitry
 * Date:    11/5/13
 */
public class BEncodeWriter {
    private final OutputStream os;

    public BEncodeWriter(OutputStream os) {
        this.os = os;
    }

    public void write(Iterable<BEncodeValue> list) throws IOException, InvalidBEncodingException, UnsupportedEncodingException {
        for (BEncodeValue row : list) {
            writeObject(row);
        }
    }

    public void write(Map<String, BEncodeValue> map) throws IOException, InvalidBEncodingException, UnsupportedEncodingException {
        writeMap(map);
    }

    private void writeObject(BEncodeValue object) throws IOException, InvalidBEncodingException, UnsupportedEncodingException {
        String valueClass = object.getValueClass();
        if ("String".equals(valueClass)) {
            writeString(object.getString());
        } else if ("Number".equals(valueClass)) {
            writeNumber(object.getLong());
        } else if ("List".equals(valueClass)) {
            writeList(object.getList());
        } else if ("Map".equals(valueClass)) { //TODO proper class type handling
            writeMap(object.getMap());
        } else {
            throw new InvalidBEncodingException("something went wrong");
        }
    }

    private void writeString(String string) throws IOException, UnsupportedEncodingException {
        String length = Integer.toString(string.length());
        os.write(length.getBytes("ISO-8859-1"));
        os.write(':');
        os.write(string.getBytes("ISO-8859-1"));
    }

    private void writeNumber(Long number) throws IOException, UnsupportedEncodingException {
        os.write('i');
        os.write(number.toString().getBytes("UTF-8"));
        os.write('e');
    }

    private void writeList(Iterable<BEncodeValue> list) throws IOException, InvalidBEncodingException, UnsupportedEncodingException {
        os.write('l');
        write(list);
        os.write('e');
    }

    private void writeMap(Map<String, BEncodeValue> map) throws IOException, InvalidBEncodingException, UnsupportedEncodingException {
        os.write('d');
        for (Map.Entry<String, BEncodeValue> entry : map.entrySet()) {
            writeString(entry.getKey());
            writeObject(entry.getValue());
        }
        os.write('e');
    }

    public static void writeOut(OutputStream os, Map<String, BEncodeValue> map) throws IOException, InvalidBEncodingException, UnsupportedEncodingException {
        BEncodeWriter writer = new BEncodeWriter(os);
        writer.write(map);
    }
}
