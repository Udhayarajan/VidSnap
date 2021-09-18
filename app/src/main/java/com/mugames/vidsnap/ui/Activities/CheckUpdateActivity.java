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

package com.mugames.vidsnap.ui.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mugames.vidsnap.Firebase.FirebaseManager;
import com.mugames.vidsnap.R;

import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY;

/**
 * Activity to check app update
 */
public class CheckUpdateActivity extends AppCompatActivity {

    FirebaseManager firebaseManager;
    Button check_update;
    Button update;
    ImageView img_status;
    TextView txt_status;
    TextView whatNew;
    TextView current_version;
    TextView versionNew;

    LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_update);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());



        firebaseManager = FirebaseManager.getInstance(this);

        check_update = findViewById(R.id.update_check);
        update = findViewById(R.id.update_open);
        img_status = findViewById(R.id.update_image);
        txt_status = findViewById(R.id.update_status);
        whatNew = findViewById(R.id.update_whatNew);
        versionNew = findViewById(R.id.update_new_ver);

        linearLayout = findViewById(R.id.update_layout);

        current_version = findViewById(R.id.update_current_version);

        if (getIntent().getStringExtra(Intent.EXTRA_TEXT) != null) {
            checkUpdate();
        }
        try {
            current_version.setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        check_update.setOnClickListener(v -> checkUpdate());

        linearLayout.setVisibility(View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void checkUpdate() {
        firebaseManager.checkUpdate((isUpdateAvailable, version, isForced, changeLog, link) -> {
            if (isUpdateAvailable) {
                img_status.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_update_available));
                txt_status.setText("New version available!");
                linearLayout.setVisibility(View.VISIBLE);
                versionNew.setText(String.format("What's new!:- v%s",version));
                update.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                        startActivity(Intent.createChooser(intent, "Open via..."));
                        finishAndRemoveTask();
                    }
                });
                whatNew.setText(HtmlCompat.fromHtml(changeLog, FROM_HTML_MODE_LEGACY));
            } else {
                img_status.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_update_not_available));
                txt_status.setText("Great! You are up-to date");
            }
        });
    }

}