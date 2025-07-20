package com.beemdevelopment.aegis.ui.components;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;

public class NoAutofillEditText extends TextInputEditText {

    public NoAutofillEditText(@NonNull Context context) {
        super(context);
    }

    public NoAutofillEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NoAutofillEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public int getAutofillType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return View.AUTOFILL_TYPE_NONE;
        } else {
            return super.getAutofillType();
        }
    }
}