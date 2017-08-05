package me.impy.aegis;

import android.app.Activity;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.app.DialogFragment;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.mattprecious.swirl.SwirlView;

import agency.tango.materialintroscreen.SlideFragment;
import me.impy.aegis.finger.FingerprintAuthenticationDialogFragment;
import me.impy.aegis.finger.SetFingerprintAuthenticationDialog;

public class CustomAuthenticationSlide extends SlideFragment implements SetFingerprintAuthenticationDialog.InterfaceCommunicator{
    private CheckBox checkBox;
    private RadioButton fingerprintRadioButton;
    private RadioGroup authenticationMethodRadioGroup;

    public static final int DIALOG_FRAGMENT = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_authentication_slide, container, false);
        final CustomAuthenticationSlide caller = this;

        fingerprintRadioButton = (RadioButton) view.findViewById(R.id.rb_fingerprint);
        fingerprintRadioButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                SetFingerprintAuthenticationDialog fragment = new SetFingerprintAuthenticationDialog();
                //fragment.setCryptoObject(new FingerprintManager.CryptoObject(cipher));
                fragment.setStage(SetFingerprintAuthenticationDialog.Stage.FINGERPRINT);
                fragment.setCaller(caller);
                //fragment.setAction(action);
                fragment.show(getActivity().getFragmentManager(), "dialog");
            }
        });

        authenticationMethodRadioGroup = (RadioGroup) view.findViewById(R.id.rg_authenticationMethod);
        return view;
    }

    @Override
    public int backgroundColor() {
        return R.color.colorHeaderSuccess;
    }

    @Override
    public int buttonsColor() {
        return R.color.colorAccent;
    }

    @Override
    public boolean canMoveFurther() {
        return authenticationMethodRadioGroup.getCheckedRadioButtonId() != -1;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }


    @Override
    public String cantMoveFurtherErrorMessage() {
        return "Please select an authentication method";
        //return getString(R.string.error_message);
    }

    @Override
    public void sendRequestCode(int code) {
        if (code == 1) {

        } else if (code == 0){
            authenticationMethodRadioGroup.clearCheck();
        }
    }
}