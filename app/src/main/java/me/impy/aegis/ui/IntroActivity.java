package me.impy.aegis.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.WindowManager;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import me.impy.aegis.Preferences;
import me.impy.aegis.R;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.db.DatabaseFileException;
import me.impy.aegis.db.DatabaseManagerException;
import me.impy.aegis.db.slots.FingerprintSlot;
import me.impy.aegis.db.slots.PasswordSlot;
import me.impy.aegis.db.slots.Slot;
import me.impy.aegis.db.slots.SlotList;
import me.impy.aegis.db.Database;
import me.impy.aegis.db.DatabaseFile;
import me.impy.aegis.db.DatabaseManager;
import me.impy.aegis.db.slots.SlotException;
import me.impy.aegis.ui.slides.CustomAuthenticatedSlide;
import me.impy.aegis.ui.slides.CustomAuthenticationSlide;
import me.impy.aegis.ui.tasks.DerivationTask;

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

    private Preferences _prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set FLAG_SECURE on the window of every IntroActivity
        _prefs = new Preferences(this);
        if (_prefs.isSecureScreenEnabled()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

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
        permSliderPage.setDescription("Aegis needs permission to use your camera in order to scan QR codes. " +
                "It also needs access to external storage to able to export the database.");
        permSliderPage.setImageDrawable(R.drawable.intro_scanner);
        permSliderPage.setBgColor(getResources().getColor(R.color.colorAccent));
        addSlide(AppIntroFragment.newInstance(permSliderPage));
        askForPermissions(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, 2);

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
            DerivationTask.Params params = new DerivationTask.Params(_passwordSlot, _authenticatedSlide.getPassword());
            new DerivationTask(this, this).execute(params);
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
        // wait for the key derivation background task
        if (cryptType != CustomAuthenticationSlide.CRYPT_TYPE_NONE &&
                (_passwordSlot == null || _passwordCipher == null)) {
            return;
        }

        // generate the master key
        MasterKey masterKey = null;
        if (cryptType != CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
            masterKey = MasterKey.generate();
        }

        SlotList slots = null;
        if (cryptType != CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
            // encrypt the master key with a key derived from the user's password
            // and add it to the list of slots
            if (_passwordSlot == null || _passwordCipher == null) {
                throw new RuntimeException();
            }
            try {
                _passwordSlot.setKey(masterKey, _passwordCipher);
                slots = new SlotList();
                slots.add(_passwordSlot);
                _databaseFile.setSlots(slots);
            } catch (SlotException e) {
                setException(e);
            }
        }

        if (cryptType == CustomAuthenticationSlide.CRYPT_TYPE_FINGER) {
            try {
                // encrypt the master key with the fingerprint key
                // and add it to the list of slots
                FingerprintSlot slot = _authenticatedSlide.getFingerSlot();
                Cipher cipher = _authenticatedSlide.getFingerCipher();
                slot.setKey(masterKey, cipher);
                slots.add(slot);
            } catch (SlotException e) {
                setException(e);
                return;
            }
        }

        // finally, save the database
        try {
            JSONObject obj = _database.serialize();
            if (cryptType == CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
                _databaseFile.setContent(obj);
            } else {
                _databaseFile.setContent(obj, masterKey);
            }
            DatabaseManager.save(getApplicationContext(), _databaseFile);
        } catch (DatabaseManagerException | DatabaseFileException e) {
            setException(e);
            return;
        }

        // send the master key back to the main activity
        Intent result = new Intent();
        result.putExtra("key", masterKey);
        setResult(RESULT_OK, result);

        // skip the intro from now on
        _prefs.setIntroDone(true);
        finish();
    }

    @Override
    public void onTaskFinished(SecretKey key) {
        if (key != null) {
            try {
                _passwordCipher = Slot.createEncryptCipher(key);
            } catch (SlotException e) {
                setException(e);
            }
        } else {
            setException(new NullPointerException());
        }
    }
}
