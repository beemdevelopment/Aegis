package com.beemdevelopment.aegis;

import androidx.annotation.IdRes;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.AmbiguousViewMatcherException;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.ui.MainActivity;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.pressBack;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.anything;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OverallTest extends AegisTest {
    private static final String _groupName = "Test";

    @Rule
    public final ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void doOverallTest() {
        ViewInteraction next = onView(withId(R.id.btnNext));
        next.perform(click());
        onView(withId(R.id.rb_password)).perform(click());
        next.perform(click());
        onView(withId(R.id.text_password)).perform(typeText(VAULT_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.text_password_confirm)).perform(typeText(VAULT_PASSWORD), closeSoftKeyboard());
        next.perform(click());
        onView(withId(R.id.btnNext)).perform(click());

        VaultManager vault = getVault();
        assertTrue(vault.isEncryptionEnabled());
        assertTrue(vault.getCredentials().getSlots().has(PasswordSlot.class));

        List<VaultEntry> entries = Arrays.asList(
                generateEntry(TotpInfo.class, "Frank", "Google"),
                generateEntry(HotpInfo.class, "John", "GitHub"),
                generateEntry(TotpInfo.class, "Alice", "Office 365"),
                generateEntry(SteamInfo.class, "Gaben", "Steam")
        );
        for (VaultEntry entry : entries) {
            addEntry(entry);
        }

        List<VaultEntry> realEntries = new ArrayList<>(vault.getEntries());
        for (int i = 0; i < realEntries.size(); i++) {
            assertTrue(realEntries.get(i).equivalates(entries.get(i)));
        }

        for (int i = 0; i < 10; i++) {
            onView(withId(R.id.rvKeyProfiles)).perform(RecyclerViewActions.actionOnItemAtPosition(1, clickChildViewWithId(R.id.buttonRefresh)));
        }

        onView(withId(R.id.rvKeyProfiles)).perform(RecyclerViewActions.actionOnItemAtPosition(0, longClick()));
        onView(withId(R.id.action_copy)).perform(click());

        onView(withId(R.id.rvKeyProfiles)).perform(RecyclerViewActions.actionOnItemAtPosition(1, longClick()));
        onView(withId(R.id.action_edit)).perform(click());
        onView(withId(R.id.text_name)).perform(clearText(), typeText("Bob"), closeSoftKeyboard());
        onView(withId(R.id.dropdown_group)).perform(click());
        onData(anything()).atPosition(1).perform(click());
        onView(withId(R.id.text_input)).perform(typeText(_groupName), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).perform(click());
        onView(isRoot()).perform(pressBack());
        onView(withId(android.R.id.button1)).perform(click());

        changeSort(R.string.sort_alphabetically_name);
        changeSort(R.string.sort_alphabetically_name_reverse);
        changeSort(R.string.sort_alphabetically);
        changeSort(R.string.sort_alphabetically_reverse);
        changeSort(R.string.sort_custom);

        changeFilter(_groupName);
        changeFilter(R.string.filter_ungrouped);
        changeFilter(R.string.all);

        onView(withId(R.id.rvKeyProfiles)).perform(RecyclerViewActions.actionOnItemAtPosition(1, longClick()));
        onView(withId(R.id.rvKeyProfiles)).perform(RecyclerViewActions.actionOnItemAtPosition(2, click()));
        onView(withId(R.id.rvKeyProfiles)).perform(RecyclerViewActions.actionOnItemAtPosition(3, click()));
        onView(withId(R.id.action_share_qr)).perform(click());
        onView(withId(R.id.btnNext)).perform(click()).perform(click()).perform(click());

        onView(withId(R.id.rvKeyProfiles)).perform(RecyclerViewActions.actionOnItemAtPosition(2, longClick()));
        onView(withId(R.id.action_delete)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());

        openContextualActionModeOverflowMenu();
        onView(withText(R.string.lock)).perform(click());
        onView(withId(R.id.text_password)).perform(typeText(VAULT_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.button_decrypt)).perform(click());
        vault = getVault();

        openContextualActionModeOverflowMenu();
        onView(withText(R.string.action_settings)).perform(click());
        onView(withId(androidx.preference.R.id.recycler_view)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_security_group_title)), click()));
        onView(withId(androidx.preference.R.id.recycler_view)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_encryption_title)), click()));
        onView(withId(android.R.id.button1)).perform(click());

        assertFalse(vault.isEncryptionEnabled());
        assertNull(vault.getCredentials());

        onView(withId(androidx.preference.R.id.recycler_view)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_encryption_title)), click()));
        onView(withId(R.id.text_password)).perform(typeText(VAULT_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.text_password_confirm)).perform(typeText(VAULT_PASSWORD), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).perform(click());

        assertTrue(vault.isEncryptionEnabled());
        assertTrue(vault.getCredentials().getSlots().has(PasswordSlot.class));
    }

    private void changeSort(@IdRes int resId) {
        onView(withId(R.id.action_sort)).perform(click());
        onView(withText(resId)).perform(click());
    }

    private void changeFilter(String text) {
        openContextualActionModeOverflowMenu();
        onView(withText(R.string.filter)).perform(click());
        onView(withText(text)).perform(click());
    }

    private void changeFilter(@IdRes int resId) {
        changeFilter(ApplicationProvider.getApplicationContext().getString(resId));
    }

    private void addEntry(VaultEntry entry) {
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.fab_enter)).perform(click());

        onView(withId(R.id.text_name)).perform(typeText(entry.getName()), closeSoftKeyboard());
        onView(withId(R.id.text_issuer)).perform(typeText(entry.getIssuer()), closeSoftKeyboard());

        if (entry.getInfo().getClass() != TotpInfo.class) {
            int i = entry.getInfo() instanceof HotpInfo ? 1 : 2;
            try {
                onView(withId(R.id.dropdown_type)).perform(click());
                onData(anything()).atPosition(i).perform(click());
            } catch (AmbiguousViewMatcherException e) {
                // for some reason, clicking twice is sometimes necessary, otherwise the test fails on the next line
                onView(withId(R.id.dropdown_type)).perform(click());
                onData(anything()).atPosition(i).perform(click());
            }
            if (entry.getInfo() instanceof HotpInfo) {
                onView(withId(R.id.text_period_counter)).perform(typeText("0"), closeSoftKeyboard());
            }
            if (entry.getInfo() instanceof SteamInfo) {
                onView(withId(R.id.text_digits)).perform(clearText(), typeText("5"), closeSoftKeyboard());
            }
        }

        String secret = Base32.encode(entry.getInfo().getSecret());
        onView(withId(R.id.text_secret)).perform(typeText(secret), closeSoftKeyboard());

        onView(withId(R.id.action_save)).perform(click());
    }
}
