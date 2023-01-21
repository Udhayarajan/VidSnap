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
            textView.setSelected(true);
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