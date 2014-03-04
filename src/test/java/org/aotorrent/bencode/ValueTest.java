package org.aotorrent.bencode;

import com.google.common.collect.Maps;
import org.aotorrent.common.bencode.BEncodeValue;
import org.aotorrent.common.bencode.InvalidBEncodingException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * User: dnapolov
 * Date: 11/18/13
 * Time: 5:13 PM
 */
public class ValueTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void testLongValue() throws InvalidBEncodingException {
        long testValue = Long.MAX_VALUE;
        BEncodeValue value = new BEncodeValue(testValue);

        org.junit.Assert.assertEquals(value, testValue);
    }

    @Test
    public void testStringValue() throws InvalidBEncodingException {
        String stringValue = "Test string";
        BEncodeValue value = new BEncodeValue(stringValue);

        org.junit.Assert.assertEquals(value, stringValue);
    }

    @Test
    public void testListValue() throws InvalidBEncodingException {
        List<BEncodeValue> listValue = Arrays.asList(new BEncodeValue(1), new BEncodeValue(2), new BEncodeValue("String"));
        BEncodeValue value = new BEncodeValue(listValue);

        org.junit.Assert.assertEquals(value, listValue);
    }

    @Test
    public void testMapValue() throws InvalidBEncodingException {
        Map<String, BEncodeValue> mapValue = Maps.newHashMap();
        List<BEncodeValue> listValue = Arrays.asList(new BEncodeValue(1), new BEncodeValue(2), new BEncodeValue("String"));

        mapValue.put("1", new BEncodeValue(1));
        mapValue.put("2", new BEncodeValue("2"));
        mapValue.put("3", new BEncodeValue(listValue));

        BEncodeValue value = new BEncodeValue(mapValue);

        org.junit.Assert.assertEquals(value, mapValue);
    }

    @Test
    public void testNullValue() {
        BEncodeValue value = new BEncodeValue(null);

        org.junit.Assert.assertNull(value.getValue());
        org.junit.Assert.assertNull(value.getValueClass());
    }

    @Test
    public void testInvalidType() throws InvalidBEncodingException {
        BEncodeValue value = new BEncodeValue("String value");

        exception.expect(InvalidBEncodingException.class);

        Long longValue = value.getLong();
    }
}
