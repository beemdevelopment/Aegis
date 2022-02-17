package com.beemdevelopment.aegis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import com.beemdevelopment.aegis.ui.PanicResponderActivity;
import com.beemdevelopment.aegis.vault.VaultManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PanicTriggerTest extends AegisTest {
    @Before
    public void before() {
        initVault();
    }

    @Test
    public void testPanicTriggerDisabled() {
        assertFalse(getApp().getPreferences().isPanicTriggerEnabled());
        launchPanic();
        assertFalse(getApp().isVaultLocked());
        assertNotNull(getApp().getVaultManager());
        assertTrue(VaultManager.fileExists(getApp()));
    }

    @Test
    public void testPanicTriggerEnabled() {
        getApp().getPreferences().setIsPanicTriggerEnabled(true);
        assertTrue(getApp().getPreferences().isPanicTriggerEnabled());
        launchPanic();
        assertTrue(getApp().isVaultLocked());
        assertNull(getApp().getVaultManager());
        assertFalse(VaultManager.fileExists(getApp()));
    }

    private void launchPanic() {
        Intent intent = new Intent(PanicResponderActivity.PANIC_TRIGGER_ACTION);
        // we need to use the deprecated ActivityTestRule class because of https://github.com/android/android-test/issues/143
        ActivityTestRule<PanicResponderActivity> rule = new ActivityTestRule<>(PanicResponderActivity.class);
        rule.launchActivity(intent);
    }
}
