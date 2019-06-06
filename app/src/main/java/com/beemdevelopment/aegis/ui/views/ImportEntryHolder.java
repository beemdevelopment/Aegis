package com.beemdevelopment.aegis.ui.views;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.ui.models.ImportEntry;

import androidx.recyclerview.widget.RecyclerView;

public class ImportEntryHolder extends RecyclerView.ViewHolder implements ImportEntry.Listener {
    private TextView _issuer;
    private TextView _accountName;
    private CheckBox _checkbox;

    private ImportEntry _entry;

    public ImportEntryHolder(final View view) {
        super(view);

        _issuer = view.findViewById(R.id.profile_issuer);
        _accountName = view.findViewById(R.id.profile_account_name);
        _checkbox = view.findViewById(R.id.checkbox_import_entry);
        view.setOnClickListener(v -> _entry.setIsChecked(!_entry.isChecked()));
    }

    public void setData(ImportEntry entry) {
        _entry = entry;

        Context context = itemView.getContext();
        _issuer.setText(!entry.getIssuer().isEmpty() ? entry.getIssuer() : context.getString(R.string.unknown_issuer));
        _accountName.setText(!entry.getName().isEmpty() ? entry.getName() : context.getString(R.string.unknown_account_name));
        _checkbox.setChecked(entry.isChecked());
    }

    public ImportEntry getEntry() {
        return _entry;
    }

    @Override
    public void onCheckedChanged(boolean value) {
        _checkbox.setChecked(value);
    }
}
