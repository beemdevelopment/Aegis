package com.beemdevelopment.aegis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;

@RunWith(RobolectricTestRunner.class)
public class PreferencesTest {
    private Context _context;
    private SharedPreferences _sharedPrefs;

    @Before
    public void setUp() {
        _context = ApplicationProvider.getApplicationContext();
        _sharedPrefs = PreferenceManager.getDefaultSharedPreferences(_context);
        _sharedPrefs.edit().clear().commit();
    }

    @Test
    public void testIsPasswordReminderNeeded() {
        long currTime = new Date().getTime();
        Preferences prefs = new Preferences(_context);

        // make sure that the password reminder is enabled by default
        assertNotEquals(prefs.getPasswordReminderFrequency(), PassReminderFreq.NEVER);

        // if the old preference is set to false, the frequency should be NEVER
        _sharedPrefs.edit().putBoolean("pref_password_reminder", false).apply();
        assertEquals(prefs.getPasswordReminderFrequency(), PassReminderFreq.NEVER);
        assertFalse(prefs.isPasswordReminderNeeded());

        // password reminders are never needed when the frequency is set to NEVER
        PassReminderFreq freq = PassReminderFreq.NEVER;
        prefs.setPasswordReminderFrequency(freq);
        assertFalse(prefs.isPasswordReminderNeeded());

        // test correct behavior when the frequency is set to something other than NEVER
        freq = PassReminderFreq.WEEKLY;
        prefs.setPasswordReminderFrequency(freq);
        assertFalse(prefs.isPasswordReminderNeeded(currTime));
        prefs.setPasswordReminderTimestamp(currTime - freq.getDurationMillis() + 1);
        assertFalse(prefs.isPasswordReminderNeeded(currTime));
        prefs.setPasswordReminderTimestamp(currTime - freq.getDurationMillis());
        assertTrue(prefs.isPasswordReminderNeeded(currTime));
        prefs.setPasswordReminderTimestamp(currTime - freq.getDurationMillis() - 1);
        assertTrue(prefs.isPasswordReminderNeeded(currTime));

        // a password reminder should no longer be needed if it's configured to be less frequent than before
        freq = PassReminderFreq.BIWEEKLY;
        prefs.setPasswordReminderFrequency(freq);
        assertFalse(prefs.isPasswordReminderNeeded(currTime));
    }

    @Test
    public void testCopyBehaviorMigration() {
        CopyBehavior[] copyBehaviors = {
                CopyBehavior.NEVER,
                CopyBehavior.SINGLETAP,
                CopyBehavior.DOUBLETAP
        };
        TapAction[] singleTapActions = {
                TapAction.NONE,
                TapAction.COPY,
                TapAction.NONE
        };
        TapAction[] doubleTapActions = {
                TapAction.NONE,
                TapAction.NONE,
                TapAction.COPY
        };

        for (int i = 0; i < copyBehaviors.length; i++) {
            _sharedPrefs.edit()
                    .clear()
                    .putInt("pref_current_copy_behavior", copyBehaviors[i].ordinal())
                    .commit();

            Preferences prefs = new Preferences(_context);

            assertEquals(singleTapActions[i], prefs.getSingleTapAction());
            assertEquals(doubleTapActions[i], prefs.getDoubleTapAction());
            assertFalse(_sharedPrefs.contains("pref_current_copy_behavior"));
        }
    }
}
