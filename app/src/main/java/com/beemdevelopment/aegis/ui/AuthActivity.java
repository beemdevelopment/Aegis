package com.beemdevelopment.aegis.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.CancelAction;
import com.beemdevelopment.aegis.crypto.KeyStoreHandle;
import com.beemdevelopment.aegis.crypto.KeyStoreHandleException;
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

    private CancelAction _cancelAction;
    private SlotList _slots;
    private FingerprintUiHelper _fingerHelper;
    private FingerprintManager.CryptoObject _fingerCryptoObj;

    @Override
    @SuppressLint("NewApi")
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
        _cancelAction = (CancelAction) intent.getSerializableExtra("cancelAction");

        // only show the fingerprint controls if the api version is new enough, permission is granted, a scanner is found and a fingerprint slot is found
        if (_slots.has(FingerprintSlot.class) && FingerprintHelper.isSupported() && FingerprintHelper.isAvailable(this)) {
            boolean invalidated = false;
            FingerprintManager manager = FingerprintHelper.getManager(this);

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
                        Cipher cipher = slot.createDecryptCipher(key);
                        _fingerCryptoObj = new FingerprintManager.CryptoObject(cipher);
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
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                char[] password = EditTextHelper.getEditTextChars(_textPassword);
                trySlots(PasswordSlot.class, password);
            }
        });

        if (_fingerHelper == null) {
            _textPassword.requestFocus();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    private void showError() {
        Dialogs.showSecureDialog(new AlertDialog.Builder(this)
                .setTitle(getString(R.string.unlock_vault_error))
                .setMessage(getString(R.string.unlock_vault_error_description))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> selectPassword())
                .create());
    }

    private <T extends Slot> void trySlots(Class<T> type, Object obj) {
        SlotListTask.Params params = new SlotListTask.Params(_slots, obj);
        new SlotListTask<>(type, this, this).execute(params);
    }

    private void selectPassword() {
        _textPassword.selectAll();

        InputMethodManager imm = (InputMethodManager)   getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    @Override
    public void onBackPressed() {
        switch (_cancelAction) {
            case KILL:
                finishAffinity();

            case CLOSE:
                Intent intent = new Intent();
                setResult(RESULT_CANCELED, intent);
                finish();
        }
    }

    @Override
    @SuppressLint("NewApi")
    public void onResume() {
        super.onResume();

        if (_fingerHelper != null) {
            _fingerHelper.startListening(_fingerCryptoObj);
        }
    }

    @Override
    @SuppressLint("NewApi")
    public void onPause() {
        super.onPause();

        if (_fingerHelper != null) {
            _fingerHelper.stopListening();
        }
    }

    @Override
    @SuppressLint("NewApi")
    public void onAuthenticated() {
        trySlots(FingerprintSlot.class, _fingerCryptoObj.getCipher());
    }

    @Override
    public void onError() {

    }

    @Override
    public void onTaskFinished(SlotListTask.Result result) {
        if (result != null) {
            // replace the old slot with the repaired one
            if (result.isSlotRepaired()) {
                _slots.replace(result.getSlot());
            }

            // send the master key back to the main activity
            Intent intent = new Intent();
            intent.putExtra("creds", new DatabaseFileCredentials(result.getKey(), _slots));
            intent.putExtra("repairedSlot", result.isSlotRepaired());
            setResult(RESULT_OK, intent);
            finish();
        } else {
            showError();
        }
    }
}
