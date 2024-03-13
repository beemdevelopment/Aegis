package com.beemdevelopment.aegis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import com.beemdevelopment.aegis.ui.PanicResponderActivity;
import com.beemdevelopment.aegis.vault.VaultRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import dagger.hilt.android.testing.HiltAndroidTest;

@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
@SmallTest
public class PanicTriggerTest extends AegisTest {
    @Before
    public void before() {
        initEncryptedVault();
    }

    @Test
    public void testPanicTriggerDisabled() {
        assertFalse(_prefs.isPanicTriggerEnabled());
        assertTrue(_vaultManager.isVaultLoaded());
        launchPanic();
        assertTrue(_vaultManager.isVaultLoaded());
        _vaultManager.getVault();
        assertTrue(VaultRepository.fileExists(getApp()));
    }

    @Test
    public void testPanicTriggerEnabled() {
        _prefs.setIsPanicTriggerEnabled(true);
        assertTrue(_prefs.isPanicTriggerEnabled());
        assertTrue(_vaultManager.isVaultLoaded());
        launchPanic();
        assertFalse(_vaultManager.isVaultLoaded());
        assertThrows(IllegalStateException.class, () -> _vaultManager.getVault());
        assertFalse(VaultRepository.fileExists(getApp()));
    }

    private void launchPanic() {
        Intent intent = new Intent(PanicResponderActivity.PANIC_TRIGGER_ACTION);
        // we need to use the deprecated ActivityTestRule class because of https://github.com/android/android-test/issues/143
        ActivityTestRule<PanicResponderActivity> rule = new ActivityTestRule<>(PanicResponderActivity.class);
        rule.launchActivity(intent);
    }
}
