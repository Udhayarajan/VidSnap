/*
 *  This file is part of VidSnap.
 *
 *  VidSnap is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  VidSnap is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with VidSnap.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mugames.vidsnap;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mugames.vidsnap.ui.activities.MainActivity;

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
