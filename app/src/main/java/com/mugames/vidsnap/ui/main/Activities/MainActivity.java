package com.mugames.vidsnap.ui.main.Activities;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;


import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.mugames.vidsnap.DataBase.HistoryDatabase;
import com.mugames.vidsnap.Firebase.FirebaseCallBacks;
import com.mugames.vidsnap.PopUpDialog;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.StorageSwitcher;
import com.mugames.vidsnap.Terms;
import com.mugames.vidsnap.Threads.Downloader;
import com.mugames.vidsnap.Threads.HttpRequest;
import com.mugames.vidsnap.Threads.MiniExecute;
import com.mugames.vidsnap.Utility.Bundles.DownloadDetails;
import com.mugames.vidsnap.Utility.DownloadReceiver;
import com.mugames.vidsnap.Utility.FileUtil;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.Utility.UtilityClass;
import com.mugames.vidsnap.Utility.UtilityInterface;
import com.mugames.vidsnap.Utility.UtilityInterface.LogoutCallBacks;
import com.mugames.vidsnap.Utility.UtilityInterface.TouchCallback;
import com.mugames.vidsnap.ViewModels.HistoryViewModel;
import com.mugames.vidsnap.ViewModels.MainActivityViewModel;
import com.mugames.vidsnap.ui.main.Fragments.DownloadFragment;
import com.mugames.vidsnap.ui.main.Fragments.HistoryFragment;
import com.mugames.vidsnap.ui.main.Fragments.HomeFragment;
import com.mugames.vidsnap.ui.main.Fragments.LoginFragment;
import com.mugames.vidsnap.ui.main.Fragments.SettingsFragment;
import com.mugames.vidsnap.ui.main.Fragments.VideoFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

import static com.mugames.vidsnap.Utility.FileUtil.GetValidFile;
import static com.mugames.vidsnap.Utility.Statics.ACTIVE_DOWNLOAD;
import static com.mugames.vidsnap.Utility.Statics.BANNER_ID;
import static com.mugames.vidsnap.Utility.Statics.COMMUNICATOR;
import static com.mugames.vidsnap.Utility.Statics.INTERSTITIAL_ID;
import static com.mugames.vidsnap.Utility.Statics.REQUEST_WRITE;
import static com.mugames.vidsnap.ViewModels.MainActivityViewModel.DYNAMIC_CACHE;
import static com.mugames.vidsnap.ViewModels.MainActivityViewModel.STATIC_CACHE;
import static com.mugames.vidsnap.ViewModels.MainActivityViewModel.db_name;
import static com.mugames.vidsnap.ViewModels.MainActivityViewModel.service_in_use;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        Toolbar.OnMenuItemClickListener{

    String TAG = Statics.TAG + ":MainActivity";



    public PopUpDialog dialog;


    TextView textView_activeDownload;

    Intent intent = null;
    DrawerLayout drawer;

    TouchCallback touchCallback;

    private NavigationView navigationView;
    private StorageSwitcher storageSwitcher;
    private ActivityResultLauncher<Intent> locationResultLauncher;

    MainActivityViewModel activityViewModel;





    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_preview);

        Log.e(TAG, "onCreate: "+getApplicationContext().getCodeCacheDir());

        activityViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);


        String[] themes = getResources().getStringArray(R.array.theme_values);
        String theme = getStringValue(R.string.key_Theme, themes[0]);

        if (theme.equals(themes[1]))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        else if (theme.equals(themes[2]))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);


        storageSwitcher = new StorageSwitcher(this);

        locationResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            setPath(result.getData());
                        }
                    }
                }
        );




        drawer = findViewById(R.id.drawer_layout);


        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        activityViewModel.checkUpdate(new FirebaseCallBacks.UpdateCallbacks() {
            @Override
            public void onReceiveData(boolean isUpdateAvailable, String v, boolean isForced, String changeLog, String url) {
                if (isUpdateAvailable && isForced) {
                    Intent i = new Intent(MainActivity.this, CheckUpdateActivity.class);
                    i.putExtra(Intent.EXTRA_TEXT, "forced");
                    startActivity(i);
                }
            }
        });

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment.newInstance(), HomeFragment.class.getName());
            navigationView.setCheckedItem(R.id.nav_home);
        }


        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.top_menu);
        onMenuCreate(toolbar.getMenu());
        toolbar.setOnMenuItemClickListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        dialog = new PopUpDialog(this);



        if (!activityViewModel.isAgree()) {
            new Terms(this).show();
            intent = getIntent();
            setIntent(null);
        } else {
            gotIntent(getIntent());
        }

    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (!activityViewModel.isAgree()) {
            this.intent = intent;
            return;
        }
        gotIntent(intent);

    }

    void gotIntent(Intent intent) {
        if (intent == null) return;
        String content = MainActivityViewModel.intentString(intent);


        if (content != null)
            replaceFragment(VideoFragment.newInstance(content), VideoFragment.class.getName());
    }




    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_home:
                replaceFragment(HomeFragment.newInstance(), HomeFragment.class.getName());
                break;
            case R.id.nav_history:
                replaceFragment(HistoryFragment.newInstance(), HistoryFragment.class.getName());
                break;
            case R.id.nav_check_update:
                checkUpdate();
                break;
            case R.id.nav_settings:
                replaceFragment(new SettingsFragment(), SettingsFragment.class.getName());
                break;
            case R.id.nav_contact_insta:
                openInsta();
                break;
            case R.id.nav_contact_twitter:
                openTwitter();
                break;
            case R.id.nav_contact_mail:
                openMail();
                break;
            case R.id.nav_bug_report:
                openBug();
                break;
            case R.id.nav_share:
                share();
                break;
            case R.id.nav_about_us:
                about();
        }
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    private void about() {
        Intent intent = new Intent(this, ContactActivity.class);
        startActivity(intent);
    }

    private void share() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        activityViewModel.shareLink(new FirebaseCallBacks.ShareCallback() {
            @Override
            public void onShareLinkGot(String link) {
                intent.putExtra(Intent.EXTRA_TEXT, "Hey! checkout new Social Media video downloader from " + link);
                intent.setType("text/plain");
                startActivity(Intent.createChooser(intent, "Share via..."));
            }
        });
    }

    private void openBug() {
        Intent intent = new Intent(this, ReportActivity.class);
        startActivity(intent);
    }

    private void checkUpdate() {
        Intent i = new Intent(this, CheckUpdateActivity.class);
        startActivity(i);
    }

    private void openMail() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("mailto:app.vidsnap@gmail.com"));
        startActivity(Intent.createChooser(intent, "Send via..."));
    }

    private void openTwitter() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?screen_name=vidsnapapp")));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/vidsnapapp")));
        }
    }

    void openInsta() {
        Uri uri = Uri.parse("http://www.instagram.com/vidsnapapp/");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            intent.setPackage("com.instagram.android");
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(Intent.createChooser(intent, "Open with..."));
        }
    }

    public void replaceFragment(Fragment fragment, String tag) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        boolean isRunning = manager.popBackStackImmediate(tag, 0);
        Fragment runningFragment = manager.findFragmentByTag(tag);


        if (!isRunning && runningFragment == null) {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.addToBackStack(tag);
            transaction.replace(R.id.fragment_container, fragment, tag).commit();

        } else if (runningFragment != null) {

//            if (isInstanceof(runningFragment,fragment,VideoFragment.class)) {
//                VideoFragment videoFragment = ((VideoFragment) runningFragment);
//                videoFragment.link = ((VideoFragment) fragment).link;
//                if(videoFragment.urlBox==null){
//                    manager.popBackStack();
//                    replaceFragment(fragment,tag);
//                    return;
//                }
//                else videoFragment.Update();
//            } else if (isInstanceof(runningFragment,fragment,DownloadFragment.class)) {
//                DownloadFragment downloadFragment = (DownloadFragment) runningFragment;
//                DownloadFragment dataFragment = (DownloadFragment) fragment;
//                if(downloadFragment.recyclerView == null) {
//                    manager.popBackStack();
//                    replaceFragment(fragment,tag);
//                    return;
//                }
//                else downloadFragment.setArrayList(dataFragment.list, dataFragment.DownloadReceiver);
//            }
            transaction.show(runningFragment).commit();
        }
    }

    boolean isInstanceof(Fragment runningFragment,Fragment incomingFragment, Class<?> fragment){
        return fragment.isInstance(runningFragment) && fragment.isInstance(incomingFragment);
    }


    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1)
            finish();
        else
            super.onBackPressed();
        updateSelector();
    }

    // in navigation Drawer
    private void updateSelector() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment == null) return;
        if (fragment instanceof HistoryFragment)
            navigationView.setCheckedItem(R.id.nav_history);
        else if (fragment instanceof HomeFragment || fragment instanceof VideoFragment)
            navigationView.setCheckedItem(R.id.nav_home);
        else if (fragment instanceof SettingsFragment)
            navigationView.setCheckedItem(R.id.nav_settings);
    }

    public void onMenuCreate(Menu menu) {
        final MenuItem menuItem = menu.findItem(R.id.menu_download);

        View actionView = menuItem.getActionView();

        textView_activeDownload = actionView.findViewById(R.id.active_download);

        actionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMenuItemClick(menuItem);
            }
        });

        setupBadge(activityViewModel.getDownloadDetailsList().size());
    }


    public void setupBadge(Integer size) {
        if (textView_activeDownload != null) {
            if (size == 0) {
                if (textView_activeDownload.getVisibility() != View.GONE) {
                    textView_activeDownload.setVisibility(View.GONE);
                }
            } else {
                textView_activeDownload.setText(String.valueOf(Math.min(size, 99)));
                if (textView_activeDownload.getVisibility() != View.VISIBLE) {
                    textView_activeDownload.setVisibility(View.VISIBLE);
                }
            }
        }
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Log.d(TAG, "onMenuItemClick: ");
        if (item.getItemId() == R.id.menu_download) {
            Log.d(TAG, "List size:"+activityViewModel.getDownloadDetailsList().size());
            replaceFragment(DownloadFragment.newInstance(), DownloadFragment.class.getName());
        }
        return false;
    }

    public void error(String reason, Exception e) {
        if (dialog != null)
            dialog.dismiss();
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        try {
            if (fragment == null)
                return;
            VideoFragment videoFragment = (VideoFragment) fragment;
            videoFragment.unLockAnalysis();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                .setCancelable(true)
                .setTitle("Oops!!")
                .setMessage(reason);
        Log.e(TAG, "error: ", e);

        if (e != null) {
            if (activityViewModel.pref.getBooleanValue(R.string.key_media_link, true)) {
                dialogBuilder.setNegativeButton("Go,Back", (dialog, which) -> {
                    FirebaseCrashlytics.getInstance().recordException(e);
                });
            } else {
                dialogBuilder.setPositiveButton("Report", (dialog, which) -> {
                    FirebaseCrashlytics.getInstance().recordException(e);
                    Toast.makeText(MainActivity.this, "Thanks for reporting", Toast.LENGTH_SHORT).show();
                });

                dialogBuilder.setNegativeButton("Go,Back", (dialog, which) -> {
                    FirebaseCrashlytics.getInstance().setCustomKey("URL", "--");
                    FirebaseCrashlytics.getInstance().recordException(e);
                });
            }
        } else dialogBuilder.setNegativeButton("Go,Back", null);
        dialogBuilder.create().show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_WRITE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                if (getPath() == null || Objects.requireNonNull(DocumentFile.fromTreeUri(getApplicationContext(), getPath())).exists()) {
                    Log.e(TAG, "onRequestPermissionsResult: ");
                    openDirectoryChooser();
                    return;
                }
            download(null);
        } else {
            Toast.makeText(this, "Sorry We can't download", Toast.LENGTH_SHORT).show();
            activityViewModel.getDownloadDetailsList().clear();
        }
    }


    private void openDirectoryChooser() {
        Log.e(TAG, "openDirectoryChooser: ");
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        dialogBuilder.setTitle("Download Path");

        DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                storageSwitcher.pick(locationResultLauncher);
            }
        };

        dialogBuilder.setCancelable(false);
        //TODO Support for Android 12
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            dialogBuilder.setMessage("No path chosen to download. Default path will be\n\nInternal Storage\\Download");
            dialogBuilder.setPositiveButton("Change", positiveListener);
            dialogBuilder.setNegativeButton("Default", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(MainActivity.this, "Default path selected", Toast.LENGTH_SHORT).show();
                    setPath(null);
                }
            });

        } else {
            dialogBuilder.setMessage("No path chosen to download. Choose path to proceed downloading.");
            dialogBuilder.setPositiveButton("Choose", positiveListener);
        }
        dialogBuilder.create().show();
    }



    public void agreedTerms() {
        activityViewModel.pref.setBooleanValue(R.string.key_terms_con, true);
        if (intent != null) gotIntent(intent);
    }


    private void setPath(Intent data) {

        activityViewModel.pref.setSavePath(data);

        for (int i = 0; i < activityViewModel.tempDetails.size(); i++) {
            activityViewModel.tempDetails.get(i).pathUri = getPath();
        }

        storageSwitcher = null;

        download(null);
    }


    public Uri getPath() {
        return activityViewModel.pref.getSavePath();
    }



    public String getStringValue(int id, String def) {
        return activityViewModel.pref.getStringValue(id, def);
    }

    public void setStringValue(int key, String value) {
        activityViewModel.pref.setStringValue(key, value);
    }

    public void download(ArrayList<DownloadDetails> details) {
        if (service_in_use && details.get(0).audioURL != null) {
            error("Video download in progress please try after completing download.", null);
            return;
        }
        if (details != null)
            activityViewModel.tempDetails.addAll(details);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE);
                return;
            }
        }
        if (getPath() == null || !isValidDownloadPath()) {
            openDirectoryChooser();
            return;
        }
        for(DownloadDetails d:activityViewModel.tempDetails)
            if(d.audioURL!=null && !FileUtil.isFileExists(getExternalFilesDir("libs")+File.separator+"jni")){
                fetchSOFiles();
                return;
            }
        realDownload();
//        tryShowAd();
    }

    private void fetchSOFiles() {
        final String abi = Build.SUPPORTED_ABIS[0];

        HashMap<String,String> abiHashMap = new HashMap<>();
        abiHashMap.put("armeabi-v7a","link for armeabi");
        abiHashMap.put("arm64-v8a","link for armeabi");
        abiHashMap.put("x86","link for armeabi");
        abiHashMap.put("x86_64","link for x86_64");

        new MiniExecute(this, abiHashMap.get(abi), true, 0, new UtilityInterface.MiniExecutorCallBack() {
            @Override
            public void onBitmapReceive(Bitmap image) {}

            @Override
            public void onSizeReceived(int size, int position) {
                downloadAdditionalModule(size);
            }
        }).start();
    }

    private void downloadAdditionalModule(long size) {
        //TODO:Add logic to download zip files from github server with downloading dialogue box
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        dialogBuilder.setTitle("Additional required");
        dialogBuilder.setMessage(String.format("Additional file (%s) needed to be download to use %s downloader. Would you like to download it ?",UtilityClass.formatFileSize(size,false),activityViewModel.tempDetails.get(0).src));
        dialogBuilder.setPositiveButton("Download", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        dialogBuilder.setNegativeButton("Leave", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this,"Download aborted",Toast.LENGTH_SHORT).show();
            }
        });
    }

    boolean isValidDownloadPath() {
        try {
            DocumentFile file = DocumentFile.fromTreeUri(MainActivity.this, getPath());
            if (file != null) return file.exists();
        }catch (IllegalArgumentException e){
            e.printStackTrace();
            return new File(getPath().getPath()).exists();
        }
        return false;
    }


    private void realDownload() {
        dialog.dismiss();
        activityViewModel.getActiveDownload().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                setupBadge(integer);
            }
        });
        for (int i = 0; i < activityViewModel.tempDetails.size(); i++) {
            Intent download = detailsToIntent(i);
            ContextCompat.startForegroundService(this, download);
        }

        activityViewModel.tempDetails.clear();
    }

    Intent detailsToIntent(int index) {

        Intent downloadIntent = new Intent(this, Downloader.class);


        activityViewModel.tempDetails.get(index).thumbNailPath = GetValidFile(activityViewModel.pref.getCachePath(DYNAMIC_CACHE), String.valueOf(new Random().nextInt()), ".muim");
        activityViewModel.tempDetails.get(index).thumbWidth = activityViewModel.tempDetails.get(index).thumbNail.getWidth();
        activityViewModel.tempDetails.get(index).thumbHeight = activityViewModel.tempDetails.get(index).thumbNail.getHeight();
        FileUtil.saveFile(activityViewModel.tempDetails.get(index).thumbNailPath, UtilityClass.bitmapToBytes(activityViewModel.tempDetails.get(index).thumbNail));




        if (activityViewModel.tempDetails.get(index).videoURL == null){
            activityViewModel.tempDetails.get(index).chuncksPath = GetValidFile(activityViewModel.pref.getCachePath(DYNAMIC_CACHE), String.valueOf(new Random().nextInt()), ".much");
            FileUtil.saveFile(activityViewModel.tempDetails.get(index).chuncksPath, String.valueOf(activityViewModel.tempDetails.get(index).m3u8URL));
        }


        activityViewModel.tempDetails.get(index).receiver = new DownloadReceiver(new Handler(Looper.getMainLooper()), this,
                activityViewModel.getDownloadDetailsList().size(), activityViewModel);


        downloadIntent.putExtra(COMMUNICATOR, activityViewModel.tempDetails.get(index));

        activityViewModel.tempDetails.get(index).thumbNail = null;


        activityViewModel.addDownloadDetails(activityViewModel.tempDetails.get(index));

        return downloadIntent;
    }


    public void signInNeeded(String reason, String loginURL, String[] loginDoneUrl, UtilityInterface.LoginIdentifier identifier) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Seems to be Private Video!")
                .setCancelable(false)
                .setMessage(reason)
                .setPositiveButton("Sign-in", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openWebPage(loginURL, loginDoneUrl, null, new UtilityInterface.CookiesInterface() {
                            @Override
                            public void onReceivedCookies(String cookies) {
                                identifier.loggedIn(cookies);
                            }
                        });
                    }
                })
                .setNegativeButton("No,Thanks", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        error("URL invalid or Login Permission Dined", null);
                    }
                })
                .create();
        dialog.show();
    }

    void openWebPage(String url, String[] loginDoneUrls, String cookies, UtilityInterface.CookiesInterface cookiesInterface) {
        //Cookies for logout
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        LoginFragment loginFragment = LoginFragment.newInstance(url, loginDoneUrls, cookies, new UtilityInterface.CookiesInterface() {
            @Override
            public void onReceivedCookies(String cookies) {
                fragmentManager.popBackStack();
                cookiesInterface.onReceivedCookies(cookies);
            }
        });
        transaction.replace(R.id.fragment_container, loginFragment, "LOGIN_FRAG");
        transaction.addToBackStack(null);
        transaction.commit();
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (touchCallback != null)
            touchCallback.onDispatchTouch(ev);
        return super.dispatchTouchEvent(ev);
    }

    public void setTouchCallback(TouchCallback touchCallback) {
        this.touchCallback = touchCallback;
    }

    public void clearHistory(LogoutCallBacks logoutCallBacks) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Warning")
                .setMessage("Downloads list will be cleared by clearing cache.")
                .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setStringValue(R.string.key_clear_history_cache, null);
//                        new Thread(() -> HistoryDatabase.getInstance(getApplication()).historyDao().deleteTable()).start();
                        FileUtil.deleteFile(activityViewModel.pref.getCachePath(STATIC_CACHE));
                        logoutCallBacks.onLoggedOut();
                    }
                })
                .setNegativeButton("Go,Back", null)
                .setCancelable(false)
                .create().show();
    }



    public void logOutInsta(LogoutCallBacks logoutCallBacks) {
        openWebPage("https://instagram.com/accounts/logout/", new String[]{"https://www.instagram.com/"}, getStringValue(R.string.key_instagram, null), new UtilityInterface.CookiesInterface() {
            @Override
            public void onReceivedCookies(String cookies) {
                Toast.makeText(MainActivity.this, "Logout Successful", Toast.LENGTH_SHORT).show();
                logoutCallBacks.onLoggedOut();
            }
        });
    }

    public void logOutFB(LogoutCallBacks logoutCallBacks) {
        //TODO FB logout
        openWebPage("https://www.facebook.com/help/contact/logout?id=260749603972907",
                new String[]{"https://www.facebook.com/help/contact/260749603972907", "https://m.facebook.com/help/contact/260749603972907"}, getStringValue(R.string.key_facebook, null),
                new UtilityInterface.CookiesInterface() {
                    @Override
                    public void onReceivedCookies(String cookies) {
                        Toast.makeText(MainActivity.this, "Logout Successful", Toast.LENGTH_SHORT).show();
                        logoutCallBacks.onLoggedOut();
                    }
                });
    }
}