package me.impy.aegis;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import agency.tango.materialintroscreen.SlideFragment;

public class CustomAuthenticationSlide extends SlideFragment {
    private RadioGroup buttonGroup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_authentication_slide, container, false);
        buttonGroup = (RadioGroup) view.findViewById(R.id.rg_authenticationMethod);

        RadioButton button = (RadioButton) view.findViewById(R.id.rb_fingerprint);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (canMoveFurther()) {
                    buttonGroup.clearCheck();
                    Toast.makeText(CustomAuthenticationSlide.this.getActivity(), "Fingerprint is not supported yet", Toast.LENGTH_SHORT).show();
                }
            }
        });

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
        return buttonGroup.getCheckedRadioButtonId() != -1;
    }

    @Override
    public String cantMoveFurtherErrorMessage() {
        return "Please select an authentication method";
    }
}
