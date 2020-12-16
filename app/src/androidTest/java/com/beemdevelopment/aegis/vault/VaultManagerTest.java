package com.beemdevelopment.aegis.vault;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.beemdevelopment.aegis.AegisTest;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class VaultManagerTest extends AegisTest {
    @Before
    public void before() {
        initVault();
    }

    @Test
    public void testToggleEncryption() throws VaultManagerException {
        getVault().disableEncryption();
        assertFalse(getVault().isEncryptionEnabled());
        assertNull(getVault().getCredentials());

        VaultFileCredentials creds = generateCredentials();
        getVault().enableEncryption(creds);
        assertTrue(getVault().isEncryptionEnabled());
        assertNotNull(getVault().getCredentials());
        assertEquals(getVault().getCredentials().getSlots().findAll(PasswordSlot.class).size(), 1);
    }
}
