package com.beemdevelopment.aegis;

public enum ViewMode {
    NORMAL,
    COMPACT,
    SMALL;

    public static ViewMode fromInteger(int x) {
        switch (x) {
            case 0:
                return NORMAL;
            case 1:
                return COMPACT;
            case 2:
                return SMALL;
        }
        return null;
    }

    public static int getViewModeNameResource(int x) {
        switch (x) {
            case 0:
                return R.string.normal_viewmode_title;
            case 1:
                return R.string.compact_mode_title;
            case 2:
                return R.string.small_mode_title;
        }

        return R.string.normal_viewmode_title;
    }

    public static String[] getViewModeNames() {
        return new String[]{
                "Normal",
                "Compact",
                "Small"
        };
    }

    public static int[] getViewModeNameResources() {
        return new int[] {
                R.string.normal_viewmode_title,
                R.string.compact_mode_title,
                R.string.small_mode_title
        };
    }
}
