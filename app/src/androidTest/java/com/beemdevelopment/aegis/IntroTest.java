package com.beemdevelopment.aegis;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.not;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.viewpager2.widget.ViewPager2;

import com.beemdevelopment.aegis.rules.ScreenshotTestRule;
import com.beemdevelopment.aegis.ui.IntroActivity;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.vault.VaultRepository;
import com.beemdevelopment.aegis.vault.slots.BiometricSlot;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.SlotList;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import dagger.hilt.android.testing.HiltAndroidTest;

@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
@LargeTest
public class IntroTest extends AegisTest {
    private final ActivityScenarioRule<IntroActivity> _activityRule = new ActivityScenarioRule<>(IntroActivity.class);

    private ViewPager2IdlingResource _viewPager2IdlingResource;

    @Rule
    public final TestRule testRule = RuleChain.outerRule(_activityRule).around(new ScreenshotTestRule());

    @Before
    public void setUp() {
        Intents.init();

        _activityRule.getScenario().onActivity(activity -> {
            _viewPager2IdlingResource = new ViewPager2IdlingResource(activity.findViewById(R.id.pager), "viewPagerIdlingResource");
            IdlingRegistry.getInstance().register(_viewPager2IdlingResource);
        });
    }

    @After
    public void tearDown() {
        Intents.release();
        IdlingRegistry.getInstance().unregister(_viewPager2IdlingResource);
    }

    @Test
    public void testIntro_None() {
        assertFalse(_prefs.isIntroDone());
        ViewInteraction next = onView(withId(R.id.btnNext));
        ViewInteraction prev = onView(withId(R.id.btnPrevious));

        prev.check(matches(not(isDisplayed())));
        next.perform(click());
        onView(withId(R.id.rb_none)).perform(click());
        prev.perform(click());
        prev.check(matches(not(isDisplayed())));
        next.perform(click());
        next.perform(click());
        prev.check(matches(not(isDisplayed())));
        next.perform(click());

        VaultRepository vault = _vaultManager.getVault();
        assertFalse(vault.isEncryptionEnabled());
        assertNull(vault.getCredentials());
        assertTrue(_prefs.isIntroDone());
    }

    @Test
    public void testIntro_Password() {
        assertFalse(_prefs.isIntroDone());
        ViewInteraction next = onView(withId(R.id.btnNext));
        ViewInteraction prev = onView(withId(R.id.btnPrevious));

        prev.check(matches(not(isDisplayed())));
        next.perform(click());
        onView(withId(R.id.rb_password)).perform(click());
        prev.perform(click());
        prev.check(matches(not(isDisplayed())));
        next.perform(click());
        next.perform(click());
        onView(withId(R.id.text_password)).perform(typeText(VAULT_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.text_password_confirm)).perform(typeText(VAULT_PASSWORD + "1"), closeSoftKeyboard());
        next.perform(click());
        onView(withId(R.id.text_password_confirm)).perform(replaceText(VAULT_PASSWORD), closeSoftKeyboard());
        prev.perform(click());
        prev.perform(click());
        prev.check(matches(not(isDisplayed())));
        next.perform(click());
        next.perform(click());
        next.perform(click());
        next.perform(click());

        VaultRepository vault = _vaultManager.getVault();
        SlotList slots = vault.getCredentials().getSlots();
        assertTrue(vault.isEncryptionEnabled());
        assertTrue(slots.has(PasswordSlot.class));
        assertFalse(slots.has(BiometricSlot.class));
        assertTrue(_prefs.isIntroDone());
    }

    @Test
    public void testIntro_Import_Plain() {
        assertFalse(_prefs.isIntroDone());
        Uri uri = getResourceUri("aegis_plain.json");
        Intent resultData = new Intent();
        resultData.setData(uri);

        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);
        intending(not(isInternal())).respondWith(result);

        ViewInteraction next = onView(withId(R.id.btnNext));
        onView(withId(R.id.btnImport)).perform(click());
        next.perform(click());

        VaultRepository vault = _vaultManager.getVault();
        assertFalse(vault.isEncryptionEnabled());
        assertNull(vault.getCredentials());
        assertTrue(_prefs.isIntroDone());
    }

    @Test
    public void testIntro_Import_Encrypted() {
        assertFalse(_prefs.isIntroDone());
        Uri uri = getResourceUri("aegis_encrypted.json");
        Intent resultData = new Intent();
        resultData.setData(uri);

        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);
        intending(not(isInternal())).respondWith(result);

        ViewInteraction next = onView(withId(R.id.btnNext));
        onView(withId(R.id.btnImport)).perform(click());
        onView(withId(R.id.text_input)).perform(typeText(VAULT_PASSWORD), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).perform(click());
        next.perform(click());

        VaultRepository vault = _vaultManager.getVault();
        SlotList slots = vault.getCredentials().getSlots();
        assertTrue(vault.isEncryptionEnabled());
        assertTrue(slots.has(PasswordSlot.class));
        assertFalse(slots.has(BiometricSlot.class));
        assertTrue(_prefs.isIntroDone());
    }

    private Uri getResourceUri(String resourceName) {
        File targetFile = new File(getInstrumentation().getTargetContext().getExternalCacheDir(), resourceName);
        try (InputStream inStream = getClass().getResourceAsStream(resourceName);
             FileOutputStream outStream = new FileOutputStream(targetFile)) {
            IOUtils.copy(inStream, outStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Uri.fromFile(targetFile);
    }

    // Source: https://stackoverflow.com/a/32763454/12972657
    private static class ViewPager2IdlingResource implements IdlingResource {
        private final String _resName;
        private boolean _isIdle = true;
        private IdlingResource.ResourceCallback _resourceCallback = null;

        public ViewPager2IdlingResource(ViewPager2 viewPager, String resName) {
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageScrollStateChanged(int state) {
                    _isIdle = (state == ViewPager2.SCROLL_STATE_IDLE || state == ViewPager2.SCROLL_STATE_DRAGGING);
                    if (_isIdle && _resourceCallback != null) {
                        _resourceCallback.onTransitionToIdle();
                    }
                }
            });
            _resName = resName;
        }

        @Override
        public String getName() {
            return _resName;
        }

        @Override
        public boolean isIdleNow() {
            return _isIdle;
        }

        @Override
        public void registerIdleTransitionCallback(IdlingResource.ResourceCallback resourceCallback) {
            _resourceCallback = resourceCallback;
        }
    }
}
