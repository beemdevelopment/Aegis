package com.beemdevelopment.aegis;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.pressBack;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Hex;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.MotpInfo;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.otp.YandexInfo;
import com.beemdevelopment.aegis.rules.ScreenshotTestRule;
import com.beemdevelopment.aegis.ui.MainActivity;
import com.beemdevelopment.aegis.ui.views.EntryAdapter;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultRepository;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import dagger.hilt.android.testing.HiltAndroidTest;

@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
@LargeTest
public class OverallTest extends AegisTest {
    private static final String _groupName = "Test";

    private final ActivityScenarioRule<MainActivity> _activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public final TestRule testRule = RuleChain.outerRule(_activityRule).around(new ScreenshotTestRule());

    @Test
    public void testOverall() {
        ViewInteraction next = onView(withId(R.id.btnNext));
        next.perform(click());
        onView(withId(R.id.rb_password)).perform(click());
        next.perform(click());
        onView(withId(R.id.text_password)).perform(click()).perform(typeText(VAULT_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.text_password_confirm)).perform(typeText(VAULT_PASSWORD), closeSoftKeyboard());
        next.perform(click());
        onView(withId(R.id.btnNext)).perform(click());

        VaultRepository vault = _vaultManager.getVault();
        assertTrue(vault.isEncryptionEnabled());
        assertTrue(vault.getCredentials().getSlots().has(PasswordSlot.class));
        assertTrue(_prefs.isIntroDone());

        List<VaultEntry> entries = Arrays.asList(
                generateEntry(TotpInfo.class, "Frank", "Google"),
                generateEntry(HotpInfo.class, "John", "GitHub"),
                generateEntry(TotpInfo.class, "Alice", "Office 365"),
                generateEntry(SteamInfo.class, "Gaben", "Steam"),
                generateEntry(YandexInfo.class, "Ivan", "Yandex", 16),
                generateEntry(MotpInfo.class, "Jimmy McGill", "PfSense", 16)
        );
        for (VaultEntry entry : entries) {
            addEntry(entry);
        }

        List<VaultEntry> realEntries = new ArrayList<>(vault.getEntries());
        for (int i = 0; i < realEntries.size(); i++) {
            String message = String.format("%s != %s", realEntries.get(i).toJson().toString(), entries.get(i).toJson().toString());
            assertTrue(message, realEntries.get(i).equivalates(entries.get(i)));
        }

        for (int i = 0; i < 10; i++) {
            onView(withId(R.id.rvKeyProfiles)).perform(RecyclerViewActions.actionOnHolderItem(withOtpType(HotpInfo.class), clickChildViewWithId(R.id.buttonRefresh)));
        }

        AtomicBoolean isErrorCardShown = new AtomicBoolean(false);
        _activityRule.getScenario().onActivity(activity -> {
            isErrorCardShown.set(((EntryAdapter)((RecyclerView) activity.findViewById(R.id.rvKeyProfiles)).getAdapter()).isErrorCardShown());
        });

        int entryPosOffset = isErrorCardShown.get() ? 1 : 0;
        onView(withId(R.id.rvKeyProfiles)).perform(RecyclerViewActions.actionOnItemAtPosition(entryPosOffset + 0, longClick()));
        onView(withId(R.id.action_copy)).perform(click());

        onView(withId(R.id.rvKeyProfiles)).perform(RecyclerViewActions.actionOnItemAtPosition(entryPosOffset + 1, longClick()));
        onView(withId(R.id.action_edit)).perform(click());
        onView(withId(R.id.text_name)).perform(clearText(), typeText("Bob"), closeSoftKeyboard());
        onView(withId(R.id.text_group)).perform(click());
        onView(withId(R.id.addGroup)).inRoot(RootMatchers.isDialog()).perform(click());
        onView(withId(R.id.text_input)).perform(typeText(_groupName), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).perform(click());
        onView(withText(R.string.save)).perform(click());
        onView(isRoot()).perform(pressBack());
        onView(withId(android.R.id.button1)).perform(click());

        changeSort(R.string.sort_alphabetically_name);
        changeSort(R.string.sort_alphabetically_name_reverse);
        changeSort(R.string.sort_alphabetically);
        changeSort(R.string.sort_alphabetically_reverse);
        changeSort(R.string.sort_custom);

        changeGroupFilter(_groupName);
        changeGroupFilter(null);

        onView(withId(R.id.rvKeyProfiles)).perform(RecyclerViewActions.actionOnItemAtPosition(entryPosOffset + 2, longClick()));
        onView(withId(R.id.rvKeyProfiles)).perform(RecyclerViewActions.actionOnItemAtPosition(entryPosOffset + 3, click()));
        onView(withId(R.id.rvKeyProfiles)).perform(RecyclerViewActions.actionOnItemAtPosition(entryPosOffset + 4, click()));
        onView(withId(R.id.action_share_qr)).perform(click());
        onView(withId(R.id.btnNext)).perform(click()).perform(click()).perform(click());

        onView(withId(R.id.rvKeyProfiles)).perform(RecyclerViewActions.actionOnItemAtPosition(entryPosOffset + 0, longClick()));
        onView(allOf(isDescendantOfA(withClassName(containsString("ActionBarContextView"))), withClassName(containsString("OverflowMenuButton")))).perform(click());
        onView(withText(R.string.action_delete)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());

        openContextualActionModeOverflowMenu();
        onView(withText(R.string.lock)).perform(click());
        onView(withId(R.id.text_password)).perform(typeText(VAULT_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.button_decrypt)).perform(click());
        vault = _vaultManager.getVault();

        openContextualActionModeOverflowMenu();
        onView(withText(R.string.action_settings)).perform(click());
        onView(withId(androidx.preference.R.id.recycler_view)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_section_security_title)), click()));
        onView(withId(androidx.preference.R.id.recycler_view)).perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
        onView(withId(android.R.id.button1)).perform(click());

        assertFalse(vault.isEncryptionEnabled());
        assertNull(vault.getCredentials());

        onView(withId(androidx.preference.R.id.recycler_view)).perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
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

    private void changeGroupFilter(String text) {
        onView(withId(R.id.chip_group)).perform(click());
        if (text == null) {
            onView(withId(R.id.btnClear)).perform(click());
        } else {
            onView(withText(text)).perform(click());
            onView(isRoot()).perform(pressBack());
        }
    }

    private void addEntry(VaultEntry entry) {
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.fab_enter)).perform(click());

        onView(withId(R.id.accordian_header)).perform(scrollTo(), click());
        onView(withId(R.id.text_name)).perform(typeText(entry.getName()), closeSoftKeyboard());
        onView(withId(R.id.text_issuer)).perform(typeText(entry.getIssuer()), closeSoftKeyboard());

        if (entry.getInfo().getClass() != TotpInfo.class) {
            String otpType;
            if (entry.getInfo() instanceof HotpInfo) {
                otpType = "HOTP";
            } else if (entry.getInfo() instanceof SteamInfo) {
                otpType = "Steam";
            } else if (entry.getInfo() instanceof YandexInfo) {
                otpType = "Yandex";
            } else if (entry.getInfo() instanceof MotpInfo) {
                otpType = "MOTP";
            } else if (entry.getInfo() instanceof TotpInfo) {
                otpType = "TOTP";
            } else {
                throw new RuntimeException(String.format("Unexpected entry type: %s", entry.getInfo().getClass().getSimpleName()));
            }

            onView(withId(R.id.dropdown_type)).perform(scrollTo(), click());
            onView(withText(otpType)).inRoot(RootMatchers.isPlatformPopup()).perform(click());
        }

        String secret;
        if (Objects.equals(entry.getInfo().getTypeId(), MotpInfo.ID)) {
            secret = Hex.encode(entry.getInfo().getSecret());
        } else {
            secret = Base32.encode(entry.getInfo().getSecret());
        }

        onView(withId(R.id.text_secret)).perform(typeText(secret), closeSoftKeyboard());

        if (entry.getInfo() instanceof YandexInfo) {
            String pin = "123456";
            ((YandexInfo) entry.getInfo()).setPin(pin);
            onView(withId(R.id.text_pin)).perform(typeText(pin), closeSoftKeyboard());
        } else if (entry.getInfo() instanceof MotpInfo) {
            String pin = "1234";
            ((MotpInfo) entry.getInfo()).setPin(pin);
            onView(withId(R.id.text_pin)).perform(typeText(pin), closeSoftKeyboard());
        }

        onView(withId(R.id.action_save)).perform(click());
    }
}
