package org.aotorrent.common.bencode;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Project: bencode-lib
 * User:    dmitry
 * Date:    11/5/13
 */
public class Writer {
    private final OutputStream os;

    public Writer(OutputStream os) {
        this.os = os;
    }

    public void write(List<Value> list) throws IOException, InvalidBEncodingException {
        for (Value row : list) {
            writeObject(row);
        }
    }

    public void write(Map<String, Value> map) throws IOException, InvalidBEncodingException {
        writeMap(map);
    }

    private void writeObject(Value object) throws IOException, InvalidBEncodingException {
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
            System.out.println("something went wrong");
        }
    }

    private void writeString(String string) throws IOException {
        String length = Integer.toString(string.length());
        os.write(length.getBytes("ISO-8859-1"));
        os.write(':');
        os.write(string.getBytes("ISO-8859-1"));
    }

    private void writeNumber(Long number) throws IOException {
        os.write('i');
        os.write(number.toString().getBytes("UTF-8"));
        os.write('e');
    }

    private void writeList(List<Value> list) throws IOException, InvalidBEncodingException {
        os.write('l');
        write(list);
        os.write('e');
    }

    private void writeMap(Map<String, Value> map) throws IOException, InvalidBEncodingException {
        os.write('d');
        for (Map.Entry<String, Value> entry : map.entrySet()) {
            writeString(entry.getKey());
            writeObject(entry.getValue());
        }
        os.write('e');
    }

    public static void writeOut(OutputStream os, Map<String, Value> map) throws IOException, InvalidBEncodingException {
        Writer writer = new Writer(os);
        writer.write(map);
    }
}
