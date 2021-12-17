/*
 *  This file is part of VidSnap.
 *
 *  VidSnap is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *  VidSnap is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with VidSnap.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.mugames.vidsnap;

import static com.mugames.vidsnap.utility.Statics.TAG;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CircularProgressDialog{

    Activity activity;
    AlertDialog dialog;
    TextView progressText;
    TextView status;
    ProgressBar progressBar;
    int progress=0;
    String statusTxt="";
    public CircularProgressDialog(Activity activity) {
        this.activity=activity;
    }


    public void show() {
        String text = progress+"%";
        if (dialog == null) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
            View v = activity.getLayoutInflater().inflate(R.layout.circular_download_dialog, null);
            progressText = v.findViewById(R.id.progressValue);
            progressBar = v.findViewById(R.id.progressBar);
            status = v.findViewById(R.id.download_status);
            status.setText(statusTxt);
            progressText.setText(text);
            progressBar.setProgress(progress);
            dialogBuilder.setView(v);
            dialogBuilder.setCancelable(false);
            dialog = dialogBuilder.create();
            dialog.show();
        }
        else {
            progressText.setText(text);
            progressBar.setProgress(progress);
            status.setText(statusTxt);
            if(dialog!=null && !dialog.isShowing()) dialog.show();
        }
    }

    public void setProgress(int progress) {
        this.progress = progress;
        show();
    }

    public void setStatusTxt(String statusTxt) {
        this.statusTxt = statusTxt;
        show();
    }

    public void dismiss(){
        if(dialog!=null && dialog.isShowing()) {
            dialog.dismiss();
            Log.e(TAG, "dismiss: called from circular" );
        }
    }
}
