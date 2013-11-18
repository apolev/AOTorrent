package org.aotorrent.bencode;

import com.google.common.collect.Maps;
import org.aotorrent.common.bencode.InvalidBEncodingException;
import org.aotorrent.common.bencode.Value;
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
        Value value = new Value(testValue);

        org.junit.Assert.assertEquals(value, testValue);
    }

    @Test
    public void testStringValue() throws InvalidBEncodingException {
        String stringValue = "Test string";
        Value value = new Value(stringValue);

        org.junit.Assert.assertEquals(value, stringValue);
    }

    @Test
    public void testListValue() throws InvalidBEncodingException {
        List<Value> listValue = Arrays.asList(new Value(1), new Value(2), new Value("String"));
        Value value = new Value(listValue);

        org.junit.Assert.assertEquals(value, listValue);
    }

    @Test
    public void testMapValue() throws InvalidBEncodingException {
        Map<String, Value> mapValue = Maps.newHashMap();
        List<Value> listValue = Arrays.asList(new Value(1), new Value(2), new Value("String"));

        mapValue.put("1", new Value(1));
        mapValue.put("2", new Value("2"));
        mapValue.put("3", new Value(listValue));

        Value value = new Value(mapValue);

        org.junit.Assert.assertEquals(value, mapValue);
    }

    @Test
    public void testNullValue() {
        Value value = new Value(null);

        org.junit.Assert.assertNull(value.getValue());
        org.junit.Assert.assertNull(value.getValueClass());
    }

    @Test
    public void testInvalidType() throws InvalidBEncodingException {
        Value value = new Value("String value");

        exception.expect(InvalidBEncodingException.class);

        Long longValue = value.getLong();
    }
}
