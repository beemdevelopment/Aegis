package com.beemdevelopment.aegis.ui.views;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.models.ImportEntry;

import androidx.recyclerview.widget.RecyclerView;

public class ImportEntryHolder extends RecyclerView.ViewHolder implements ImportEntry.Listener {
    private TextView _issuer;
    private TextView _accountName;
    private CheckBox _checkbox;

    private ImportEntry _data;

    public ImportEntryHolder(final View view) {
        super(view);

        _issuer = view.findViewById(R.id.profile_issuer);
        _accountName = view.findViewById(R.id.profile_account_name);
        _checkbox = view.findViewById(R.id.checkbox_import_entry);
        view.setOnClickListener(v -> _data.setIsChecked(!_data.isChecked()));
    }

    public void setData(ImportEntry data) {
        _data = data;

        Context context = itemView.getContext();
        _issuer.setText(!_data.getEntry().getIssuer().isEmpty() ? _data.getEntry().getIssuer() : context.getString(R.string.unknown_issuer));
        _accountName.setText(!_data.getEntry().getName().isEmpty() ? _data.getEntry().getName() : context.getString(R.string.unknown_account_name));
        _checkbox.setChecked(_data.isChecked());
    }

    public ImportEntry getData() {
        return _data;
    }

    @Override
    public void onCheckedChanged(boolean value) {
        _checkbox.setChecked(value);
    }
}
