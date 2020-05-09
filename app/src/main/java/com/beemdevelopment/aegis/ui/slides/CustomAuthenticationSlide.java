package com.beemdevelopment.aegis.ui.slides;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.BiometricsHelper;
import com.github.appintro.SlidePolicy;
import com.google.android.material.snackbar.Snackbar;

public class CustomAuthenticationSlide extends Fragment implements SlidePolicy, RadioGroup.OnCheckedChangeListener {
    public static final int CRYPT_TYPE_INVALID = 0;
    public static final int CRYPT_TYPE_NONE = 1;
    public static final int CRYPT_TYPE_PASS = 2;
    public static final int CRYPT_TYPE_BIOMETRIC = 3;

    private RadioGroup _buttonGroup;
    private int _bgColor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_authentication_slide, container, false);
        _buttonGroup = view.findViewById(R.id.rg_authenticationMethod);
        _buttonGroup.setOnCheckedChangeListener(this);
        onCheckedChanged(_buttonGroup, _buttonGroup.getCheckedRadioButtonId());

        // only enable the fingerprint option if the api version is new enough, permission is granted and a scanner is found
        if (BiometricsHelper.isAvailable(getContext())) {
            RadioButton button = view.findViewById(R.id.rb_biometrics);
            TextView text = view.findViewById(R.id.text_rb_biometrics);
            button.setEnabled(true);
            text.setEnabled(true);
            _buttonGroup.check(R.id.rb_biometrics);
        }

        view.findViewById(R.id.main).setBackgroundColor(_bgColor);
        return view;
    }

    public void setBgColor(int color) {
        _bgColor = color;
    }

    @Override
    public boolean isPolicyRespected() {
        return _buttonGroup.getCheckedRadioButtonId() != -1;
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {
        Snackbar snackbar = Snackbar.make(getView(), getString(R.string.snackbar_authentication_method), Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        if (i == -1) {
            return;
        }

        int id;
        switch (i) {
            case R.id.rb_none:
                id = CRYPT_TYPE_NONE;
                break;
            case R.id.rb_password:
                id = CRYPT_TYPE_PASS;
                break;
            case R.id.rb_biometrics:
                id = CRYPT_TYPE_BIOMETRIC;
                break;
            default:
                throw new RuntimeException(String.format("Unsupported security setting: %d", i));
        }

        Intent intent = getActivity().getIntent();
        intent.putExtra("cryptType", id);
    }
}
