package com.beemdevelopment.aegis;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.TestCase.assertTrue;

import android.content.Intent;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.ui.MainActivity;
import com.beemdevelopment.aegis.vault.VaultEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import dagger.hilt.android.testing.HiltAndroidTest;

@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
@LargeTest
public class DeepLinkTest extends AegisTest {
    @Before
    public void before() {
        initEmptyEncryptedVault();
    }

    @Test
    public void testDeepLinkIntent() {
        VaultEntry entry = generateEntry(TotpInfo.class, "Bob", "Google");
        GoogleAuthInfo info = new GoogleAuthInfo(entry.getInfo(), entry.getName(), entry.getIssuer());
        launch(info.getUri());

        onView(withId(R.id.action_save)).perform(click());

        VaultEntry createdEntry = (VaultEntry) _vaultManager.getVault().getEntries().toArray()[0];
        assertTrue(createdEntry.equivalates(entry));
    }

    @Test
    public void testDeepLinkIntent_Empty() {
        launch(null);
    }

    @Test
    public void testDeepLinkIntent_Bad() {
        launch(Uri.parse("otpauth://bad"));
        onView(withId(android.R.id.button1)).perform(click());
    }

    @SuppressWarnings("deprecation")
    private void launch(Uri uri) {
        Intent intent = new Intent(getApp(), MainActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);

        // we need to use the deprecated ActivityTestRule class because of https://github.com/android/android-test/issues/143
        ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(MainActivity.class);
        rule.launchActivity(intent);
    }
}
