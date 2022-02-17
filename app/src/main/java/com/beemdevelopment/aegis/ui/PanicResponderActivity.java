package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.beemdevelopment.aegis.BuildConfig;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.crypto.pins.GuardianProjectFDroidRSA2048;
import com.beemdevelopment.aegis.vault.VaultManager;

import info.guardianproject.GuardianProjectRSA4096;
import info.guardianproject.trustedintents.TrustedIntents;

public class PanicResponderActivity extends AegisActivity {
    public static final String PANIC_TRIGGER_ACTION = "info.guardianproject.panic.action.TRIGGER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Preferences prefs = getPreferences();

        if (!prefs.isPanicTriggerEnabled()) {
            Toast.makeText(this, R.string.panic_trigger_ignore_toast, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Intent intent;
        if (!BuildConfig.TEST.get()) {
            TrustedIntents trustedIntents = TrustedIntents.get(this);
            trustedIntents.addTrustedSigner(GuardianProjectRSA4096.class);
            trustedIntents.addTrustedSigner(GuardianProjectFDroidRSA2048.class);

            intent = trustedIntents.getIntentFromTrustedSender(this);
        } else {
            intent = getIntent();
        }

        if (intent != null && PANIC_TRIGGER_ACTION.equals(intent.getAction())) {
            getApp().lock(false);
            VaultManager.deleteFile(this);
            finishApp();
            return;
        }

        finish();
    }

    private void finishApp() {
        ExitActivity.exitAppAndRemoveFromRecents(this);
        finishAndRemoveTask();
    }
}
