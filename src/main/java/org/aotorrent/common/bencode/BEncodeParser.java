package org.aotorrent.common.bencode;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Project: bencode-lib
 * User:    dmitry
 * Date:    11/5/13
 */
public class BEncodeParser {

    private final InputStream is;

    public BEncodeParser(InputStream is) {
        this.is = is;
    }

    public List<BEncodeValue> parse() throws IOException, InvalidBEncodingException {
        List<BEncodeValue> result = Lists.newArrayList();

        BEncodeValue temporary = getNext();

        while (temporary != null) {
            result.add(temporary);
            temporary = getNext();
        }

        return result;
    }

    public Map<String, BEncodeValue> parseMap() throws IOException, InvalidBEncodingException {
        return decodeMap().getMap();
    }

    @Nullable
    public BEncodeValue parseOne() throws IOException, InvalidBEncodingException {
        return getNext();
    }

    @Nullable
    private BEncodeValue getNext() throws InvalidBEncodingException, IOException {
        int typeToken = getNextType();

        if ((typeToken == -1) || typeToken == 'e') {
            return null;
        }

        if (Character.isDigit(typeToken)) {
            return decodeString();
        } else if (typeToken == 'i') {
            return decodeNumber();
        } else if (typeToken == 'l') {
            return decodeList();
        } else if (typeToken == 'd') {
            return decodeMap();
        } else {
            throw new InvalidBEncodingException("Unknown type '" + typeToken + '\'');
        }
    }

    @Nullable
    private BEncodeValue decodeString() throws InvalidBEncodingException, IOException {

        int length = getStringLength();

        if (length <= 0) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            Integer oneByte = is.read();
            stringBuilder.append(Character.toChars(oneByte)[0]);
        }

        return new BEncodeValue(stringBuilder.toString());
    }

    private BEncodeValue decodeNumber() throws IOException, InvalidBEncodingException {
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
        got = is.read();
        while (Character.isDigit(got)) {
            number = number * 10 + Character.digit(got, 10);
            got = is.read();
        }

        if (negative) {
            return new BEncodeValue(number * (-1));
        }

        return new BEncodeValue(number);
    }

    private BEncodeValue decodeList() throws IOException, InvalidBEncodingException {
        int iLetter = is.read();

        if (iLetter != 'l') {
            throw new InvalidBEncodingException("Invalid byte sequence: " + (char) iLetter);
        }

        List<BEncodeValue> result = Lists.newArrayList();

        BEncodeValue value = getNext();

        while (value != null) {
            result.add(value);
            value = getNext();
        }

        iLetter = is.read();

        if (iLetter != 'e') {
            throw new InvalidBEncodingException("Invalid byte sequence: " + (char) iLetter);
        }

        return new BEncodeValue(result);
    }

    private BEncodeValue decodeMap() throws InvalidBEncodingException, IOException {
        int iLetter = is.read();

        if (iLetter != 'd') {
            throw new InvalidBEncodingException("Invalid byte sequence: " + (char) iLetter);
        }

        Map<String, BEncodeValue> result = Maps.newLinkedHashMap();

        BEncodeValue key = decodeString();
        while (key != null) {
            BEncodeValue value = getNext();
            result.put(key.getString(), value);
            key = decodeString();
        }

        iLetter = is.read();

        if (iLetter != 'e') {
            throw new InvalidBEncodingException("Invalid byte sequence: " + (char) iLetter);
        }

        return new BEncodeValue(result);

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

        int next = is.read();

        while ((next != ':') && (next != -1)) {
            length = length * 10 + Character.digit(next, 10);
            next = is.read();
        }

        if (next == -1) {
            throw new InvalidBEncodingException("Unexpected end of stream.");
        }

        return length;
    }

    public static Map<String, BEncodeValue> parse(InputStream is) throws IOException, InvalidBEncodingException {
        BEncodeParser parser = new BEncodeParser(is);
        return parser.parseMap();
    }

    @Nullable
    public static BEncodeValue parseOne(InputStream is) throws IOException, InvalidBEncodingException {
        BEncodeParser parser = new BEncodeParser(is);
        return parser.parseOne();
    }
}
