package me.impy.aegis.helpers;

import android.os.Handler;

public class UIRefresher {
    private boolean _running;
    private Listener _listener;
    private Handler _handler;

    public UIRefresher(Listener listener) {
        _listener = listener;
        _handler = new Handler();
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
                if (_running) {
                    _listener.onRefresh();
                    _handler.postDelayed(this, _listener.getMillisTillNextRefresh());
                }
            }
        }, _listener.getMillisTillNextRefresh());
    }

    public void stop() {
        _running = false;
    }

    public interface Listener {
        void onRefresh();
        long getMillisTillNextRefresh();
    }
}
