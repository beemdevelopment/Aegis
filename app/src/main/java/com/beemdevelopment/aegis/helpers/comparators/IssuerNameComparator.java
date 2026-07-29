package com.beemdevelopment.aegis.helpers.comparators;

import com.beemdevelopment.aegis.vault.VaultEntry;

import java.text.Collator;
import java.util.Comparator;

public class IssuerNameComparator implements Comparator<VaultEntry> {
    private final Collator _collator;

    public IssuerNameComparator() {
        _collator = Collator.getInstance();
        _collator.setStrength(Collator.SECONDARY);
    }

    @Override
    public int compare(VaultEntry a, VaultEntry b) {
        int result = _collator.compare(a.getIssuer(), b.getIssuer());
        if (result == 0) {
            result = a.getIssuer().compareTo(b.getIssuer());
        }
        return result;
    }
}
