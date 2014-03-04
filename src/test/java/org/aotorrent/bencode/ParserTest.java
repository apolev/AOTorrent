package org.aotorrent.bencode;

import com.google.common.collect.Maps;
import org.aotorrent.common.bencode.BEncodeParser;
import org.aotorrent.common.bencode.BEncodeValue;
import org.aotorrent.common.bencode.InvalidBEncodingException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * User: dnapolov
 * Date: 11/18/13
 * Time: 7:20 PM
 */
public class ParserTest {

    @Test
    public void numberParseTest() throws IOException, InvalidBEncodingException {
        String input = "i100500e";
        BEncodeValue value = BEncodeParser.parseOne(new ByteArrayInputStream(input.getBytes()));
        org.junit.Assert.assertEquals(100500, value.getLong());
    }

    @Test
    public void stringParseTest() throws IOException, InvalidBEncodingException {
        String input = "10:yoyoyoyoyo";
        BEncodeValue value = BEncodeParser.parseOne(new ByteArrayInputStream(input.getBytes()));
        org.junit.Assert.assertEquals("yoyoyoyoyo", value.getString());
    }

    @Test
    public void listParseTest() throws IOException, InvalidBEncodingException {
        String input = "l10:yoyoyoyoyoi100500ee";
        BEncodeValue value = BEncodeParser.parseOne(new ByteArrayInputStream(input.getBytes()));
        org.junit.Assert.assertEquals(value, Arrays.asList(new BEncodeValue("yoyoyoyoyo"), new BEncodeValue(100500)));
    }

    @Test
    public void mapParseTest() throws IOException, InvalidBEncodingException {
        String input = "d5:firstl10:yoyoyoyoyoi100500eee";
        BEncodeValue value = BEncodeParser.parseOne(new ByteArrayInputStream(input.getBytes()));
        Map<String, BEncodeValue> valueMap = Maps.newHashMap();
        valueMap.put("first", new BEncodeValue(Arrays.asList(new BEncodeValue("yoyoyoyoyo"), new BEncodeValue(100500))));
        org.junit.Assert.assertEquals(value, valueMap);
    }
}
