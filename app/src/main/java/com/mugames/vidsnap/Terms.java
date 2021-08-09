package com.mugames.vidsnap;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;

public class Terms {
    MainActivity activity;
    AlertDialog dialog;

    public Terms(MainActivity mainActivity) {
        this.activity = mainActivity;
    }

    public void show(){
        MaterialAlertDialogBuilder dialogBuilder=new MaterialAlertDialogBuilder(activity);
        View v = activity.getLayoutInflater().inflate(R.layout.terms, null);
        dialogBuilder.setView(v);
        dialogBuilder.setCancelable(false);
        Button close=v.findViewById(R.id.term_close);
        Button agree=v.findViewById(R.id.term_agree);
        agree.setAlpha(.75f);
        agree.setEnabled(false);
        CheckBox checkBox=v.findViewById(R.id.term_checkbox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(checkBox.isChecked()){
                    agree.setAlpha(1);
                    agree.setEnabled(true);
                }
                else {
                    agree.setAlpha(.75f);
                    agree.setEnabled(false);
                }
            }
        });

        agree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                activity.agreedTerms();
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.finish();
            }
        });
        dialog = dialogBuilder.create();
        dialog.show();
    }
}
