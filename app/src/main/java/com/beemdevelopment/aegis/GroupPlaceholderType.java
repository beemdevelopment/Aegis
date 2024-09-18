package com.beemdevelopment.aegis;

public enum GroupPlaceholderType {
    ALL,
    NO_GROUP;

    public int getStringRes() {
        switch (this) {
            case ALL:
                return R.string.all;
            case NO_GROUP:
                return R.string.no_group;
            default:
                throw new IllegalArgumentException("Unexpected placeholder type: " + this);
        }
    }
}
