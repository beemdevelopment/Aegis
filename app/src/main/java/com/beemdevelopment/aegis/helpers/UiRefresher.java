package com.beemdevelopment.aegis.helpers;

import android.os.Handler;

import com.beemdevelopment.aegis.VibrationPatterns;

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

        _handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                _listener.onRefresh();
                _handler.postDelayed(this, _listener.getMillisTillNextRefresh());
            }
        }, _listener.getMillisTillNextRefresh());

        _handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                _listener.onExpiring();
                _handler.postDelayed(this, getNextRun());
            }
        },  getInitialRun());
    }

    private long getInitialRun() {
        long sum = _listener.getMillisTillNextRefresh() - VibrationPatterns.getLengthInMillis(VibrationPatterns.EXPIRING);
        if (sum < 0) {
            return getNextRun();
        }

        return sum;
    }

    private long getNextRun() {
        return (_listener.getMillisTillNextRefresh() + _listener.getPeriodMillis()) - VibrationPatterns.getLengthInMillis(VibrationPatterns.EXPIRING);
    }

    public void stop() {
        _handler.removeCallbacksAndMessages(null);
        _running = false;
    }

    public interface Listener {
        void onRefresh();
        void onExpiring();
        long getMillisTillNextRefresh();
        long getPeriodMillis();
    }
}
