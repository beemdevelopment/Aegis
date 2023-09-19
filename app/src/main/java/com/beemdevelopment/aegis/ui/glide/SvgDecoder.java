package com.beemdevelopment.aegis.ui.glide;

import androidx.annotation.NonNull;

import com.beemdevelopment.aegis.icons.IconType;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.io.IOException;
import java.io.InputStream;

import static com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;

// source: https://github.com/bumptech/glide/tree/master/samples/svg/src/main/java/com/bumptech/glide/samples/svg

/** Decodes an SVG internal representation from an {@link InputStream}. */
public class SvgDecoder implements ResourceDecoder<InputStream, SVG> {

    @Override
    public boolean handles(@NonNull InputStream source, @NonNull Options options) {
        return options.get(VaultEntryIconLoader.ICON_TYPE) == IconType.SVG;
    }

    public Resource<SVG> decode(
            @NonNull InputStream source, int width, int height, @NonNull Options options)
            throws IOException {
        try {
            SVG svg = SVG.getFromInputStream(source);
            if (width != SIZE_ORIGINAL) {
                svg.setDocumentWidth(width);
            }
            if (height != SIZE_ORIGINAL) {
                svg.setDocumentHeight(height);
            }
            return new SimpleResource<>(svg);
        } catch (SVGParseException ex) {
            throw new IOException("Cannot load SVG from stream", ex);
        }
    }
}
