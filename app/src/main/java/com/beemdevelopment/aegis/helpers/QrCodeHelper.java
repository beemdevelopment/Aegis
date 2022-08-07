package com.beemdevelopment.aegis.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import androidx.annotation.ColorInt;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class QrCodeHelper {
    private QrCodeHelper() {

    }

    public static Result decodeFromSource(LuminanceSource source) throws NotFoundException {
        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.ALSO_INVERTED, true);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        MultiFormatReader reader = new MultiFormatReader();
        return reader.decode(bitmap, hints);
    }

    public static Result decodeFromStream(InputStream inStream) throws DecodeError {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeStream(inStream, null, bmOptions);
        if (bitmap == null) {
            throw new DecodeError("Unable to decode stream to bitmap");
        }

        // If ZXing is not able to decode the image on the first try, we try a couple of
        // more times with smaller versions of the same image.
        for (int i = 0; i <= 2; i++) {
            if (i != 0) {
                bitmap = BitmapHelper.resize(bitmap, bitmap.getWidth() / (i * 2), bitmap.getHeight() / (i * 2));
            }

            try {
                int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
                bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

                LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), pixels);
                return decodeFromSource(source);
            } catch (NotFoundException ignored) {

            }
        }

        throw new DecodeError(NotFoundException.getNotFoundInstance());
    }

    public static Bitmap encodeToBitmap(String data, int width, int height, @ColorInt int backgroundColor) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, width, height);

        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? Color.BLACK : backgroundColor;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public static class DecodeError extends Exception {
        public DecodeError(String message) {
            super(message);
        }

        public DecodeError(Throwable cause) {
            super(cause);
        }
    }
}
