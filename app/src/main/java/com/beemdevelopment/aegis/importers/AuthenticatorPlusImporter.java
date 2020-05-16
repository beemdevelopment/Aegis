package com.beemdevelopment.aegis.importers;

import android.content.Context;

import com.beemdevelopment.aegis.ui.Dialogs;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.LocalFileHeader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class AuthenticatorPlusImporter extends DatabaseImporter {
    private static final String FILENAME = "Accounts.txt";

    public AuthenticatorPlusImporter(Context context) {
        super(context);
    }

    @Override
    protected String getAppPkgName() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getAppSubPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public State read(FileReader reader) throws DatabaseImporterException {
        try {
            return new EncryptedState(reader.readAll());
        } catch (IOException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class EncryptedState extends DatabaseImporter.State {
        private byte[] _data;

        private EncryptedState(byte[] data) {
            super(true);
            _data = data;
        }

        @Override
        public void decrypt(Context context, DecryptListener listener) {
            Dialogs.showPasswordInputDialog(context, password -> {
                try (ByteArrayInputStream inStream = new ByteArrayInputStream(_data);
                     ZipInputStream zipStream = new ZipInputStream(inStream, password)) {
                    LocalFileHeader header;
                    while ((header = zipStream.getNextEntry()) != null) {
                        File file = new File(header.getFileName());
                        if (file.getName().equals(FILENAME)) {
                            FileReader reader = new FileReader(zipStream);
                            GoogleAuthUriImporter importer = new GoogleAuthUriImporter(context);
                            GoogleAuthUriImporter.State state = importer.read(reader);
                            listener.onStateDecrypted(state);
                            return;
                        }
                    }

                    throw new FileNotFoundException(FILENAME);
                } catch (IOException | DatabaseImporterException e) {
                    listener.onError(e);
                }
            });
        }
    }
}
