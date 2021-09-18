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

package com.mugames.vidsnap.Firebase;

import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.mugames.vidsnap.Firebase.FirebaseCallBacks.UpdateCallbacks;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Utility.UtilityInterface;
import com.mugames.vidsnap.ui.Activities.ReportActivity;

public class FirebaseManager {
    private static volatile FirebaseManager instance;

    Context context;
    FirebaseRemoteConfig remoteConfig;
    FirebaseDatabase firebaseDatabase;

    DatabaseReference bugReference;
    DatabaseReference siteReference;
    DatabaseReference otherReference;

    Long issue_Index= (long) -1;
    Long site_Index= (long) -1;
    Long other_Index= (long) -1;

    private FirebaseManager(Context context){
        this.context= context;
        remoteConfig = FirebaseRemoteConfig.getInstance();
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        firebaseDatabase = FirebaseDatabase.getInstance();
    }


    public static FirebaseManager getInstance(@NonNull Context context) {
        if(instance==null){
            synchronized (FirebaseManager.class){
                if(instance == null)
                    instance = new FirebaseManager(context.getApplicationContext());
            }
        }
        return instance;
    }

    public void getShareLink(FirebaseCallBacks.ShareLinkCallback shareLinkCallback) {
        remoteConfig.fetchAndActivate()
                .addOnCompleteListener( new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if(task.isSuccessful()){
                            shareLinkCallback.onShareLinkGot(remoteConfig.getString("share_link"));
                        }
                    }
                });
    }

    public void getShareText(FirebaseCallBacks.ShareTextCallback textCallback){
        remoteConfig.fetchAndActivate()
                .addOnCompleteListener( new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if(task.isSuccessful()){
                            textCallback.onShareTextGot(remoteConfig.getString("share_message"));
                        }
                    }
                });
    }

    public void getInstaCookie(UtilityInterface.CookiesInterface cookiesInterface) {
        remoteConfig.fetchAndActivate()
                .addOnCompleteListener(new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if(task.isSuccessful()){
                            cookiesInterface.onReceivedCookies(remoteConfig.getString("insta_cookies"));
                        }
                    }
                });
    }

    public enum REPORT{
        BUG,
        SITE_REQUEST,
        OTHER
    }



    public void checkUpdate(UpdateCallbacks updateCallbacks) {
        remoteConfig.fetchAndActivate()
                .addOnCompleteListener(new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if(task.isSuccessful()){
                            boolean isUpdateAvailable =false;
                            boolean isForced = false;
                            String version = remoteConfig.getString("new_version");
                            int code =0;
                            try{
                                code = Integer.parseInt(remoteConfig.getString("new_code"));
                            }catch (NumberFormatException  e){}
                            try {
                                isForced = code>context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionCode;
                                isUpdateAvailable = versionToInt(version)>versionToInt(context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName);
                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                            }
                            updateCallbacks.onReceiveData(isUpdateAvailable,version,
                                    isForced,
                                    remoteConfig.getString("change_log"),
                                    remoteConfig.getString("update_link"));
                        }
                    }
                });
    }

    int versionToInt(String version){
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);
            if(Character.isDigit(c)) builder.append(c);
        }
        return Integer.parseInt(builder.toString());
    }

    public void initReport(){
        bugReference =firebaseDatabase.getReference("Bugs");
        siteReference = firebaseDatabase.getReference("Features");
                otherReference = firebaseDatabase.getReference("Others");
        countBugs();
        count_site();
        count_others();
    }



    public void saveReport(ReportActivity reportActivity, REPORT type, String service, String des, String mail){
        ReportHelper report = new ReportHelper(service, des, mail);

        ValueEventListener saveListener =new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Toast.makeText(context,"Report Submitted",Toast.LENGTH_SHORT).show();
                reportActivity.finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        if(type.equals(REPORT.BUG)){
            bugReference.child(String.valueOf(issue_Index)).setValue(report);
            bugReference.addValueEventListener(saveListener);
        }else if(type.equals(REPORT.SITE_REQUEST)){
            siteReference.child(String.valueOf(site_Index)).setValue(report);
            siteReference.addValueEventListener(saveListener);
        }
        else {
            otherReference.child(String.valueOf(other_Index)).setValue(report);
            otherReference.addValueEventListener(saveListener);
        }
    }

    void countBugs(){
        bugReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                issue_Index = snapshot.getChildrenCount();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    void count_site(){
        siteReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                site_Index = snapshot.getChildrenCount();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    void count_others(){
        otherReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                other_Index = snapshot.getChildrenCount();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Keep
    public static class ReportHelper {
        public String service;
        public String des;
        public String mail;

        public ReportHelper(String service, String des, String mail) {
            this.service = service;
            this.des = des;
            this.mail = mail;
        }
    }
}
