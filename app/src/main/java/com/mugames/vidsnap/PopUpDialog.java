package com.mugames.vidsnap;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

public class PopUpDialog{

    String text;
    Activity activity;
    AlertDialog dialog;
    TextView textView;
    public PopUpDialog(Activity activity) {
        this.activity=activity;
    }


    public void show(String text) {
        if (dialog == null) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
            View v = activity.getLayoutInflater().inflate(R.layout.loading_dialog, null);
            textView = v.findViewById(R.id.process_name);
            textView.setText(text);
            dialogBuilder.setView(v);
            dialogBuilder.setCancelable(false);
            dialog = dialogBuilder.create();
            dialog.show();
        }
        else {
            textView.setText(text);
            if(dialog!=null && !dialog.isShowing()) dialog.show();
        }

    }


    public void dismiss(){
        if(dialog!=null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}