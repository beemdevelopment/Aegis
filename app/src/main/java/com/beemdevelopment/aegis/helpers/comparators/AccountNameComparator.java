package com.beemdevelopment.aegis.helpers.comparators;

import com.beemdevelopment.aegis.vault.VaultEntry;

import java.text.Collator;
import java.util.Comparator;

public class AccountNameComparator implements Comparator<VaultEntry> {
    private final Collator _collator;

    public AccountNameComparator() {
        _collator = Collator.getInstance();
        _collator.setStrength(Collator.SECONDARY);
    }

    @Override
    public int compare(VaultEntry a, VaultEntry b) {
        int result = _collator.compare(a.getName(), b.getName());
        if (result == 0) {
            result = a.getName().compareTo(b.getName());
        }
        return result;
    }
}