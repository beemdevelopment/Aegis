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
     * Retrieves the height (in dp) that the divider between entries should have in this view mode.
     */
    public float getDividerHeight() {
        if (this == ViewMode.COMPACT) {
            return 0;
        } else if (this == ViewMode.TILES) {
            return 4;
        }

        return 20;
    }

    public int getColumnSpan() {
        if (this == ViewMode.TILES) {
            return 2;
        }

        return 1;
    }

    public float getDividerWidth() {
        if (this == ViewMode.TILES) {
            return 4;
        }

        return 0;
    }

    public String getFormattedAccountName(String accountName) {
        if (this == ViewMode.TILES) {
            return accountName;
        }

        return String.format("(%s)", accountName);
    }
}
