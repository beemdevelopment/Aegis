package me.impy.aegis;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptResult;
import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.crypto.slots.FingerprintSlot;
import me.impy.aegis.crypto.slots.PasswordSlot;
import me.impy.aegis.crypto.slots.Slot;
import me.impy.aegis.crypto.slots.SlotCollection;
import me.impy.aegis.db.Database;
import me.impy.aegis.db.DatabaseFile;

public class IntroActivity extends AppIntro implements DerivationTask.Callback {
    public static final int RESULT_OK = 0;
    public static final int RESULT_EXCEPTION = 1;

    private CustomAuthenticatedSlide _authenticatedSlide;
    private CustomAuthenticationSlide _authenticationSlide;
    private Fragment _endSlide;

    private Database _database;
    private DatabaseFile _databaseFile;
    private PasswordSlot _passwordSlot;
    private Cipher _passwordCipher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showSkipButton(false);
        //showPagerIndicator(false);
        setGoBackLock(true);

        SliderPage homeSliderPage = new SliderPage();
        homeSliderPage.setTitle("Welcome");
        homeSliderPage.setDescription("Aegis is a secure, free and open source 2FA app");
        homeSliderPage.setImageDrawable(R.drawable.intro_shield);
        homeSliderPage.setBgColor(getResources().getColor(R.color.colorPrimary));
        addSlide(AppIntroFragment.newInstance(homeSliderPage));

        SliderPage permSliderPage = new SliderPage();
        permSliderPage.setTitle("Permissions");
        permSliderPage.setDescription("Aegis needs permission to use your camera in order to scan QR codes.");
        permSliderPage.setImageDrawable(R.drawable.intro_scanner);
        permSliderPage.setBgColor(getResources().getColor(R.color.colorAccent));
        addSlide(AppIntroFragment.newInstance(permSliderPage));
        askForPermissions(new String[]{Manifest.permission.CAMERA}, 2);

        _authenticationSlide = new CustomAuthenticationSlide();
        _authenticationSlide.setBgColor(getResources().getColor(R.color.colorHeaderSuccess));
        addSlide(_authenticationSlide);
        _authenticatedSlide = new CustomAuthenticatedSlide();
        _authenticatedSlide.setBgColor(getResources().getColor(R.color.colorPrimary));
        addSlide(_authenticatedSlide);

        SliderPage endSliderPage = new SliderPage();
        endSliderPage.setTitle("All done!");
        endSliderPage.setDescription("Aegis has been set up and is ready to go.");
        endSliderPage.setImageDrawable(R.drawable.intro_shield);
        endSliderPage.setBgColor(getResources().getColor(R.color.colorPrimary));
        _endSlide = AppIntroFragment.newInstance(endSliderPage);
        addSlide(_endSlide);

        // create the database and database file
        _database = new Database();
        _databaseFile = new DatabaseFile();
    }

    private void setException(Exception e) {
        Intent result = new Intent();
        result.putExtra("exception", e);
        setResult(RESULT_EXCEPTION, result);
        finish();
    }

    @Override
    public void onSlideChanged(Fragment oldFragment, Fragment newFragment) {
        Intent intent = getIntent();
        int cryptType = intent.getIntExtra("cryptType", CustomAuthenticationSlide.CRYPT_TYPE_INVALID);

        if (newFragment == _endSlide && cryptType != CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
            _passwordSlot = new PasswordSlot();
            new DerivationTask(this, this).execute(new DerivationTask.Params() {{
                Slot = _passwordSlot;
                Password = _authenticatedSlide.getPassword();
            }});
        } else if (oldFragment == _authenticationSlide && newFragment != _endSlide) {
            // skip to the last slide if no encryption will be used
            if (cryptType == CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
                // TODO: no magic indices
                getPager().setCurrentItem(5);
            }
        }
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        int cryptType = _authenticatedSlide.getCryptType();

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

        SlotCollection slots = _databaseFile.getSlots();
        if (cryptType != CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
            try {
                // encrypt the master key with a key derived from the user's password
                // and add it to the list of slots
                if (_passwordSlot == null || _passwordCipher == null) {
                    throw new RuntimeException();
                }
                try {
                    slots.encrypt(_passwordSlot, masterKey, _passwordCipher);
                    slots.add(_passwordSlot);
                } catch (Exception e) {
                    setException(e);
                }
            } catch (Exception e) {
                setException(e);
                return;
            }
        }

        if (cryptType == CustomAuthenticationSlide.CRYPT_TYPE_FINGER) {
            try {
                // encrypt the master key with the fingerprint key
                // and add it to the list of slots
                FingerprintSlot slot = new FingerprintSlot();
                Cipher cipher = _authenticatedSlide.getFingerCipher();
                slots.encrypt(slot, masterKey, cipher);
                slots.add(slot);
            } catch (Exception e) {
                setException(e);
                return;
            }
        }

        // finally, save the database
        try {
            byte[] bytes = _database.serialize();
            if (cryptType == CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
                _databaseFile.setContent(bytes);
            } else {
                CryptResult result = masterKey.encrypt(bytes);
                _databaseFile.setContent(result.Data);
                _databaseFile.setCryptParameters(result.Parameters);
            }
            _databaseFile.save(getApplicationContext());
        } catch (Exception e) {
            setException(e);
            return;
        }

        // send the master key back to the main activity
        Intent result = new Intent();
        result.putExtra("key", masterKey);
        setResult(RESULT_OK, result);

        // skip the intro from now on
        SharedPreferences prefs = this.getSharedPreferences("me.impy.aegis", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("passedIntro", true).apply();
        finish();
    }

    @Override
    public void onTaskFinished(SecretKey key) {
        if (key != null) {
            try {
                _passwordCipher = Slot.createCipher(key, Cipher.ENCRYPT_MODE);
            } catch (Exception e) {
                setException(e);
            }
        } else {
            setException(new NullPointerException());
        }
    }
}
