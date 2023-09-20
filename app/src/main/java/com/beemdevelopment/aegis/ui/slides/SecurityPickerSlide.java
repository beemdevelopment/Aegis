package com.beemdevelopment.aegis.ui.slides;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.BiometricsHelper;
import com.beemdevelopment.aegis.ui.intro.SlideFragment;

public class SecurityPickerSlide extends SlideFragment {
    public static final int CRYPT_TYPE_INVALID = 0;
    public static final int CRYPT_TYPE_NONE = 1;
    public static final int CRYPT_TYPE_PASS = 2;
    public static final int CRYPT_TYPE_BIOMETRIC = 3;

    private RadioGroup _buttonGroup;
    private RadioButton _bioButton;
    private TextView _bioText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_security_picker_slide, container, false);
        _buttonGroup = view.findViewById(R.id.rg_authenticationMethod);
        _bioButton = view.findViewById(R.id.rb_biometrics);
        _bioText = view.findViewById(R.id.text_rb_biometrics);
        updateBiometricsOption(true);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBiometricsOption(false);
    }

    /**
     * Updates the status of the biometrics option. Auto-selects the biometrics option
     * if the API version is new enough, permission is granted and a scanner is found.
     */
    private void updateBiometricsOption(boolean autoSelect) {
        boolean canUseBio = BiometricsHelper.isAvailable(requireContext());
        _bioButton.setEnabled(canUseBio);
        _bioText.setEnabled(canUseBio);

        if (!canUseBio && _buttonGroup.getCheckedRadioButtonId() == R.id.rb_biometrics) {
            _buttonGroup.check(R.id.rb_password);
        }

        if (canUseBio && autoSelect) {
            _buttonGroup.check(R.id.rb_biometrics);
        }
    }

    @Override
    public boolean isFinished() {
        return _buttonGroup.getCheckedRadioButtonId() != -1;
    }

    @Override
    public void onNotFinishedError() {
         Toast.makeText(requireContext(), R.string.snackbar_authentication_method, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSaveIntroState(@NonNull Bundle introState) {
        int buttonId = _buttonGroup.getCheckedRadioButtonId();

        int type;
        if (buttonId == R.id.rb_none) {
            type = CRYPT_TYPE_NONE;
        } else if (buttonId == R.id.rb_password) {
            type = CRYPT_TYPE_PASS;
        } else if (buttonId == R.id.rb_biometrics) {
            type = CRYPT_TYPE_BIOMETRIC;
        } else {
            throw new RuntimeException(String.format("Unsupported security type: %d", buttonId));
        }

        introState.putInt("cryptType", type);
    }
}
