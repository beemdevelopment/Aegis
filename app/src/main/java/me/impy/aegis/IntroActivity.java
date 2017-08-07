package me.impy.aegis;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import java.lang.reflect.UndeclaredThrowableException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.MessageButtonBehaviour;
import agency.tango.materialintroscreen.SlideFragmentBuilder;
import me.impy.aegis.crypto.CryptResult;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.crypto.slots.PasswordSlot;
import me.impy.aegis.crypto.slots.SlotCollection;
import me.impy.aegis.db.Database;
import me.impy.aegis.db.DatabaseFile;

public class IntroActivity extends MaterialIntroActivity {
    public static final int RESULT_OK = 0;
    public static final int RESULT_EXCEPTION = 1;

    private CustomAuthenticatedSlide authenticatedSlide;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideBackButton();

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.colorPrimary)
                .buttonsColor(R.color.colorAccent)
                .image(R.drawable.intro_shield)
                .title("Welcome")
                .description("Aegis is a brand new open source(!) authenticator app which generates tokens for your accounts.")
                .build());

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.colorAccent)
                .buttonsColor(R.color.colorPrimary)
                .neededPermissions(new String[]{Manifest.permission.CAMERA})
                .image(R.drawable.intro_scanner)
                .title("Permissions")
                .description("Aegis needs permission to your camera in order to function properly. This is needed to scan QR codes.")
                .build(),
                new MessageButtonBehaviour(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                }, "Permission granted"));

        addSlide(new CustomAuthenticationSlide());

        authenticatedSlide = new CustomAuthenticatedSlide();
        addSlide(authenticatedSlide);

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.colorPrimary)
                .buttonsColor(R.color.colorAccent)
                .image(R.drawable.intro_shield)
                .title("All done!")
                .description("Aegis has been set up and is ready to go.")
                .build());
    }

    private void setException(Exception e) {
        Intent result = new Intent();
        result.putExtra("exception", e);
        setResult(RESULT_EXCEPTION, result);
    }

    @Override
    public void onFinish() {
        super.onFinish();

        // create the database and database file
        Database database = new Database();
        DatabaseFile databaseFile = new DatabaseFile();

        int cryptType = authenticatedSlide.getCryptType();

        // generate the master key
        MasterKey masterKey = null;
        if (cryptType != CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
            try {
                masterKey = MasterKey.generate();
            } catch (Exception e) {
                setException(e);
                return;
            }
        }

        if (cryptType != CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
            try {
                // encrypt the master key with a key derived from the user's password
                // and add it to the list of slots
                SlotCollection slots = databaseFile.getSlots();
                PasswordSlot slot = new PasswordSlot();
                Cipher cipher = authenticatedSlide.getCipher(slot, Cipher.ENCRYPT_MODE);
                masterKey.encryptSlot(slot, cipher);
                slots.add(slot);
            } catch (Exception e) {
                setException(e);
                return;
            }
        }

        if (cryptType != CustomAuthenticationSlide.CRYPT_TYPE_FINGER) {
            // TODO
        }

        // finally, save the database
        try {
            byte[] bytes = database.serialize();
            if (cryptType == CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
                databaseFile.setContent(bytes);
            } else {
                CryptResult result = masterKey.encrypt(bytes);
                databaseFile.setContent(result.Data);
                databaseFile.setCryptParameters(result.Parameters);
            }
            databaseFile.save(getApplicationContext());
        } catch (Exception e) {
            setException(e);
            return;
        }

        // send the master key back to the main activity
        Intent result = new Intent();
        result.putExtra("key", masterKey);
        setResult(RESULT_OK, result);

        // skip the intro from now on
        // TODO: show the intro if we can't find any database files
        SharedPreferences prefs = this.getSharedPreferences("me.impy.aegis", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("passedIntro", true).apply();
    }
}
