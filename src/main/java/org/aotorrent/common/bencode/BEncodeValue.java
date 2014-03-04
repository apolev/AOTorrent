package org.aotorrent.common.bencode;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Project: bencode-lib
 * User:    dmitry
 * Date:    11/6/13
 */
public class BEncodeValue {

    private Object value;

    public BEncodeValue(Object value) {
        if (value instanceof Integer) {
            this.value = ((Integer) value).longValue();
        } else {
            this.value = value;
        }
    }

    @Nullable
    public Object getValue() {
        return value;
    }

    @Nullable
    public String getValueClass() {

        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return "Number";
        }
        if (value instanceof List) {
            return "List";
        }
        if (value instanceof Map) {
            return "Map";
        }
        return "String";
    }

    public long getLong() throws InvalidBEncodingException {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        throw new InvalidBEncodingException("Long expected but " + value.getClass().toString() + " found.");
    }

    public String getString() throws InvalidBEncodingException {
        if (value instanceof String) {
            return (String) value;
        }
        throw new InvalidBEncodingException("String expected but " + value.getClass().toString() + " found.");
    }

    @SuppressWarnings("unchecked")
    public List<BEncodeValue> getList() throws InvalidBEncodingException {
        if (value instanceof List) {
            return (List<BEncodeValue>) value;
        }
        throw new InvalidBEncodingException("List expected but " + value.getClass().toString() + " found.");
    }

    @SuppressWarnings("unchecked")
    public Map<String, BEncodeValue> getMap() throws InvalidBEncodingException {
        if (value instanceof Map) {
            return (Map<String, BEncodeValue>) value;
        }
        throw new InvalidBEncodingException("Map expected but " + value.getClass().toString() + " found.");
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BEncodeValue value1 = (BEncodeValue) o;

        return value.equals(value1.getValue());

    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
