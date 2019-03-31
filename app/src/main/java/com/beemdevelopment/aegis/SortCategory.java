package com.beemdevelopment.aegis;

import com.beemdevelopment.aegis.helpers.comparators.AccountNameComparator;
import com.beemdevelopment.aegis.helpers.comparators.IssuerNameComparator;

import java.util.Comparator;

public enum SortCategory {
    CUSTOM,
    ACCOUNT,
    ACCOUNTREVERSED,
    ISSUER,
    ISSUERREVERSED;

    public static SortCategory fromInteger(int x) {
        switch(x) {
            case 0:
                return CUSTOM;
            case 1:
                return ACCOUNT;
            case 2:
                return ACCOUNTREVERSED;
            case 3:
                return ISSUER;
            case 4:
                return ISSUERREVERSED;
        }
        return null;
    }

    public static Comparator getComparator(SortCategory sortCategory) {
        switch(sortCategory) {
            case ACCOUNT:
            case ACCOUNTREVERSED:
                return new AccountNameComparator();
            case ISSUER:
            case ISSUERREVERSED:
                return new IssuerNameComparator();
            case CUSTOM:
                return new IssuerNameComparator();
        }
        return null;
    }

    public static boolean isReversed(SortCategory sortCategory) {
        switch(sortCategory) {
            case ACCOUNTREVERSED:
            case ISSUERREVERSED:
                return true;

            default:
                return false;
        }
    }

    public static int getMenuItem(SortCategory sortCategory) {
        switch (sortCategory) {
            case CUSTOM:
                return R.id.menu_sort_custom;
            case ACCOUNT:
                return R.id.menu_sort_alphabetically_name;
            case ACCOUNTREVERSED:
                return R.id.menu_sort_alphabetically_name_reverse;
            case ISSUER:
                return R.id.menu_sort_alphabetically;
            case ISSUERREVERSED:
                return R.id.menu_sort_alphabetically_reverse;

            default:
                return R.id.menu_sort_custom;
        }
    }
}
