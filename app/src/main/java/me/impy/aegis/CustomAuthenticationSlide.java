package me.impy.aegis;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.paolorotolo.appintro.ISlidePolicy;

public class CustomAuthenticationSlide extends Fragment implements ISlidePolicy {
    public static final int CRYPT_TYPE_INVALID = 0;
    public static final int CRYPT_TYPE_NONE = 1;
    public static final int CRYPT_TYPE_PASS = 2;
    public static final int CRYPT_TYPE_FINGER = 3;

    private RadioGroup buttonGroup;
    private int bgColor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_authentication_slide, container, false);
        final Context context = getContext();

        buttonGroup = (RadioGroup) view.findViewById(R.id.rg_authenticationMethod);
        buttonGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == -1) {
                    return;
                }

                int id;
                switch (checkedId) {
                    case R.id.rb_none:
                        id = CRYPT_TYPE_NONE;
                        break;
                    case R.id.rb_password:
                        id = CRYPT_TYPE_PASS;
                        break;
                    case R.id.rb_fingerprint:
                        id = CRYPT_TYPE_FINGER;
                        // TODO: remove this
                        group.clearCheck();
                        Toast.makeText(context, "Fingerprint is not supported yet", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        throw new RuntimeException();
                }
                Intent intent = getActivity().getIntent();
                intent.putExtra("cryptType", id);
            }
        });

        // only show the fingerprint option if the api version is new enough, permission is granted and a scanner is found
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED && fingerprintManager.isHardwareDetected()) {
                RadioButton button = (RadioButton) view.findViewById(R.id.rb_fingerprint);
                TextView text = (TextView) view.findViewById(R.id.text_rb_fingerprint);
                button.setVisibility(View.VISIBLE);
                text.setVisibility(View.VISIBLE);
            }
        }

        view.findViewById(R.id.main).setBackgroundColor(bgColor);
        return view;
    }

    public void setBgColor(int color) {
        bgColor = color;
    }

    @Override
    public boolean isPolicyRespected() {
        return buttonGroup.getCheckedRadioButtonId() != -1;
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {
        Snackbar snackbar = Snackbar.make(getView(), "Please select an authentication method", Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    /*@Override
    public int buttonsColor() {
        return R.color.colorAccent;
    }*/
}
