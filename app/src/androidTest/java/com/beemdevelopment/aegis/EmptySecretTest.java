package com.beemdevelopment.aegis;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.rules.ScreenshotTestRule;
import com.beemdevelopment.aegis.ui.MainActivity;
import com.beemdevelopment.aegis.vault.VaultEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import dagger.hilt.android.testing.HiltAndroidTest;

@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
@SmallTest
public class EmptySecretTest extends AegisTest {
    private ActivityScenario<MainActivity> _scenario;

    @Before
    public void before() throws OtpInfoException {
        initEmptyPlainVault();
        _vaultManager.getVault().addEntry(new VaultEntry(new TotpInfo(new byte[0])));

        _scenario = ActivityScenario.launch(MainActivity.class);
    }

    @After
    public void after() {
        _scenario.close();
    }

    @Test
    public void testVaultEntryEmptySecret() {
        onView(withId(R.id.rvKeyProfiles)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.error_all_caps)), click()));
    }
}
