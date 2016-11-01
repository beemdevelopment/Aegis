package me.impy.aegis;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

public class EditProfileBottomSheetdialog extends BottomSheetDialogFragment {
    LinearLayout copyLayout;

    public static EditProfileBottomSheetdialog getInstance() {
        return new EditProfileBottomSheetdialog();
    }

/*    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_edit_profile, null);
        dialog.setContentView(contentView);

        copyLayout = (LinearLayout)contentView.findViewById(R.id.copy_button);
    }*/

    public LinearLayout GetCopyLayout()
    {
        return copyLayout;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_edit_profile, container, false);
    }
}