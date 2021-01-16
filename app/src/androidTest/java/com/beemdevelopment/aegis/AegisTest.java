package com.beemdevelopment.aegis;

import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.platform.app.InstrumentationRegistry;

import com.beemdevelopment.aegis.crypto.CryptoUtils;
import com.beemdevelopment.aegis.crypto.SCryptParameters;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.vault.Vault;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.beemdevelopment.aegis.vault.VaultManagerException;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.SlotException;

import org.hamcrest.Matcher;

import java.lang.reflect.InvocationTargetException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public abstract class AegisTest {
    public static final String VAULT_PASSWORD = "test";

    protected AegisApplication getApp() {
        return (AegisApplication) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
    }

    protected VaultManager getVault() {
        return getApp().getVaultManager();
    }

    protected VaultManager initVault() {
        VaultFileCredentials creds = generateCredentials();
        VaultManager vault = getApp().initVaultManager(new Vault(), creds);
        try {
            vault.save(false);
        } catch (VaultManagerException e) {
            throw new RuntimeException(e);
        }

        getApp().getPreferences().setIntroDone(true);
        return vault;
    }

    protected VaultFileCredentials generateCredentials() {
        PasswordSlot slot = new PasswordSlot();
        byte[] salt = CryptoUtils.generateSalt();
        SCryptParameters scryptParams = new SCryptParameters(
                CryptoUtils.CRYPTO_SCRYPT_N,
                CryptoUtils.CRYPTO_SCRYPT_r,
                CryptoUtils.CRYPTO_SCRYPT_p,
                salt
        );

        VaultFileCredentials creds = new VaultFileCredentials();
        try {
            SecretKey key = slot.deriveKey(VAULT_PASSWORD.toCharArray(), scryptParams);
            slot.setKey(creds.getKey(), CryptoUtils.createEncryptCipher(key));
        } catch (NoSuchAlgorithmException
                | InvalidKeyException
                | InvalidAlgorithmParameterException
                | NoSuchPaddingException
                | SlotException e) {
            throw new RuntimeException(e);
        }

        creds.getSlots().add(slot);
        return creds;
    }

    protected static <T extends OtpInfo> VaultEntry generateEntry(Class<T> type, String name, String issuer) {
        byte[] secret = CryptoUtils.generateRandomBytes(20);

        OtpInfo info;
        try {
            info = type.getConstructor(byte[].class).newInstance(secret);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        return new VaultEntry(info, name, issuer);
    }

    // source: https://stackoverflow.com/a/30338665
    protected static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                v.performClick();
            }
        };
    }
}
