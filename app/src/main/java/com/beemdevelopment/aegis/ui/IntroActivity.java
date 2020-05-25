package com.beemdevelopment.aegis.ui;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.fragment.app.Fragment;

import com.beemdevelopment.aegis.AegisApplication;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.slides.CustomAuthenticatedSlide;
import com.beemdevelopment.aegis.ui.slides.CustomAuthenticationSlide;
import com.beemdevelopment.aegis.vault.Vault;
import com.beemdevelopment.aegis.vault.VaultFile;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultFileException;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.beemdevelopment.aegis.vault.VaultManagerException;
import com.github.appintro.AppIntro2;
import com.github.appintro.AppIntroFragment;
import com.github.appintro.model.SliderPage;

import org.json.JSONObject;

public class IntroActivity extends AppIntro2 {
    private CustomAuthenticatedSlide _authenticatedSlide;
    private CustomAuthenticationSlide _authenticationSlide;
    private Fragment _endSlide;

    private AegisApplication _app;
    private Preferences _prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _app = (AegisApplication) getApplication();
        // set FLAG_SECURE on the window of every IntroActivity
        _prefs = new Preferences(this);
        if (_prefs.isSecureScreenEnabled()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        setWizardMode(true);
        setSkipButtonEnabled(false);
        showStatusBar(true);
        setSystemBackButtonLocked(true);
        setBarColor(getResources().getColor(R.color.colorPrimary));

        SliderPage homeSliderPage = new SliderPage();
        homeSliderPage.setTitle(getString(R.string.welcome));
        homeSliderPage.setImageDrawable(R.drawable.app_icon);
        homeSliderPage.setTitleColor(getResources().getColor(R.color.primary_text_dark));
        homeSliderPage.setDescription(getString(R.string.app_description));
        homeSliderPage.setDescriptionColor(getResources().getColor(R.color.primary_text_dark));
        homeSliderPage.setBackgroundColor(getResources().getColor(R.color.colorSecondary));
        addSlide(AppIntroFragment.newInstance(homeSliderPage));

        _authenticationSlide = new CustomAuthenticationSlide();
        _authenticationSlide.setBgColor(getResources().getColor(R.color.colorSecondary));
        addSlide(_authenticationSlide);
        _authenticatedSlide = new CustomAuthenticatedSlide();
        _authenticatedSlide.setBgColor(getResources().getColor(R.color.colorSecondary));
        addSlide(_authenticatedSlide);

        SliderPage endSliderPage = new SliderPage();
        endSliderPage.setTitle(getString(R.string.setup_completed));
        endSliderPage.setDescription(getString(R.string.setup_completed_description));
        endSliderPage.setImageDrawable(R.drawable.app_icon);
        endSliderPage.setBackgroundColor(getResources().getColor(R.color.colorSecondary));
        _endSlide = AppIntroFragment.newInstance(endSliderPage);
        addSlide(_endSlide);
    }

    @Override
    public void onSlideChanged(Fragment oldFragment, Fragment newFragment) {
        if (oldFragment == _authenticationSlide && newFragment != _endSlide) {
            // skip to the last slide if no encryption will be used
            int cryptType = getIntent().getIntExtra("cryptType", CustomAuthenticationSlide.CRYPT_TYPE_INVALID);
            if (cryptType == CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
                // TODO: no magic indices
                goToNextSlide(false);
            }
        }

        setSwipeLock(true);
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        int cryptType = _authenticatedSlide.getCryptType();
        VaultFileCredentials creds = _authenticatedSlide.getCredentials();

        Vault vault = new Vault();
        VaultFile vaultFile = new VaultFile();
        try {
            JSONObject obj = vault.toJson();
            if (cryptType == CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
                vaultFile.setContent(obj);
            } else {
                vaultFile.setContent(obj, creds);
            }

            VaultManager.save(getApplicationContext(), vaultFile);
        } catch (VaultManagerException | VaultFileException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.vault_init_error, e);
            return;
        }

        if (cryptType == CustomAuthenticationSlide.CRYPT_TYPE_NONE) {
            _app.initVaultManager(vault, null);
        } else {
            _app.initVaultManager(vault, creds);
        }

        // skip the intro from now on
        _prefs.setIntroDone(true);

        setResult(RESULT_OK);
        finish();
    }

    public void goToNextSlide() {
        super.goToNextSlide(false);
    }
}
