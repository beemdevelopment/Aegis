package com.beemdevelopment.aegis.util;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A map data structure abstraction for storing values with a UUID as the key. Keys
 * must be specified by the value itself, instead of separately. It uses a
 * LinkedHashMap internally (a hash map with a separate linked list that maintains
 * the order).
 * @param <T> The type of values in this map
 */
public class UUIDMap <T extends UUIDMap.Value> implements Iterable<T>, Serializable {
    private LinkedHashMap<UUID, T> _map = new LinkedHashMap<>();

    /**
     * Adds a value to the internal map.
     * @throws AssertionError if a map value with the UUID of the given value already exists.
     */
    public void add(T value) {
        UUID uuid = value.getUUID();
        if (_map.containsKey(uuid)) {
            throw new AssertionError(String.format("Existing value found with UUID: %s", uuid));
        }
        _map.put(uuid, value);
    }

    /**
     * Removes a value from the internal map.
     * @throws AssertionError if no map value exists with the UUID of the given value.
     * @return The old value that is now no longer present in the internal map.
     */
    public T remove(T value) {
        T oldValue = getByUUID(value.getUUID());
        _map.remove(oldValue.getUUID());
        return oldValue;
    }

    /**
     * Clears the internal map.
     */
    public void wipe() {
        _map.clear();
    }

    /**
     * Replaces an old value (with the same UUID as the new given value) in the
     * internal map with the new given value.
     * @throws AssertionError if no map value exists with the UUID of the given value.
     * @return The old value that is now no longer present in the internal map.
     */
    public T replace(T newValue) {
        T oldValue = getByUUID(newValue.getUUID());
        _map.put(oldValue.getUUID(), newValue);
        return oldValue;
    }

    /**
      * Moves value1 to the position of value2.
      */
    public void move(T value1, T value2) {
        List<T> values = new ArrayList<>(_map.values());

        int vi1 = -1, vi2 = -1;
        for (int i = 0; i < values.size(); i++) {
            T value = values.get(i);
            if (value.getUUID().equals(value1.getUUID())) {
                vi1 = i;
            }
            if (value.getUUID().equals(value2.getUUID())) {
                vi2 = i;
            }
        }

        if (vi1 < 0) {
            throw new AssertionError(String.format("No value found for value1 with UUID: %s", value1.getUUID()));
        }
        if (vi2 < 0) {
            throw new AssertionError(String.format("No value found for value2 with UUID: %s", value2.getUUID()));
        }

        CollectionUtils.move(values, vi1, vi2);

        _map.clear();
        for (T value : values) {
            _map.put(value.getUUID(), value);
        }
    }

    /**
     * Reports whether the internal map contains a value with the UUID of the given value.
     */
    public boolean has(T value) {
        return has(value.getUUID());
    }

    /**
     * Reports whether the internal map contains a value with the given UUID.
     */
    public boolean has(UUID uuid) {
        return _map.containsKey(uuid);
    }

    /**
     * Returns a read-only view of the values in the internal map.
     */
    public Collection<T> getValues() {
        return Collections.unmodifiableCollection(_map.values());
    }

    /**
     * Retrieves an entry from the internal map that has the given UUID.
     * @throws AssertionError if no map value exists with the given UUID.
     */
    public T getByUUID(UUID uuid) {
        T value = _map.get(uuid);
        if (value == null) {
            throw new AssertionError(String.format("No value found with UUID: %s", uuid));
        }
        return value;
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return _map.values().iterator();
    }

    public static abstract class Value implements Serializable {
        private UUID _uuid;

        protected Value(UUID uuid) {
            _uuid = uuid;
        }

        protected Value() {
            this(UUID.randomUUID());
        }

        @NonNull
        public final UUID getUUID() {
            return _uuid;
        }

        /**
         * Resets the UUID of this value by generating a new random one.
         * The caller must ensure that this Value is not in a UUIDMap yet. Otherwise, bad things will happen.
         */
        public final void resetUUID() {
            _uuid = UUID.randomUUID();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof Value)) {
                return false;
            }

            return Objects.equals(getUUID(), ((Value) o).getUUID());
        }
    }
}
