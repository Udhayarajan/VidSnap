package com.mugames.vidsnap.ui.main.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.mugames.vidsnap.R;
import com.mugames.vidsnap.ViewModels.MainActivityViewModel;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;

public class ShareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        Intent intent = new Intent(this, MainActivity.class);
        String content = MainActivityViewModel.intentString(getIntent());
        if(content ==null){
            Start(this,null);
            finish();
            return;
        }
        intent.putExtra(Intent.EXTRA_TEXT,content);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Start(this,intent);
        finish();


    }

    public static void Start(Activity activity, Intent intent){
        activity.startActivity(intent);
    }
}