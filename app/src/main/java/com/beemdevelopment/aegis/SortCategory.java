package com.beemdevelopment.aegis;

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
                return new IssuerNameComparator();
            case ACCOUNTREVERSED:
                return new IssuerNameComparator();
            case ISSUER:
                return new IssuerNameComparator();
            case ISSUERREVERSED:
                return new IssuerNameComparator();
            case CUSTOM:
                return new IssuerNameComparator();
        }
        return null;
    }
}
