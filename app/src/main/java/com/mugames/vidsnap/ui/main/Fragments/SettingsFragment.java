package com.mugames.vidsnap.ui.main.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.DropDownPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.mugames.vidsnap.Utility.AppPref;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.StorageSwitcher;
import com.mugames.vidsnap.Utility.FileUtil;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.Utility.UtilityInterface;

import java.io.File;

import static com.mugames.vidsnap.ViewModels.MainActivityViewModel.STATIC_CACHE;
import static com.mugames.vidsnap.ViewModels.MainActivityViewModel.db_name;


public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {


    private ActivityResultLauncher<Intent> locationResultLauncher;
    StorageSwitcher storageSwitcher;

    SharedPreferences sharedPreferences;

    Preference download;
    Preference insta;
    Preference fb;
    Preference cacheHistory;

    AppPref pref;

    SwitchPreferenceCompat sendLinkSwitch;

    DropDownPreference dbPath;

    String TAG = Statics.TAG + ":Settings";



    public SettingsFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        pref = new AppPref(getActivity());

        super.onCreate(savedInstanceState);
        storageSwitcher = new StorageSwitcher(getActivity());

        locationResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            if (result.getData() != null) {
                                setPath(result.getData());
                            }
                        }
                    }
                }
        );
    }


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());


        ListPreference theme = findPreference(getString(R.string.key_Theme));

        download = findPreference(getString(R.string.key_download));
        dbPath = findPreference(getString(R.string.key_cache_path));

        sendLinkSwitch = findPreference(getString(R.string.key_media_link));


        insta = findPreference(getString(R.string.key_instagram));
        fb = findPreference(getString(R.string.key_facebook));
        cacheHistory = findPreference(getString(R.string.key_clear_history_cache));

        sendLinkSwitch.setChecked(pref.getBooleanValue(R.string.key_media_link,true));

        if(!FileUtil.isSDPresent(getActivity())){
            setString(R.string.key_cache_path,null);
            dbPath.setEnabled(false);
        }

        sendLinkSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                pref.setBooleanValue(R.string.key_media_link, (Boolean) newValue);
                return true;
            }
        });

        refreshCookiesLayout();

        Uri pathUri = pref.getSavePath();
        String path = pathUri==null?"Not Set":FileUtil.displayFormatPath(getActivity(),pref.getSavePath());

        download.setSummary(path);



        download.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                storageSwitcher.pick(locationResultLauncher);
                return true;
            }
        });

        if (theme != null) {
            theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.e(TAG, "onPreferenceChange: " + theme.findIndexOfValue(newValue.toString()));
                    if (theme.findIndexOfValue(newValue.toString()) == 0)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    else if (theme.findIndexOfValue(newValue.toString()) == 1)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    else
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    return true;
                }
            });
        }
    }

    private void setPath(Intent data) {
        pref.setSavePath(data);

        String disPath = FileUtil.displayFormatPath(getActivity(), pref.getSavePath());

        Log.d(TAG, "setPathDisp: "+disPath);

        download.setSummary(disPath);
    }

    public String getValue(int id,String def){
        return  pref.getStringValue(id,def);
    }

    public String getValue(int id){
        return  sharedPreferences.getString(getString(id),null);
    }

    public void setString(int key, String value){
        pref.setStringValue(key,value);
    }

    void refreshCookiesLayout(){
        String instaCookie= getValue(R.string.key_instagram);
        String faceBookCookie= getValue(R.string.key_facebook);
        String hisCache= FileUtil.isFileExists(pref.getCachePath(STATIC_CACHE))?"devalue":null;


        if(instaCookie!=null){
            insta.setOnPreferenceClickListener(this);
            instaCookie = "Tap to Logout";
        }
        else{
            insta.setOnPreferenceClickListener(null);
            instaCookie ="Not logged in";
        }
        if(faceBookCookie!=null){
            fb.setOnPreferenceClickListener(this);
            faceBookCookie = "Tap to Logout";
        }
        else{
            fb.setOnPreferenceClickListener(null);
            faceBookCookie ="Not logged in";
        }
        if(hisCache!=null){
            cacheHistory.setOnPreferenceClickListener(this);
            hisCache = "Tap to clear cache";
        }
        else{
            cacheHistory.setOnPreferenceClickListener(null);
            hisCache="Great, No cache Found!!";
        }

        insta.setSummary(instaCookie);
        fb.setSummary(faceBookCookie);
        cacheHistory.setSummary(hisCache);

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(preference==insta){
            ((MainActivity)getActivity()).logOutInsta(new UtilityInterface.LogoutCallBacks() {
                @Override
                public void onLoggedOut() {
                    setString(R.string.key_instagram,null);
                    refreshCookiesLayout();
                }
            });

        }else if(preference==fb){
            ((MainActivity)getActivity()).logOutFB(new UtilityInterface.LogoutCallBacks() {
                @Override
                public void onLoggedOut() {
                    setString(R.string.key_facebook,null);
                    refreshCookiesLayout();
                }
            });


        }else if(preference== cacheHistory){
            ((MainActivity)getActivity()).clearHistory(new UtilityInterface.LogoutCallBacks() {
                @Override
                public void onLoggedOut() {
                    Toast.makeText(getActivity(),"Cleared caches",Toast.LENGTH_SHORT).show();
                    refreshCookiesLayout();
                }
            });
        }else {
            return false;
        }
        return true;
    }
}