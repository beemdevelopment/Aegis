package me.impy.aegis;

import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;

import com.mattprecious.swirl.SwirlView;

import javax.crypto.Cipher;

import agency.tango.materialintroscreen.SlideFragment;
import me.impy.aegis.finger.FingerprintAuthenticationDialogFragment;

public class CustomAuthenticatedSlide extends SlideFragment {
    private CheckBox checkBox;
    private RadioButton passwordRadioButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_authenticated_slide, container, false);
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
        return true; //checkBox.isChecked();
    }

    public void onAuthenticated(FingerprintAuthenticationDialogFragment.Action action, FingerprintManager.CryptoObject obj) {

    }

    @Override
    public String cantMoveFurtherErrorMessage() {
        return "Ja bijna vriend";
        //return getString(R.string.error_message);
    }
}
