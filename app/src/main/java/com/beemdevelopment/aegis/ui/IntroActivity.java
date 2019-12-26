package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.fragment.app.Fragment;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.vault.Vault;
import com.beemdevelopment.aegis.vault.VaultFile;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultFileException;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.beemdevelopment.aegis.vault.VaultManagerException;
import com.beemdevelopment.aegis.vault.slots.BiometricSlot;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.Slot;
import com.beemdevelopment.aegis.vault.slots.SlotException;
import com.beemdevelopment.aegis.ui.slides.CustomAuthenticatedSlide;
import com.beemdevelopment.aegis.ui.slides.CustomAuthenticationSlide;
import com.beemdevelopment.aegis.ui.tasks.DerivationTask;
import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class IntroActivity extends AppIntro2 implements DerivationTask.Callback {
    public static final int RESULT_OK = 0;
    public static final int RESULT_EXCEPTION = 1;

    private CustomAuthenticatedSlide _authenticatedSlide;
    private CustomAuthenticationSlide _authenticationSlide;
    private Fragment _endSlide;

    private Vault _vault;
    private VaultFile _vaultFile;
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

        setWizardMode(true);
        showSkipButton(false);
        pager.setPagingEnabled(false);
        //showPagerIndicator(false);
        setGoBackLock(true);
        setBarColor(getResources().getColor(R.color.colorPrimary));

        SliderPage homeSliderPage = new SliderPage();
        homeSliderPage.setTitle(getString(R.string.welcome));
        homeSliderPage.setImageDrawable(R.drawable.app_icon);
        homeSliderPage.setTitleColor(getResources().getColor(R.color.primary_text_dark));
        homeSliderPage.setDescription(getString(R.string.app_description));
        homeSliderPage.setDescColor(getResources().getColor(R.color.primary_text_dark));
        homeSliderPage.setBgColor(getResources().getColor(R.color.colorSecondary));
        addSlide(AppIntroFragment.newInstance(homeSliderPage));

        _authenticationSlide = new CustomAuthenticationSlide();
        _authenticationSlide.setBgColor(getResources().getColor(R.color.colorSecondary));
        //_authenticationSlide.setDescColor(getResources().getColor(R.color.primary_text_dark));
        addSlide(_authenticationSlide);
        _authenticatedSlide = new CustomAuthenticatedSlide();
        _authenticatedSlide.setBgColor(getResources().getColor(R.color.colorSecondary));
        addSlide(_authenticatedSlide);

        SliderPage endSliderPage = new SliderPage();
        endSliderPage.setTitle(getString(R.string.setup_completed));
        endSliderPage.setDescription(getString(R.string.setup_completed_description));
        endSliderPage.setImageDrawable(R.drawable.app_icon);
        endSliderPage.setBgColor(getResources().getColor(R.color.colorSecondary));
        _endSlide = AppIntroFragment.newInstance(endSliderPage);
        addSlide(_endSlide);

        _vault = new Vault();
        _vaultFile = new VaultFile();
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
                getPager().setCurrentItem(4);
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
        VaultFileCredentials creds = null;
        if (cryptType != CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
            creds = new VaultFileCredentials();
        }

        if (cryptType != CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
            // encrypt the master key with a key derived from the user's password
            // and add it to the list of slots
            if (_passwordSlot == null || _passwordCipher == null) {
                throw new RuntimeException();
            }
            try {
                _passwordSlot.setKey(creds.getKey(), _passwordCipher);
                creds.getSlots().add(_passwordSlot);
            } catch (SlotException e) {
                setException(e);
            }
        }

        if (cryptType == CustomAuthenticationSlide.CRYPT_TYPE_BIOMETRIC) {
            BiometricSlot slot = _authenticatedSlide.getBiometricSlot();
            try {
                slot.setKey(creds.getKey(), _authenticatedSlide.getBiometriCipher());
            } catch (SlotException e) {
                setException(e);
            }
            creds.getSlots().add(slot);
        }

        // finally, save the vault
        try {
            JSONObject obj = _vault.toJson();
            if (cryptType == CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
                _vaultFile.setContent(obj);
            } else {
                _vaultFile.setContent(obj, creds);
            }
            VaultManager.save(getApplicationContext(), _vaultFile);
        } catch (VaultManagerException | VaultFileException e) {
            setException(e);
            return;
        }

        // send the master key back to the main activity
        Intent result = new Intent();
        result.putExtra("creds", creds);
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
