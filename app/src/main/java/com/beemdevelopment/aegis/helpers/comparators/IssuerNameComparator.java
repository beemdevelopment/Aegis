package com.beemdevelopment.aegis.helpers.comparators;

import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.ui.views.EntryHolder;

import java.util.Comparator;

public class IssuerNameComparator implements Comparator<DatabaseEntry> {
    @Override
    public int compare(DatabaseEntry a, DatabaseEntry b) {
        return a.getIssuer().compareToIgnoreCase(b.getIssuer());
    }
}