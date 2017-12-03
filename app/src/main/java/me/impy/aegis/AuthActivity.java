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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mattprecious.swirl.SwirlView;

import java.lang.reflect.UndeclaredThrowableException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import me.impy.aegis.crypto.KeyStoreHandle;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.crypto.slots.FingerprintSlot;
import me.impy.aegis.crypto.slots.PasswordSlot;
import me.impy.aegis.crypto.slots.Slot;
import me.impy.aegis.crypto.slots.SlotCollection;
import me.impy.aegis.finger.FingerprintUiHelper;
import me.impy.aegis.helpers.AuthHelper;

public class AuthActivity extends AppCompatActivity implements FingerprintUiHelper.Callback, SlotCollectionTask.Callback {
    public static final int RESULT_OK = 0;
    public static final int RESULT_EXCEPTION = 1;

    private EditText _textPassword;

    private SlotCollection _slots;
    private FingerprintUiHelper _fingerHelper;
    private Cipher _fingerCipher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        _textPassword = (EditText) findViewById(R.id.text_password);
        LinearLayout boxFingerprint = (LinearLayout) findViewById(R.id.box_fingerprint);
        SwirlView imgFingerprint = (SwirlView) findViewById(R.id.img_fingerprint);
        TextView textFingerprint = (TextView) findViewById(R.id.text_fingerprint);

        Intent intent = getIntent();
        _slots = (SlotCollection) intent.getSerializableExtra("slots");

        // only show the fingerprint controls if the api version is new enough, permission is granted, a scanner is found and a fingerprint slot is found
        Context context = getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            FingerprintManager manager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED && manager.isHardwareDetected()) {
                if (_slots.has(FingerprintSlot.class)) {
                    try {
                        KeyStoreHandle handle = new KeyStoreHandle();
                        if (handle.keyExists()) {
                            SecretKey key = handle.getKey();
                            _fingerCipher = Slot.createCipher(key, Cipher.DECRYPT_MODE);
                            _fingerHelper = new FingerprintUiHelper(manager, imgFingerprint, textFingerprint, this);
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
                char[] password = AuthHelper.getPassword(_textPassword, true);
                trySlots(PasswordSlot.class, password);
            }
        });
    }

    private void showError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Decryption error");
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

    private <T extends Slot> void trySlots(Class<T> type, Object obj) {
        new SlotCollectionTask<>(type, this, this).execute(new SlotCollectionTask.Params(){{
            Slots = _slots;
            Obj = obj;
        }});
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
