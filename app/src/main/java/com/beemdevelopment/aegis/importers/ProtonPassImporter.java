package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.net.Uri;

import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.GoogleAuthInfoException;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultGroup;
import com.topjohnwu.superuser.io.SuFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ProtonPassImporter extends DatabaseImporter {
    public ProtonPassImporter(Context context) {
        super(context);
    }

    @Override
    protected SuFile getAppPath() {
        throw new UnsupportedOperationException();
    }


    @Override
    protected State read(InputStream stream, boolean isInternal) throws DatabaseImporterException {
        // Unzip
        ZipInputStream zis = new ZipInputStream(stream);

        // Read file from zip
        ZipEntry zipEntry;
        try {
            while((zipEntry = zis.getNextEntry()) != null)
            {
                if(!zipEntry.getName().equals("Proton Pass/data.json"))
                {
                    continue;
                }

                // Read file
                BufferedReader br = new BufferedReader(new InputStreamReader(zis));
                StringBuilder json = new StringBuilder();
                String line;
                while((line = br.readLine()) != null){
                    json.append(line);
                }
                br.close();

                // Parse JSON
                JSONTokener tokener = new JSONTokener(json.toString());
                JSONObject jsonObject = new JSONObject(tokener);

                return new State(jsonObject);
            }
        }catch (IOException | JSONException e)
        {
            throw new DatabaseImporterException(e);
        }

        //Json not found
        throw new DatabaseImporterException("Invalid proton zip file");
    }

    public static class State extends DatabaseImporter.State {
        private JSONObject _jsonObject;

        private State(JSONObject jsonObject)
        {
            super(false);
            _jsonObject = jsonObject;
        }

        public Result convert() throws DatabaseImporterException {
            Result result = new Result();

            try {
                JSONObject vaults = this._jsonObject.getJSONObject("vaults");
                Iterator<String> keys = vaults.keys();

                // Iterate over vaults
                while (keys.hasNext())
                {
                    JSONObject vault = vaults.getJSONObject(keys.next());
                    JSONArray items = vault.getJSONArray("items");

                    //Create a new group
                    VaultGroup group = new VaultGroup(vault.getString("name"));
                    result.addGroup(group);

                    // Iterate over items on the vault
                    for(int j = 0; j < items.length(); j++)
                    {
                        JSONObject item = items.getJSONObject(j);

                        try{
                            VaultEntry entry = this.fromItem(item);

                            if(entry == null)
                            {
                                continue;
                            }

                            entry.addGroup(group.getUUID());
                            result.addEntry(entry);
                        }catch (JSONException | GoogleAuthInfoException e)
                        {
                            result.addError(new DatabaseImporterEntryException(e, "Can't import " + item.getString("itemId")));
                        }
                    }
                }

                return result;
            }catch (JSONException e)
            {
                throw new DatabaseImporterException(e);
            }
        }

        public VaultEntry fromItem(JSONObject item) throws JSONException, GoogleAuthInfoException {
            JSONObject data = item.getJSONObject("data");
            JSONObject metadata = data.getJSONObject("metadata");
            JSONObject content = data.getJSONObject("content");

            //Only login items
            if(!data.getString("type").equals("login"))
            {
                return null;
            }


            String uri = content.getString("totpUri");
            if(uri.isEmpty())
            {
                return null;
            }

            Uri toptURI = Uri.parse(content.getString("totpUri"));

            GoogleAuthInfo entry = GoogleAuthInfo.parseUri(toptURI);

            return new VaultEntry(entry.getOtpInfo(), metadata.getString("name"), entry.getIssuer());
        }
    }
}
