package com.beemdevelopment.aegis.icons;

public class IconPackExistsException extends IconPackException {
    private IconPack _pack;

    public IconPackExistsException(IconPack pack) {
        super(String.format("Icon pack %s (%d) already exists", pack.getName(), pack.getVersion()));
        _pack = pack;
    }

    public IconPack getIconPack() {
        return _pack;
    }
}
