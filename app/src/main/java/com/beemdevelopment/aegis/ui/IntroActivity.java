package com.beemdevelopment.aegis.ui;

import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_BIOMETRIC;
import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_INVALID;
import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_NONE;
import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_PASS;

import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ThemeMap;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.intro.IntroBaseActivity;
import com.beemdevelopment.aegis.ui.intro.SlideFragment;
import com.beemdevelopment.aegis.ui.slides.DoneSlide;
import com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide;
import com.beemdevelopment.aegis.ui.slides.SecuritySetupSlide;
import com.beemdevelopment.aegis.ui.slides.WelcomeSlide;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultRepositoryException;
import com.beemdevelopment.aegis.vault.slots.BiometricSlot;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;

public class IntroActivity extends IntroBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(WelcomeSlide.class);
        addSlide(SecurityPickerSlide.class);
        addSlide(SecuritySetupSlide.class);
        addSlide(DoneSlide.class);
    }

    @Override
    protected void onSetTheme() {
        setTheme(ThemeMap.NO_ACTION_BAR);
    }

    @Override
    protected boolean onBeforeSlideChanged(Class<? extends SlideFragment> oldSlide, Class<? extends SlideFragment> newSlide) {
        // hide the keyboard before every slide change
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        if (oldSlide == SecurityPickerSlide.class
                && newSlide == SecuritySetupSlide.class
                && getState().getInt("cryptType", CRYPT_TYPE_INVALID) == CRYPT_TYPE_NONE) {
            skipToSlide(DoneSlide.class);
            return true;
        }

        return false;
    }

    @Override
    protected void onDonePressed() {
        Bundle state = getState();

        int cryptType = state.getInt("cryptType", CRYPT_TYPE_INVALID);
        VaultFileCredentials creds = (VaultFileCredentials) state.getSerializable("creds");
        if (cryptType == CRYPT_TYPE_INVALID
                || (cryptType == CRYPT_TYPE_NONE && creds != null)
                || (cryptType == CRYPT_TYPE_PASS && (creds == null || !creds.getSlots().has(PasswordSlot.class)))
                || (cryptType == CRYPT_TYPE_BIOMETRIC && (creds == null || !creds.getSlots().has(PasswordSlot.class) || !creds.getSlots().has(BiometricSlot.class)))) {
            throw new RuntimeException(String.format("State of SecuritySetupSlide not properly propagated, cryptType: %d, creds: %s", cryptType, creds));
        }

        try {
            _vaultManager.init(creds);
        } catch (VaultRepositoryException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.vault_init_error, e);
            return;
        }

        // skip the intro from now on
        _prefs.setIntroDone(true);

        setResult(RESULT_OK);
        finish();
    }
}
