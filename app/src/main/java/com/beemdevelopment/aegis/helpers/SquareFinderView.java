package com.beemdevelopment.aegis.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import me.dm7.barcodescanner.core.ViewFinderView;

public class SquareFinderView extends ViewFinderView {

    public SquareFinderView(Context context) {
        super(context);
        init();
    }

    public SquareFinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setSquareViewFinder(true);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}