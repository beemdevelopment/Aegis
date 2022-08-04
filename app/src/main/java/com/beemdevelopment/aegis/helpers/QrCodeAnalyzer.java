package com.beemdevelopment.aegis.helpers;

import static android.graphics.ImageFormat.YUV_420_888;

import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class QrCodeAnalyzer implements ImageAnalysis.Analyzer {
    private static final String TAG = QrCodeAnalyzer.class.getSimpleName();
    public static final Size RESOLUTION = new Size(1200, 1600);

    private final QrCodeAnalyzer.Listener _listener;

    public QrCodeAnalyzer(QrCodeAnalyzer.Listener listener) {
        _listener = listener;
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        int format = image.getFormat();
        if (format != YUV_420_888) {
            Log.e(TAG, String.format("Unexpected YUV image format: %d", format));
            image.close();
            return;
        }

        ImageProxy.PlaneProxy plane = image.getPlanes()[0];
        ByteBuffer buf = plane.getBuffer();
        byte[] data = new byte[buf.remaining()];
        buf.get(data);
        buf.rewind();

        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                data,
                plane.getRowStride(),
                image.getHeight(),
                0,
                0,
                image.getWidth(),
                image.getHeight(),
                false
        );

        MultiFormatReader reader = new MultiFormatReader();
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
            hints.put(DecodeHintType.ALSO_INVERTED, true);

            Result result = reader.decode(bitmap, hints);
            if (_listener != null) {
                _listener.onQrCodeDetected(result);
            }
        } catch (NotFoundException ignored) {

        } finally {
            image.close();
        }
    }

    public interface Listener {
        void onQrCodeDetected(Result result);
    }
}
