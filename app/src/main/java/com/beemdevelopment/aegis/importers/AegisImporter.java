package com.beemdevelopment.aegis.importers;

import android.content.Context;

import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.ui.tasks.PasswordSlotDecryptTask;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultFile;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultFileException;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.SlotList;
import com.topjohnwu.superuser.io.SuFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AegisImporter extends DatabaseImporter {

    public AegisImporter(Context context) {
        super(context);
    }

    @Override
    protected SuFile getAppPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public State read(InputStream stream, boolean isInternal) throws DatabaseImporterException {
        try {
            byte[] bytes = IOUtils.readAll(stream);
            VaultFile file = VaultFile.fromBytes(bytes);
            if (file.isEncrypted()) {
                return new EncryptedState(file);
            }
            return new DecryptedState(file.getContent());
        } catch (VaultFileException | IOException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class EncryptedState extends State {
        private VaultFile _file;

        private EncryptedState(VaultFile file) {
            super(true);
            _file = file;
        }

        public SlotList getSlots() {
            return _file.getHeader().getSlots();
        }

        public State decrypt(VaultFileCredentials creds) throws DatabaseImporterException {
            JSONObject obj;
            try {
                obj = _file.getContent(creds);
            } catch (VaultFileException e) {
                throw new DatabaseImporterException(e);
            }

            return new DecryptedState(obj);
        }

        public State decrypt(char[] password) throws DatabaseImporterException {
            List<PasswordSlot> slots = getSlots().findAll(PasswordSlot.class);
            PasswordSlotDecryptTask.Result result = PasswordSlotDecryptTask.decrypt(slots, password);
            VaultFileCredentials creds = new VaultFileCredentials(result.getKey(), getSlots());
            return decrypt(creds);
        }

        @Override
        public void decrypt(Context context, DecryptListener listener) {

        }
    }

    public static class DecryptedState extends State {
        private JSONObject _obj;

        private DecryptedState(JSONObject obj) {
            super(false);
            _obj = obj;
        }

        @Override
        public Result convert() throws DatabaseImporterException {
            Result result = new Result();

            try {
                JSONArray array = _obj.getJSONArray("entries");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject entryObj = array.getJSONObject(i);
                    try {
                        VaultEntry entry = convertEntry(entryObj);
                        result.addEntry(entry);
                    } catch (DatabaseImporterEntryException e) {
                        result.addError(e);
                    }
                }
            } catch (JSONException e) {
                throw new DatabaseImporterException(e);
            }

            return result;
        }

        private static VaultEntry convertEntry(JSONObject obj) throws DatabaseImporterEntryException {
            try {
                return VaultEntry.fromJson(obj);
            } catch (JSONException | OtpInfoException | EncodingException e) {
                throw new DatabaseImporterEntryException(e, obj.toString());
            }
        }
    }
}
