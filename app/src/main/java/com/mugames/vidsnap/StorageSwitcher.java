package com.mugames.vidsnap;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.mugames.vidsnap.Utility.FileUtil;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.Utility.UtilityClass;

import java.io.File;

import static androidx.activity.result.contract.ActivityResultContracts.*;
import static com.mugames.vidsnap.Utility.Statics.PACKAGE_NAME;

public class StorageSwitcher {
    Activity activity;
    AlertDialog dialog;

    String TAG = Statics.TAG+":StorageSwitcher";




    public StorageSwitcher(Activity activity){
        this.activity= activity;
    }

    public void show(SwitcherInterface switcherInterface,boolean isExCardAvailable){
        AlertDialog.Builder  builder= new AlertDialog.Builder(activity);
        View v=activity.getLayoutInflater().inflate(R.layout.storage_switcher,null);
        Button sd= v.findViewById(R.id.internal);
        Button ex=v.findViewById(R.id.external);
        if(!isExCardAvailable){
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

    public void dismiss(){
        dialog.dismiss();
    }

    public void pick(ActivityResultLauncher<Intent> locationResultLauncher){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        locationResultLauncher.launch(intent);
    }


    public interface SwitcherInterface{
        void onClick(boolean isInternal);
    }

}