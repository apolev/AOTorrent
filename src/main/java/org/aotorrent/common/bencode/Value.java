package org.aotorrent.common.bencode;

import java.util.List;
import java.util.Map;

/**
 * Project: bencode-lib
 * User:    dmitry
 * Date:    11/6/13
 */
public class Value {

    Object value;

    public Value(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public String getValueClass() {
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
    public List<Value> getList() throws InvalidBEncodingException {
        if (value instanceof List) {
            return (List<Value>) value;
        }
        throw new InvalidBEncodingException("List expected but " + value.getClass().toString() + " found.");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Value> getMap() throws InvalidBEncodingException {
        if (value instanceof Map) {
            return (Map<String, Value>) value;
        }
        throw new InvalidBEncodingException("Map expected but " + value.getClass().toString() + " found.");
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
