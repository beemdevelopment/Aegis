package com.beemdevelopment.aegis;

import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.helpers.comparators.AccountNameComparator;
import com.beemdevelopment.aegis.helpers.comparators.IssuerNameComparator;

import java.util.Collections;
import java.util.Comparator;

public enum SortCategory {
    CUSTOM,
    ACCOUNT,
    ACCOUNT_REVERSED,
    ISSUER,
    ISSUER_REVERSED;

    private static SortCategory[] _values;

    static {
        _values = values();
    }

    public static SortCategory fromInteger(int x) {
        return _values[x];
    }

    public Comparator<DatabaseEntry> getComparator() {
        Comparator<DatabaseEntry> comparator = null;

        switch (this) {
            case ACCOUNT:
                comparator = new AccountNameComparator();
                break;
            case ACCOUNT_REVERSED:
                comparator = Collections.reverseOrder(new AccountNameComparator());
                break;
            case ISSUER:
                comparator = new IssuerNameComparator();
                break;
            case ISSUER_REVERSED:
                comparator = Collections.reverseOrder(new IssuerNameComparator());
                break;
        }

        return comparator;
    }

    public int getMenuItem() {
        switch (this) {
            case CUSTOM:
                return R.id.menu_sort_custom;
            case ACCOUNT:
                return R.id.menu_sort_alphabetically_name;
            case ACCOUNT_REVERSED:
                return R.id.menu_sort_alphabetically_name_reverse;
            case ISSUER:
                return R.id.menu_sort_alphabetically;
            case ISSUER_REVERSED:
                return R.id.menu_sort_alphabetically_reverse;
            default:
                return R.id.menu_sort_custom;
        }
    }
}
