package com.beemdevelopment.aegis.helpers.comparators;

import com.beemdevelopment.aegis.vault.VaultEntry;

import java.util.Comparator;

public class FavoriteComparator implements Comparator<VaultEntry> {
    @Override
    public int compare(VaultEntry a, VaultEntry b) {
        return -1 * Boolean.compare(a.isFavorite(), b.isFavorite());
    }
}