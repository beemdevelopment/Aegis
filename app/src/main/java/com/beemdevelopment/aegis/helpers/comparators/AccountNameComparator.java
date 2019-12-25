package com.beemdevelopment.aegis.helpers.comparators;

import com.beemdevelopment.aegis.vault.VaultEntry;

import java.util.Comparator;

public class AccountNameComparator implements Comparator<VaultEntry> {
    @Override
    public int compare(VaultEntry a, VaultEntry b) {
        return a.getName().compareToIgnoreCase(b.getName());
    }
}