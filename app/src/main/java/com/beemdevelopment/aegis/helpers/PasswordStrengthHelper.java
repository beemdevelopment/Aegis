package com.beemdevelopment.aegis.helpers;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.beemdevelopment.aegis.R;
import com.google.android.material.textfield.TextInputLayout;
import com.google.common.base.Strings;
import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;

public class PasswordStrengthHelper {
    // Limit the password length to prevent zxcvbn4j from exploding
    private static final int MAX_PASSWORD_LENGTH = 64;

    // Material design color palette
    private final static String[] COLORS = {"#FF5252", "#FF5252", "#FFC107", "#8BC34A", "#4CAF50"};

    private final Zxcvbn _zxcvbn = new Zxcvbn();
    private final EditText _textPassword;
    private final ProgressBar _barPasswordStrength;
    private final TextView _textPasswordStrength;
    private final TextInputLayout _textPasswordWrapper;

    public PasswordStrengthHelper(
            EditText textPassword,
            ProgressBar barPasswordStrength,
            TextView textPasswordStrength,
            TextInputLayout textPasswordWrapper
    ) {
        _textPassword = textPassword;
        _barPasswordStrength = barPasswordStrength;
        _textPasswordStrength = textPasswordStrength;
        _textPasswordWrapper = textPasswordWrapper;
    }

    public void measure(Context context) {
        if (_textPassword.getText().length() > MAX_PASSWORD_LENGTH) {
            _barPasswordStrength.setProgress(0);
            _textPasswordStrength.setText(R.string.password_strength_unknown);
        } else {
            Strength strength = _zxcvbn.measure(_textPassword.getText());
            _barPasswordStrength.setProgress(strength.getScore());
            _barPasswordStrength.setProgressTintList(ColorStateList.valueOf(Color.parseColor(getColor(strength.getScore()))));
            _textPasswordStrength.setText((_textPassword.getText().length() != 0) ? getString(strength.getScore(), context) : "");
            String warning = strength.getFeedback().getWarning();
            _textPasswordWrapper.setError(warning);
            _textPasswordWrapper.setErrorEnabled(!Strings.isNullOrEmpty(warning));
            strength.wipe();
        }
    }

    private static String getString(int score, Context context) {
        if (score < 0 || score > 4) {
            throw new IllegalArgumentException("Not a valid zxcvbn score");
        }

        String[] strings = context.getResources().getStringArray(R.array.password_strength);
        return strings[score];
    }

    private static String getColor(int score) {
        if (score < 0 || score > 4) {
            throw new IllegalArgumentException("Not a valid zxcvbn score");
        }

        return COLORS[score];
    }
}
