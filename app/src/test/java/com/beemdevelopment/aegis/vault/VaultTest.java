package com.beemdevelopment.aegis.vault;

import static org.junit.Assert.assertEquals;

import com.beemdevelopment.aegis.util.IOUtils;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public class VaultTest {
    @Test
    public void testGroupConversion()
            throws IOException, VaultFileException, VaultException {
        Vault vault;
        try (InputStream inStream = getClass().getResourceAsStream("aegis_plain_grouped_v2.json")) {
            byte[] bytes = IOUtils.readAll(inStream);
            VaultFile file = VaultFile.fromBytes(bytes);
            vault = Vault.fromJson(file.getContent());
        }

        checkGroups(vault);

        // After saving to and loading from the new format, the same checks should still pass
        vault = Vault.fromJson(vault.toJson());
        checkGroups(vault);
    }

    private static void checkGroups(Vault vault) {
        // No old groups should be present anymore
        assertEquals(0, vault.getEntries().getValues().stream()
                .filter(e -> e.getOldGroup() != null)
                .count());

        // New groups should have been created, and groups with the same name
        // should have been merged into a single group
        assertEquals(2, vault.getGroups().getValues().size());

        // Only one group with name group1
        List<VaultGroup> foundGroups = vault.getGroups().getValues().stream()
                .filter(g -> g.getName().equals("group1"))
                .collect(Collectors.toList());
        assertEquals(1, foundGroups.size());
        VaultGroup group1 = foundGroups.get(0);

        // Only one group with name group2
        foundGroups = vault.getGroups().getValues().stream()
                .filter(g -> g.getName().equals("group2"))
                .collect(Collectors.toList());
        assertEquals(1, foundGroups.size());
        VaultGroup group2 = foundGroups.get(0);

        // Two entries in group1
        assertEquals(2, vault.getEntries().getValues().stream()
                .filter(e -> e.getGroups().contains(group1.getUUID()))
                .count());

        // One entry in group2
        assertEquals(1, vault.getEntries().getValues().stream()
                .filter(e -> e.getGroups().contains(group2.getUUID()))
                .count());

        // Rest of the entries in no groups
        assertEquals(vault.getEntries().getValues().size() - 3, vault.getEntries().getValues().stream()
                .filter(e -> e.getGroups().isEmpty())
                .count());
    }
}
