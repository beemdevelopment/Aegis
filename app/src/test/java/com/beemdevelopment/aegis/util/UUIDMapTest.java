package com.beemdevelopment.aegis.util;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class UUIDMapTest {
    private UUIDMap<Value> _map;

    @Before
    public void init() {
        _map = new UUIDMap<>();
    }

    @Test
    public void addValue() {
        // try adding a new value
        Value value = addNewValue();

        // try re-adding the value
        assertThrows(AssertionError.class, () -> _map.add(value));

        // try adding a clone of the value
        assertThrows(AssertionError.class, () -> _map.add(Cloner.clone(value)));
    }

    @Test
    public void removeValue() {
        // try removing a value
        final Value value = addNewValue();
        Value oldValue = _map.remove(value);
        assertFalse(_map.has(value));

        // ensure we got the original value back
        assertEquals(value, oldValue);

        // try removing a non-existent value
        assertThrows(AssertionError.class, () -> _map.remove(value));

        // try removing a value using a clone
        Value value2 = addNewValue();
        _map.remove(Cloner.clone(value2));
        assertFalse(_map.has(value2));
    }

    @Test
    public void replaceValue() {
        Value value = addNewValue();

        // replace the value with a clone
        Value valueClone = Cloner.clone(value);
        Value oldValue = _map.replace(valueClone);

        // ensure we got the original value back
        assertEquals(value, oldValue);

        // ensure that the clone is now stored in the map
        assertSame(_map.getByUUID(value.getUUID()), valueClone);
    }

    @Test
    public void swapValue() {
        Collection<Value> values = _map.getValues();

        // set up the map with some values
        Value value1 = addNewValue();
        Value value2 = addNewValue();
        Value value3 = addNewValue();
        Value value4 = addNewValue();

        // set up a reference list with the reverse order
        List<Value> ref = new ArrayList<>(values);
        Collections.reverse(ref);

        // the lists should not be equal at this point
        assertNotEquals(values, ref);

        // swap the values and see if the lists are equal now
        _map.swap(value1, value4);
        _map.swap(value2, value3);
        assertArrayEquals(values.toArray(), ref.toArray());
    }

    private Value addNewValue() {
        Value value = new Value();
        assertFalse(_map.has(value));
        _map.add(value);
        assertTrue(_map.has(value));
        return value;
    }

    private static class Value extends UUIDMap.Value {

    }
}
