package com.beemdevelopment.aegis.ui.glide;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.caverock.androidsvg.SVG;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SvgBytesDecoder implements ResourceDecoder<ByteBuffer, SVG> {
    private SvgDecoder _decoder = new SvgDecoder();

    @Override
    public boolean handles(@NonNull ByteBuffer source, @NonNull Options options) throws IOException {
        try (ByteArrayInputStream inStream = new ByteArrayInputStream(source.array())) {
            return _decoder.handles(inStream, options);
        }
    }

    public Resource<SVG> decode(@NonNull ByteBuffer source, int width, int height, @NonNull Options options) throws IOException {
        try (ByteArrayInputStream inStream = new ByteArrayInputStream(source.array())) {
            return _decoder.decode(inStream, width, height, options);
        }
    }
}
