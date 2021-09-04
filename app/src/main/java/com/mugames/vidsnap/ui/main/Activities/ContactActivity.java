package com.mugames.vidsnap.ui.main.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;

import com.mugames.vidsnap.R;

public class ContactActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        Toolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ImageButton imageButton = findViewById(R.id.button);
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
    }
}