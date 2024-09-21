package com.beemdevelopment.aegis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;

@RunWith(RobolectricTestRunner.class)
public class PreferencesTest {
    @Test
    public void testIsPasswordReminderNeeded() {
        long currTime = new Date().getTime();
        Context context = ApplicationProvider.getApplicationContext();
        Preferences prefs = new Preferences(context);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        // make sure that the password reminder is enabled by default
        assertNotEquals(prefs.getPasswordReminderFrequency(), PassReminderFreq.NEVER);

        // if the old preference is set to false, the frequency should be NEVER
        sharedPrefs.edit().putBoolean("pref_password_reminder", false).apply();
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
}
