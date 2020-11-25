package com.beemdevelopment.aegis.ui.intro;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.beemdevelopment.aegis.R;

public class SlideIndicator extends View {
    private Paint _paint;
    private int _slideCount;
    private int _slideIndex;

    private float _dotRadius;
    private float _dotSeparator;
    private int _dotColor;
    private int _dotColorSelected;

    public SlideIndicator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        _paint = new Paint();
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);

        TypedArray array = null;
        try {
            array = context.obtainStyledAttributes(attrs, R.styleable.SlideIndicator);
            _dotRadius = array.getDimension(R.styleable.SlideIndicator_dot_radius, 5f);
            _dotSeparator = array.getDimension(R.styleable.SlideIndicator_dot_separation, 5f);
            _dotColor = array.getColor(R.styleable.SlideIndicator_dot_color, Color.GRAY);
            _dotColorSelected = array.getColor(R.styleable.SlideIndicator_dot_color_selected, Color.BLACK);
        } finally {
            if (array != null) {
                array.recycle();
            }
        }
    }

    public void setSlideCount(int slideCount) {
        if (slideCount < 0) {
            throw new IllegalArgumentException("Slide count cannot be negative");
        }

        _slideCount = slideCount;
        invalidate();
    }

    public void setCurrentSlide(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Slide index cannot be negative");
        }

        if (index + 1 > _slideCount) {
            throw new IllegalStateException(String.format("Slide index out of range, slides: %d, index: %d", _slideCount, index));
        }

        _slideIndex = index;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (_slideCount <= 0) {
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        float dotDp = density * _dotRadius * 2;
        float spaceDp = density * _dotSeparator;

        float offset;
        if (_slideCount % 2 == 0) {
            offset = (spaceDp / 2) + (dotDp / 2) + dotDp * (_slideCount / 2f - 1) + spaceDp * (_slideCount / 2f - 1);
        } else {
            int spaces = _slideCount > 1 ? _slideCount - 2 : 0;
            offset = (_slideCount - 1) * (dotDp / 2) + spaces * spaceDp;
        }

        canvas.translate((getWidth() / 2f) - offset,getHeight() / 2f);

        for (int i = 0; i < _slideCount; i++) {
            int slideIndex = isRtl() ? (_slideCount - 1) - _slideIndex : _slideIndex;
            _paint.setColor(i == slideIndex ? _dotColorSelected : _dotColor);
            canvas.drawCircle(0,0, dotDp / 2, _paint);
            canvas.translate(dotDp + spaceDp,0);
        }
    }

    private boolean isRtl() {
        return getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }
}
