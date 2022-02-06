package com.beemdevelopment.aegis;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.not;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.beemdevelopment.aegis.ui.IntroActivity;
import com.beemdevelopment.aegis.vault.VaultRepository;
import com.beemdevelopment.aegis.vault.slots.BiometricSlot;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.SlotList;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import dagger.hilt.android.testing.HiltAndroidTest;

@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
@LargeTest
public class IntroTest extends AegisTest {
    @Rule
    public final ActivityScenarioRule<IntroActivity> activityRule = new ActivityScenarioRule<>(IntroActivity.class);

    @Test
    public void doIntro_None() {
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
        next.perform(click());

        VaultRepository vault = _vaultManager.getVault();
        assertFalse(vault.isEncryptionEnabled());
        assertNull(vault.getCredentials());
    }

    @Test
    public void doIntro_Password() {
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
    }
}
