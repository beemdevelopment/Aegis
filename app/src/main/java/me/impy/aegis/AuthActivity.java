package me.impy.aegis;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.UndeclaredThrowableException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.KeyStoreHandle;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.crypto.slots.FingerprintSlot;
import me.impy.aegis.crypto.slots.PasswordSlot;
import me.impy.aegis.crypto.slots.Slot;
import me.impy.aegis.crypto.slots.SlotCollection;
import me.impy.aegis.crypto.slots.SlotIntegrityException;
import me.impy.aegis.finger.FingerprintUiHelper;
import me.impy.aegis.helpers.AuthHelper;

public class AuthActivity extends AppCompatActivity implements FingerprintUiHelper.Callback {
    public static final int RESULT_OK = 0;
    public static final int RESULT_EXCEPTION = 1;

    private EditText textPassword;

    private SlotCollection slots;
    private LinearLayout boxFingerprint;
    private ImageView imgFingerprint;
    private TextView textFingerprint;
    private FingerprintUiHelper fingerHelper;
    private Cipher fingerCipher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        textPassword = (EditText) findViewById(R.id.text_password);
        boxFingerprint = (LinearLayout) findViewById(R.id.box_fingerprint);
        imgFingerprint = (ImageView) findViewById(R.id.img_fingerprint);
        textFingerprint = (TextView) findViewById(R.id.text_fingerprint);

        Intent intent = getIntent();
        slots = (SlotCollection) intent.getSerializableExtra("slots");

        // only show the fingerprint controls if the api version is new enough, permission is granted, a scanner is found and a fingerprint slot is found
        Context context = getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            FingerprintManager manager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED && manager.isHardwareDetected()) {
                if (slots.has(FingerprintSlot.class)) {
                    try {
                        KeyStoreHandle handle = new KeyStoreHandle();
                        if (handle.keyExists()) {
                            SecretKey key = handle.getKey();
                            fingerCipher = Slot.createCipher(key, Cipher.DECRYPT_MODE);
                            fingerHelper = new FingerprintUiHelper(manager, imgFingerprint, textFingerprint, this);
                            boxFingerprint.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception e) {
                        throw new UndeclaredThrowableException(e);
                    }
                }
            }
        }

        Button button = (Button) findViewById(R.id.button_decrypt);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trySlots(PasswordSlot.class);
            }
        });
    }

    private void showError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Master key integrity check failed for every slot. Make sure you didn't mistype your password.");
        builder.setCancelable(false);
        builder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    private MasterKey decryptPasswordSlot(PasswordSlot slot) throws Exception {
        char[] password = AuthHelper.getPassword(textPassword, true);
        SecretKey key = slot.deriveKey(password);
        CryptoUtils.zero(password);
        Cipher cipher = Slot.createCipher(key, Cipher.DECRYPT_MODE);
        return slots.decrypt(slot, cipher);
    }

    private MasterKey decryptFingerSlot(FingerprintSlot slot) throws Exception {
        return slots.decrypt(slot, fingerCipher);
    }

    private <T extends Slot> void trySlots(Class<T> type) {
        try {
            if (!slots.has(type)) {
                throw new RuntimeException();
            }

            MasterKey masterKey = null;
            for (Slot slot : slots.findAll(type)) {
                try {
                    if (slot instanceof PasswordSlot) {
                        masterKey = decryptPasswordSlot((PasswordSlot) slot);
                    } else if (slot instanceof FingerprintSlot) {
                        masterKey = decryptFingerSlot((FingerprintSlot) slot);
                    } else {
                        throw new RuntimeException();
                    }
                    break;
                } catch (SlotIntegrityException e) {
                }
            }

            if (masterKey == null) {
                throw new SlotIntegrityException();
            }

            setKey(masterKey);
        } catch (SlotIntegrityException e) {
            showError();
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    private void setKey(MasterKey key) {
        // send the master key back to the main activity
        Intent result = new Intent();
        result.putExtra("key", key);
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

        if (fingerHelper != null) {
            fingerHelper.startListening(new FingerprintManager.CryptoObject(fingerCipher));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (fingerHelper != null) {
            fingerHelper.stopListening();
        }
    }

    @Override
    public void onAuthenticated() {
        trySlots(FingerprintSlot.class);
    }

    @Override
    public void onError() {

    }
}
