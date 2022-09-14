package com.beemdevelopment.aegis.icons;

import com.google.common.io.Files;

import java.util.Locale;

public enum IconType {
    INVALID,
    SVG,
    PNG,
    JPEG;

    public static IconType fromMimeType(String mimeType) {
        switch (mimeType) {
            case "image/svg+xml":
                return SVG;
            case "image/png":
                return PNG;
            case "image/jpeg":
                return JPEG;
            default:
                return INVALID;
        }
    }

    public static IconType fromFilename(String filename) {
        switch (Files.getFileExtension(filename).toLowerCase(Locale.ROOT)) {
            case "svg":
                return SVG;
            case "png":
                return PNG;
            case "jpg":
                // intentional fallthrough
            case "jpeg":
                return JPEG;
            default:
                return INVALID;
        }
    }

    public String toMimeType() {
        switch (this) {
            case SVG:
                return "image/svg+xml";
            case PNG:
                return "image/png";
            case JPEG:
                return "image/jpeg";
            default:
                throw new RuntimeException(String.format("Can't convert icon type %s to MIME type", this));
        }
    }
}
