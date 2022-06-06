package com.beemdevelopment.aegis.rules;

import android.graphics.Bitmap;

import androidx.test.runner.screenshot.BasicScreenCaptureProcessor;
import androidx.test.runner.screenshot.ScreenCapture;
import androidx.test.runner.screenshot.ScreenCaptureProcessor;
import androidx.test.runner.screenshot.Screenshot;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.IOException;
import java.util.HashSet;

public class ScreenshotTestRule extends TestWatcher {
    @Override
    protected void failed(Throwable e, Description description) {
        super.failed(e, description);

        String filename = description.getTestClass().getSimpleName() + "-" + description.getMethodName();

        ScreenCapture capture = Screenshot.capture();
        capture.setName(filename);
        capture.setFormat(Bitmap.CompressFormat.PNG);

        HashSet<ScreenCaptureProcessor> processors = new HashSet<>();
        processors.add(new BasicScreenCaptureProcessor());

        try {
            capture.process(processors);
        } catch (IOException e2) {
            e.printStackTrace();
        }
    }
}
