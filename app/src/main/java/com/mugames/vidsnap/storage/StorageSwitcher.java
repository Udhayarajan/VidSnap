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

package com.mugames.vidsnap.storage;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;

import com.mugames.vidsnap.R;
import com.mugames.vidsnap.utility.Statics;

import java.io.File;

public class StorageSwitcher {
    Activity activity;
    AlertDialog dialog;

    String TAG = Statics.TAG + ":StorageSwitcher";


    public StorageSwitcher(Activity activity) {
        this.activity = activity;
    }

    public void show(SwitcherInterface switcherInterface, boolean isExCardAvailable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View v = activity.getLayoutInflater().inflate(R.layout.storage_switcher, null);
        Button sd = v.findViewById(R.id.internal);
        Button ex = v.findViewById(R.id.external);
        if (!isExCardAvailable) {
            ex.setAlpha(0.75f);
            ex.setEnabled(false);
        }
        builder.setView(v);
        builder.setCancelable(true);

        dialog = builder.create();

        sd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switcherInterface.onClick(true);
            }
        });
        ex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switcherInterface.onClick(false);
            }
        });
        dialog.show();
    }

    public void dismiss() {
        dialog.dismiss();
    }

    public void pick(ActivityResultLauncher<Intent> locationResultLauncher) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri uri = AppPref.getInstance(activity).getSavePath();
            if (uri == null) intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, "");
            else {
                if (DocumentsContract.isTreeUri(uri)){
                    uri = DocumentFile.fromTreeUri(activity, uri).getUri();
                }
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
            }
        }
        locationResultLauncher.launch(intent);
    }


    public interface SwitcherInterface {
        void onClick(boolean isInternal);
    }

}