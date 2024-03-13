package com.beemdevelopment.aegis.ui;

import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_BIOMETRIC;
import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_INVALID;
import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_NONE;
import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_PASS;

import android.os.Bundle;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.intro.IntroBaseActivity;
import com.beemdevelopment.aegis.ui.intro.SlideFragment;
import com.beemdevelopment.aegis.ui.slides.DoneSlide;
import com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide;
import com.beemdevelopment.aegis.ui.slides.SecuritySetupSlide;
import com.beemdevelopment.aegis.ui.slides.WelcomeSlide;
import com.beemdevelopment.aegis.vault.VaultFile;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultRepository;
import com.beemdevelopment.aegis.vault.VaultRepositoryException;
import com.beemdevelopment.aegis.vault.slots.BiometricSlot;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;

public class IntroActivity extends IntroBaseActivity {
    // Permission request codes
    private static final int CODE_PERM_NOTIFICATIONS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(WelcomeSlide.class);
        addSlide(SecurityPickerSlide.class);
        addSlide(SecuritySetupSlide.class);
        addSlide(DoneSlide.class);
    }

    @Override
    protected boolean onBeforeSlideChanged(Class<? extends SlideFragment> oldSlide, @NonNull Class<? extends SlideFragment> newSlide) {
        // hide the keyboard before every slide change
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        if (oldSlide == SecurityPickerSlide.class
                && newSlide == SecuritySetupSlide.class
                && getState().getInt("cryptType", CRYPT_TYPE_INVALID) == CRYPT_TYPE_NONE) {
            skipToSlide(DoneSlide.class);
            return true;
        }

        if (oldSlide == WelcomeSlide.class
                && newSlide == SecurityPickerSlide.class
                && getState().getBoolean("imported")) {
            skipToSlide(DoneSlide.class);
            return true;
        }

        // on the welcome page, we don't want the keyboard to push any views up
        getWindow().setSoftInputMode(newSlide == WelcomeSlide.class
                ? WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                : WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        return false;
    }

    @Override
    protected void onAfterSlideChanged(@Nullable Class<? extends SlideFragment> oldSlide, @NonNull Class<? extends SlideFragment> newSlide) {
        // If the user has enabled encryption, we need to request permission to show notifications
        // in order to be able to show the "Vault unlocked" notification.
        //
        // NOTE: Disabled for now. See issue: #1047
        /*if (newSlide == DoneSlide.class && getState().getSerializable("creds") != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionHelper.request(this, CODE_PERM_NOTIFICATIONS, Manifest.permission.POST_NOTIFICATIONS);
            }
        }*/
    }

    @Override
    protected void onDonePressed() {
        Bundle state = getState();

        VaultFileCredentials creds = (VaultFileCredentials) state.getSerializable("creds");
        if (!state.getBoolean("imported")) {
            int cryptType = state.getInt("cryptType", CRYPT_TYPE_INVALID);
            if (cryptType == CRYPT_TYPE_INVALID
                    || (cryptType == CRYPT_TYPE_NONE && creds != null)
                    || (cryptType == CRYPT_TYPE_PASS && (creds == null || !creds.getSlots().has(PasswordSlot.class)))
                    || (cryptType == CRYPT_TYPE_BIOMETRIC && (creds == null || !creds.getSlots().has(PasswordSlot.class) || !creds.getSlots().has(BiometricSlot.class)))) {
                throw new RuntimeException(String.format("State of SecuritySetupSlide not properly propagated, cryptType: %d, creds: %s", cryptType, creds));
            }

            try {
                _vaultManager.initNew(creds);
            } catch (VaultRepositoryException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(this, R.string.vault_init_error, e);
                return;
            }
        } else {
            VaultFile vaultFile;
            try {
                vaultFile = VaultRepository.readVaultFile(this);
            } catch (VaultRepositoryException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(this, R.string.vault_load_error, e);
                return;
            }

            try {
                _vaultManager.loadFrom(vaultFile, creds);
            } catch (VaultRepositoryException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(this, R.string.vault_load_error, e);
                return;
            }
        }

        // skip the intro from now on
        _prefs.setIntroDone(true);

        setResult(RESULT_OK);
        finish();
    }
}
