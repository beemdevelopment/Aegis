package com.beemdevelopment.aegis;

import com.beemdevelopment.aegis.helpers.comparators.AccountNameComparator;
import com.beemdevelopment.aegis.helpers.comparators.IssuerNameComparator;

import java.util.Comparator;

public enum SortCategory {
    ACCOUNT,
    ACCOUNTREVERSED,
    ISSUER,
    ISSUERREVERSED,
    CUSTOM;

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
        switch(sortCategory)
        {
            case ACCOUNTREVERSED:
            case ISSUERREVERSED:
                return true;

            default:
                return false;
        }
    }
}
