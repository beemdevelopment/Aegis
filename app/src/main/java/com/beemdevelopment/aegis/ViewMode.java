package com.beemdevelopment.aegis;

import androidx.annotation.LayoutRes;

public enum ViewMode {
    NORMAL,
    COMPACT,
    SMALL,
    TILES;

    private static ViewMode[] _values;

    static {
        _values = values();
    }

    public static ViewMode fromInteger(int x) {
        return _values[x];
    }

    @LayoutRes
    public int getLayoutId() {
        switch (this) {
            case NORMAL:
                return R.layout.card_entry;
            case COMPACT:
                return R.layout.card_entry_compact;
            case SMALL:
                return R.layout.card_entry_small;
            case TILES:
                return R.layout.card_entry_tile;
            default:
                return R.layout.card_entry;
        }
    }

    /**
     * Retrieves the offset (in dp) that should exist between entries in this view mode.
     */
    public float getItemOffset() {
        if (this == ViewMode.COMPACT) {
            return 1;
        } else if (this == ViewMode.TILES) {
            return 4;
        }

        return 8;
    }

    public int getSpanCount() {
        if (this == ViewMode.TILES) {
            return 2;
        }

        return 1;
    }

    public String getFormattedAccountName(String accountName) {
        if (this == ViewMode.TILES) {
            return accountName;
        }

        return String.format("(%s)", accountName);
    }
}
