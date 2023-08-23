package com.beemdevelopment.aegis.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UUIDMapTest {
    @Test
    public void addValue() {
        // try adding a new value
        UUIDMap<Value> map = new UUIDMap<>();
        Value value = addNewValue(map);

        // try re-adding the value
        assertThrows(AssertionError.class, () -> map.add(value));

        // try adding a clone of the value
        assertThrows(AssertionError.class, () -> map.add(Cloner.clone(value)));
    }

    @Test
    public void removeValue() {
        // try removing a value
        UUIDMap<Value> map = new UUIDMap<>();
        final Value value = addNewValue(map);
        Value oldValue = map.remove(value);
        assertFalse(map.has(value));

        // ensure we got the original value back
        assertEquals(value, oldValue);

        // try removing a non-existent value
        assertThrows(AssertionError.class, () -> map.remove(value));

        // try removing a value using a clone
        Value value2 = addNewValue(map);
        map.remove(Cloner.clone(value2));
        assertFalse(map.has(value2));
    }

    @Test
    public void replaceValue() {
        UUIDMap<Value> map = new UUIDMap<>();
        Value value = addNewValue(map);

        // replace the value with a clone
        Value valueClone = Cloner.clone(value);
        Value oldValue = map.replace(valueClone);

        // ensure we got the original value back
        assertEquals(value, oldValue);

        // ensure that the clone is now stored in the map
        assertSame(map.getByUUID(value.getUUID()), valueClone);
    }

    @Test
    public void moveValue() {
        // move the first value to the last value
        UUIDMap<Value> map = fillNewMap(4);
        Value[] values = map.getValues().toArray(new Value[0]);
        map.move(values[0], values[3]);
        assertArrayEquals(map.getValues().toArray(new Value[0]), new Value[]{
                values[1],
                values[2],
                values[3],
                values[0]
        });

        // move the last value to the first value
        map = fillNewMap(4);
        values = map.getValues().toArray(new Value[0]);
        map.move(values[3], values[0]);
        assertArrayEquals(map.getValues().toArray(new Value[0]), new Value[]{
                values[3],
                values[0],
                values[1],
                values[2]
        });

        // move the second value to the third value
        map = fillNewMap(4);
        values = map.getValues().toArray(new Value[0]);
        map.move(values[1], values[2]);
        assertArrayEquals(map.getValues().toArray(new Value[0]), new Value[]{
                values[0],
                values[2],
                values[1],
                values[3]
        });

        // move the third value to the second value
        map = fillNewMap(4);
        values = map.getValues().toArray(new Value[0]);
        map.move(values[2], values[1]);
        assertArrayEquals(map.getValues().toArray(new Value[0]), new Value[]{
                values[0],
                values[2],
                values[1],
                values[3]
        });

        // move the third value to the first value
        map = fillNewMap(4);
        values = map.getValues().toArray(new Value[0]);
        map.move(values[2], values[0]);
        assertArrayEquals(map.getValues().toArray(new Value[0]), new Value[]{
                values[2],
                values[0],
                values[1],
                values[3]
        });
    }

    private UUIDMap<Value> fillNewMap(int n) {
        UUIDMap<Value> map = new UUIDMap<>();
        for (int i = 0; i < n; i++) {
            addNewValue(map);
        }
        return map;
    }

    private Value addNewValue(UUIDMap<Value> map) {
        Value value = new Value();
        assertFalse(map.has(value));
        map.add(value);
        assertTrue(map.has(value));
        return value;
    }

    private static class Value extends UUIDMap.Value {

    }
}
