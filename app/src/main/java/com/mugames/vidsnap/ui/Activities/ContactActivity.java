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
import androidx.core.text.HtmlCompat;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mugames.vidsnap.R;

/**
 * About activity
 */
public class ContactActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        Toolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ImageButton imageButton = findViewById(R.id.button_insta);
        imageButton.setOnClickListener(v->{
            Uri uri = Uri.parse("http://www.instagram.com/udhayarajan_m/");
            Intent intent = new Intent(Intent.ACTION_VIEW,uri);
            try{
                intent.setPackage("com.instagram.android");
                startActivity(intent);
            }catch (ActivityNotFoundException e){
                intent = new Intent(Intent.ACTION_VIEW,uri);
                startActivity(Intent.createChooser(intent, "Open with..."));
            }
        });

        ImageButton linkedin = findViewById(R.id.button_linkedin);
        linkedin.setOnClickListener(v->{
            String profile_url = "https://www.linkedin.com/in/udhayarajan-m";
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(profile_url));
                intent.setPackage("com.linkedin.android");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(profile_url)));
            }
        });

        TextView sourceCode = findViewById(R.id.code_base_url);
        sourceCode.setClickable(true);
        sourceCode.setMovementMethod(LinkMovementMethod.getInstance());
        sourceCode.setText(HtmlCompat.fromHtml("<p> You can get source code and information about libraries form <a href='https://github.com/Udhayarajan/VidSnap'>here</a></p>",HtmlCompat.FROM_HTML_MODE_LEGACY));
    }
}