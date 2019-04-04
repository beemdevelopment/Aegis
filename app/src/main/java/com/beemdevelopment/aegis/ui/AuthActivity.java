package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.crypto.KeyStoreHandle;
import com.beemdevelopment.aegis.crypto.KeyStoreHandleException;
import com.beemdevelopment.aegis.crypto.MasterKey;
import com.beemdevelopment.aegis.db.DatabaseFileCredentials;
import com.beemdevelopment.aegis.db.slots.FingerprintSlot;
import com.beemdevelopment.aegis.db.slots.PasswordSlot;
import com.beemdevelopment.aegis.db.slots.Slot;
import com.beemdevelopment.aegis.db.slots.SlotException;
import com.beemdevelopment.aegis.db.slots.SlotList;
import com.beemdevelopment.aegis.helpers.EditTextHelper;
import com.beemdevelopment.aegis.helpers.FingerprintHelper;
import com.beemdevelopment.aegis.helpers.FingerprintUiHelper;
import com.beemdevelopment.aegis.ui.tasks.SlotListTask;
import com.mattprecious.swirl.SwirlView;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import androidx.appcompat.app.AlertDialog;

public class AuthActivity extends AegisActivity implements FingerprintUiHelper.Callback, SlotListTask.Callback {
    private EditText _textPassword;

    private SlotList _slots;
    private FingerprintUiHelper _fingerHelper;
    private Cipher _fingerCipher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        _textPassword = findViewById(R.id.text_password);
        LinearLayout boxFingerprint = findViewById(R.id.box_fingerprint);
        LinearLayout boxFingerprintInfo = findViewById(R.id.box_fingerprint_info);
        TextView textFingerprint = findViewById(R.id.text_fingerprint);
        Button decryptButton = findViewById(R.id.button_decrypt);

        _textPassword.setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                decryptButton.performClick();
            }
            return false;
        });

        SwirlView imgFingerprint = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ViewGroup insertPoint = findViewById(R.id.img_fingerprint_insert);
            imgFingerprint = new SwirlView(this);
            insertPoint.addView(imgFingerprint, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        Intent intent = getIntent();
        _slots = (SlotList) intent.getSerializableExtra("slots");

        // only show the fingerprint controls if the api version is new enough, permission is granted, a scanner is found and a fingerprint slot is found
        FingerprintManager manager = FingerprintHelper.getManager(this);
        if (manager != null && _slots.has(FingerprintSlot.class)) {
            boolean invalidated = false;
            try {
                // find a fingerprint slot with an id that matches an alias in the keystore
                for (FingerprintSlot slot : _slots.findAll(FingerprintSlot.class)) {
                    String id = slot.getUUID().toString();
                    KeyStoreHandle handle = new KeyStoreHandle();
                    if (handle.containsKey(id)) {
                        SecretKey key = handle.getKey(id);
                        // if 'key' is null, it was permanently invalidated
                        if (key == null) {
                            invalidated = true;
                            continue;
                        }
                        _fingerCipher = slot.createDecryptCipher(key);
                        _fingerHelper = new FingerprintUiHelper(manager, imgFingerprint, textFingerprint, this);
                        boxFingerprint.setVisibility(View.VISIBLE);
                        invalidated = false;
                        break;
                    }
                }
            } catch (KeyStoreHandleException | SlotException e) {
                throw new RuntimeException(e);
            }

            // display a help message if a matching invalidated keystore entry was found
            if (invalidated) {
                boxFingerprintInfo.setVisibility(View.VISIBLE);
            }
        }

        decryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                char[] password = EditTextHelper.getEditTextChars(_textPassword);
                trySlots(PasswordSlot.class, password);
            }
        });
    }

    private void showError() {
        Dialogs.showSecureDialog(new AlertDialog.Builder(this)
                .setTitle(getString(R.string.unlock_vault_error))
                .setMessage(getString(R.string.unlock_vault_error_description))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null)
                .create());
    }

    private <T extends Slot> void trySlots(Class<T> type, Object obj) {
        SlotListTask.Params params = new SlotListTask.Params(_slots, obj);
        new SlotListTask<>(type, this, this).execute(params);
    }

    private void setKey(MasterKey key) {
        // send the master key back to the main activity
        Intent result = new Intent();
        result.putExtra("creds", new DatabaseFileCredentials(key, _slots));
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onBackPressed() {
        // ignore back button presses
    }

    @Override
    public void onResume() {
        super.onResume();

        if (_fingerHelper != null) {
            _fingerHelper.startListening(new FingerprintManager.CryptoObject(_fingerCipher));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (_fingerHelper != null) {
            _fingerHelper.stopListening();
        }
    }

    @Override
    public void onAuthenticated() {
        trySlots(FingerprintSlot.class, _fingerCipher);
    }

    @Override
    public void onError() {

    }

    @Override
    public void onTaskFinished(MasterKey key) {
        if (key != null) {
            setKey(key);
        } else {
            showError();
        }
    }
}
