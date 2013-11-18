package org.aotorrent.common.bencode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Project: bencode-lib
 * User:    dmitry
 * Date:    11/5/13
 */
public class Parser {

    private final InputStream is;

    public Parser(InputStream is) {
        this.is = is;
    }

    public List<Value> parse() throws IOException, InvalidBEncodingException {
        List<Value> result = new ArrayList<Value>();

        Value temporary;

        while ((temporary = getNext()) != null) {
            result.add(temporary);
        }

        return result;
    }

    public Map<String, Value> parseMap() throws IOException, InvalidBEncodingException {
        return decodeMap().getMap();
    }

    public Value parseOne() throws IOException, InvalidBEncodingException {
        return getNext();
    }

    private Value getNext() throws InvalidBEncodingException, IOException {
        int typeToken = getNextType();

        if ((typeToken == -1) || typeToken == 'e') {
            return null;
        }

        if (Character.isDigit(typeToken))
            return decodeString();
        else if (typeToken == 'i')
            return decodeNumber();
        else if (typeToken == 'l')
            return decodeList();
        else if (typeToken == 'd')
            return decodeMap();
        else
            throw new InvalidBEncodingException
                    ("Unknown type '" + typeToken + "'");
    }

    private Value decodeString() throws InvalidBEncodingException, IOException {

        int length = getStringLength();

        if (length <= 0) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            Integer oneByte = is.read();
            stringBuilder.append((char) oneByte.byteValue());
        }

        return new Value(stringBuilder.toString());
    }

    private Value decodeNumber() throws IOException, InvalidBEncodingException {
        int iLetter = is.read();

        if (iLetter != 'i') {
            throw new InvalidBEncodingException("Invalid byte sequence: " + (char) iLetter);
        }

        long number = 0;
        boolean negative = false;

        int got = is.read();

        // first character
        if (got == '-') {
            negative = true;
        } else if (Character.isDigit(got)) {
            number = Character.digit(got, 10);
        } else {
            throw new InvalidBEncodingException("Invalid byte sequence: " + (char) got);
        }

        // rest characters
        while (Character.isDigit(got = is.read())) {
            number = number * 10 + Character.digit(got, 10);
        }

        if (negative) {
            return new Value(number * (-1));
        }

        return new Value(number);
    }

    private Value decodeList() throws IOException, InvalidBEncodingException {
        int iLetter = is.read();

        if (iLetter != 'l') {
            throw new InvalidBEncodingException("Invalid byte sequence: " + (char) iLetter);
        }

        Value value;

        List<Value> result = new ArrayList<Value>();

        while ((value = getNext()) != null) {
            result.add(value);
        }

        iLetter = is.read();

        if (iLetter != 'e') {
            throw new InvalidBEncodingException("Invalid byte sequence: " + (char) iLetter);
        }

        return new Value(result);
    }

    private Value decodeMap() throws InvalidBEncodingException, IOException {
        int iLetter = is.read();

        if (iLetter != 'd') {
            throw new InvalidBEncodingException("Invalid byte sequence: " + (char) iLetter);
        }

        Map<String, Value> result = new HashMap<String, Value>();

        Value key;
        while ((key = decodeString()) != null) {
            Value value = getNext();
            result.put(key.getString(), value);
        }

        iLetter = is.read();

        if (iLetter != 'e') {
            throw new InvalidBEncodingException("Invalid byte sequence: " + (char) iLetter);
        }

        return new Value(result);

    }

    private int getNextType() throws IOException {
        is.mark(2);
        int type = is.read();
        is.reset();
        return type;
    }

    private int getStringLength() throws IOException, InvalidBEncodingException {
        is.mark(2);
        int character = is.read();
        if (!Character.isDigit(character)) {
            is.reset();
            return -1;
        }
        int length = Character.digit(character, 10);

        if (length < 0) {
            throw new InvalidBEncodingException("Length expected but " + length + "found");
        }

        int next;

        while (((next = is.read()) != ':') && (next != -1)) {
            length = length * 10 + Character.digit(next, 10);
        }

        if (next == -1) {
            throw new InvalidBEncodingException("Unexpected end of stream.");
        }

        return length;
    }

    public static Map<String, Value> parse(InputStream is) throws IOException, InvalidBEncodingException {
        Parser parser = new Parser(is);
        return parser.parseMap();
    }

    public static Value parseOne(InputStream is) throws IOException, InvalidBEncodingException {
        Parser parser = new Parser(is);
        return parser.parseOne();
    }
}
