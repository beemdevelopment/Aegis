package com.beemdevelopment.aegis.helpers;

import android.os.Handler;

public class UiRefresher {
    private boolean _running;
    private Listener _listener;
    private Handler _handler;

    public UiRefresher(Listener listener) {
        _listener = listener;
        _handler = new Handler();
    }

    public void destroy() {
        stop();
        _listener = null;
    }

    public void start() {
        if (_running) {
            return;
        }
        _running = true;

        _listener.onRefresh();
        _handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                _listener.onRefresh();
                _handler.postDelayed(this, _listener.getMillisTillNextRefresh());
            }
        }, _listener.getMillisTillNextRefresh());
    }

    public void stop() {
        _handler.removeCallbacksAndMessages(null);
        _running = false;
    }

    public interface Listener {
        void onRefresh();
        long getMillisTillNextRefresh();
    }
}
