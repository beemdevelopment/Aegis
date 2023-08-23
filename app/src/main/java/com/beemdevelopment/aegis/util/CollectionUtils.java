package com.beemdevelopment.aegis.util;

import java.util.List;

public class CollectionUtils {

    public static <T> void move(List<T> list, int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return;
        }

        T item = list.remove(fromIndex);
        list.add(toIndex, item);
    }
}
