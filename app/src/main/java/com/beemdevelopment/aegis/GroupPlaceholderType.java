package com.beemdevelopment.aegis;

public enum GroupPlaceholderType {
    ALL,
    NEW_GROUP,
    NO_GROUP;

    public int getStringRes() {
        switch (this) {
            case ALL:
                return R.string.all;
            case NEW_GROUP:
                return R.string.new_group;
            case NO_GROUP:
                return R.string.no_group;
            default:
                throw new IllegalArgumentException("Unexpected placeholder type: " + this);
        }
    }
}
