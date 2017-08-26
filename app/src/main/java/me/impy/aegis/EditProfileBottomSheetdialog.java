package me.impy.aegis;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class EditProfileBottomSheetdialog extends BottomSheetDialogFragment {
    LinearLayout _copyLayout;

    public static EditProfileBottomSheetdialog getInstance() {
        return new EditProfileBottomSheetdialog();
    }

    public LinearLayout GetCopyLayout()
    {
        return _copyLayout;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_edit_profile, container, false);
    }
}
