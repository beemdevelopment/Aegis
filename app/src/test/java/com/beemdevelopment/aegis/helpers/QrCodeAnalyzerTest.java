package com.beemdevelopment.aegis.helpers;

import static android.graphics.ImageFormat.YUV_420_888;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Rect;
import android.media.Image;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;

import com.beemdevelopment.aegis.util.IOUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

@RunWith(RobolectricTestRunner.class)
public class QrCodeAnalyzerTest {
    private static final String _expectedUri = "otpauth://totp/neo4j:Charlotte?secret=B33WS2ALPT34K4BNY24AYROE4M&issuer=neo4j&algorithm=SHA1&digits=6&period=30";

    @Test
    public void testScanQrCode() {
        boolean found = scan("qr.y.gz", 1600, 1200, 1600);
        assertTrue("QR code not found", found);
    }

    @Test
    public void testScanStridedQrCode() {
        boolean found = scan("qr.strided.y.gz", 1840, 1380, 1840);
        assertFalse("QR code found", found);

        found = scan("qr.strided.y.gz", 1840, 1380, 1856);
        assertTrue("QR code not found", found);
    }

    private boolean scan(String fileName, int width, int height, int rowStride) {
        AtomicBoolean found = new AtomicBoolean();
        QrCodeAnalyzer analyzer = new QrCodeAnalyzer(result -> {
            assertEquals(_expectedUri, result.getText());
            found.set(true);
        });

        FakeImageProxy imgProxy;
        try (InputStream inStream = getClass().getResourceAsStream(fileName);
             GZIPInputStream zipStream = new GZIPInputStream(inStream)) {
            imgProxy = new FakeImageProxy(IOUtils.readAll(zipStream), width, height, rowStride);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        analyzer.analyze(imgProxy);
        return found.get();
    }

    private static class FakePlaneProxy implements ImageProxy.PlaneProxy {
        private final byte[] _y;
        private final int _rowStride;

        public FakePlaneProxy(byte[] y, int rowStride) {
            _y = y;
            _rowStride = rowStride;
        }

        @Override
        public int getRowStride() {
            return _rowStride;
        }

        @Override
        public int getPixelStride() {
            return 1;
        }

        @NonNull
        @Override
        public ByteBuffer getBuffer() {
            return ByteBuffer.wrap(_y);
        }
    }

    private static class FakeImageProxy implements ImageProxy {
        private final byte[] _y;
        private final int _width;
        private final int _height;
        private final int _rowStride;

        public FakeImageProxy(byte[] y, int width, int height, int rowStride) {
            _y = y;
            _width = width;
            _height = height;
            _rowStride = rowStride;
        }

        @Override
        public void close() {

        }

        @NonNull
        @Override
        public Rect getCropRect() {
            return null;
        }

        @Override
        public void setCropRect(@Nullable @org.jetbrains.annotations.Nullable Rect rect) {

        }

        @Override
        public int getFormat() {
            return YUV_420_888;
        }

        @Override
        public int getHeight() {
            return _height;
        }

        @Override
        public int getWidth() {
            return _width;
        }

        @NonNull
        @Override
        public ImageProxy.PlaneProxy[] getPlanes() {
            return new PlaneProxy[]{new FakePlaneProxy(_y, _rowStride)};
        }

        @NonNull
        @Override
        public ImageInfo getImageInfo() {
            return null;
        }

        @Nullable
        @Override
        public Image getImage() {
            return null;
        }
    }
}
