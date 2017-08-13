package me.impy.aegis;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.lang.reflect.UndeclaredThrowableException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.crypto.slots.PasswordSlot;
import me.impy.aegis.crypto.slots.Slot;
import me.impy.aegis.crypto.slots.SlotCollection;

public class AuthActivity extends AppCompatActivity {
    public static final int RESULT_OK = 0;
    public static final int RESULT_EXCEPTION = 1;

    private EditText textPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        textPassword = (EditText) findViewById(R.id.text_password);

        Intent intent = getIntent();
        final SlotCollection slots = (SlotCollection) intent.getSerializableExtra("slots");

        Button button = (Button) findViewById(R.id.button_decrypt);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MasterKey masterKey = null;
                try {
                    if (slots.has(PasswordSlot.class)) {
                        PasswordSlot slot = slots.find(PasswordSlot.class);
                        char[] password = getPassword(true);
                        SecretKey key = slot.deriveKey(password);
                        CryptoUtils.zero(password);
                        Cipher cipher = Slot.createCipher(key, Cipher.DECRYPT_MODE);
                        masterKey = MasterKey.decryptSlot(slot, cipher);
                    }
                } catch (Exception e) {
                    // TODO: feedback
                    throw new UndeclaredThrowableException(e);
                }

                // send the master key back to the main activity
                Intent result = new Intent();
                result.putExtra("key", masterKey);
                setResult(RESULT_OK, result);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // ignore back button presses
    }

    private char[] getPassword(boolean clear) {
        char[] password = getEditTextChars(textPassword);
        if (clear) {
            textPassword.getText().clear();
        }
        return password;
    }

    private static char[] getEditTextChars(EditText text) {
        Editable editable = text.getText();
        char[] chars = new char[editable.length()];
        editable.getChars(0, editable.length(), chars, 0);
        return chars;
    }
}
