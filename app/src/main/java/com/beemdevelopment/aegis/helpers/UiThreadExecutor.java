package com.beemdevelopment.aegis.helpers;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

public class UiThreadExecutor implements Executor {
    private final Handler _handler = new Handler(Looper.getMainLooper());

    @Override
    public void execute(@NonNull Runnable command) {
        _handler.post(command);
    }
}
