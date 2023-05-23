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

package com.mugames.vidsnap.ui.activities;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
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
import com.mugames.vidsnap.BuildConfig;
import com.mugames.vidsnap.CircularProgressDialog;
import com.mugames.vidsnap.database.History;
import com.mugames.vidsnap.database.HistoryDatabase;
import com.mugames.vidsnap.firebase.FirebaseCallBacks;
import com.mugames.vidsnap.firebase.FirebaseManager;
import com.mugames.vidsnap.PopUpDialog;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.storage.StorageSwitcher;
import com.mugames.vidsnap.Terms;
import com.mugames.vidsnap.network.Downloader;
import com.mugames.vidsnap.network.MiniExecute;
import com.mugames.vidsnap.storage.AppPref;
import com.mugames.vidsnap.ui.fragments.EditFragment;
import com.mugames.vidsnap.utility.OneTimeShareManager;
import com.mugames.vidsnap.utility.VideoSharedBroadcast;
import com.mugames.vidsnap.utility.bundles.DownloadDetails;
import com.mugames.vidsnap.utility.DownloadReceiver;
import com.mugames.vidsnap.storage.FileUtil;
import com.mugames.vidsnap.utility.Statics;
import com.mugames.vidsnap.utility.UtilityClass;
import com.mugames.vidsnap.utility.UtilityInterface;
import com.mugames.vidsnap.utility.UtilityInterface.ConfigurationCallback;
import com.mugames.vidsnap.utility.UtilityInterface.TouchCallback;
import com.mugames.vidsnap.ui.fragments.StatusFragment;
import com.mugames.vidsnap.ui.viewmodels.MainActivityViewModel;
import com.mugames.vidsnap.ui.fragments.DownloadFragment;
import com.mugames.vidsnap.ui.fragments.HistoryFragment;
import com.mugames.vidsnap.ui.fragments.HomeFragment;
import com.mugames.vidsnap.ui.fragments.LoginFragment;
import com.mugames.vidsnap.ui.fragments.SettingsFragment;
import com.mugames.vidsnap.ui.fragments.VideoFragment;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2core.FetchObserver;
import com.tonyodev.fetch2core.Reason;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import static com.mugames.vidsnap.postprocessor.FFMPEG.FFMPEG_VERSION;
import static com.mugames.vidsnap.utility.Statics.ACTIVE_DOWNLOAD;
import static com.mugames.vidsnap.utility.Statics.COMMUNICATOR;
import static com.mugames.vidsnap.utility.Statics.FETCH_MESSAGE;
import static com.mugames.vidsnap.utility.Statics.FILE_MIME;
import static com.mugames.vidsnap.utility.Statics.OUTFILE_URI;
import static com.mugames.vidsnap.utility.Statics.PROGRESS;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_UPDATE_AUDIO;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_UPDATE_MERGING;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_UPDATE_RECODE_AUDIO;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_UPDATE_RECODE_VIDEO;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_UPDATE_VIDEO;
import static com.mugames.vidsnap.utility.Statics.REQUEST_WRITE;
import static com.mugames.vidsnap.storage.AppPref.LIBRARY_PATH;
import static com.mugames.vidsnap.ui.viewmodels.VideoFragmentViewModel.URL_KEY;
import static com.mugames.vidsnap.utility.Statics.RESULT_CODE;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        Toolbar.OnMenuItemClickListener, FetchObserver<Download>, UtilityInterface.DialogueInterface, UtilityInterface.LoginHelper {

    String TAG = Statics.TAG + ":MainActivity";


    public PopUpDialog dialog;
    CircularProgressDialog circularProgressDialog;


    TextView textView_activeDownload;

    Intent intent = null;
    DrawerLayout drawer;

    TouchCallback touchCallback;

    private NavigationView navigationView;
    private StorageSwitcher storageSwitcher;
    private ActivityResultLauncher<Intent> locationResultLauncher;

    private OneTimeShareManager oneTimeShareManager;

    MainActivityViewModel activityViewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_preview);

        FirebaseCrashlytics.getInstance().setCustomKey("FCMToken", AppPref.getInstance(this).getFcmToken());


        activityViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        activityViewModel.getActiveDownload().observe(this, this::setupBadge);

        String[] themes = getResources().getStringArray(R.array.theme_values);
        String theme = getStringValue(R.string.key_Theme, themes[0]);

        if (theme.equals(themes[1]))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        else if (theme.equals(themes[2]))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);


        storageSwitcher = new StorageSwitcher(this);


        activityViewModel.getDownloadFailedResponseLiveData().observe(this, result ->
                error(result.getResponse(), result.getException()));

        locationResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            setPath(result.getData());
                        } else {
                            activityViewModel.tempDetails.clear();
                        }
                    }
                }
        );

        oneTimeShareManager = new OneTimeShareManager(this, result -> activityViewModel.deleteSharedFile());

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


        if (activityViewModel.isNotAgreed()) {
            new Terms(this).show();
            intent = getIntent();
            setIntent(null);
        } else {
            gotIntent(getIntent());
        }

        if (activityViewModel.isProgressDialogVisible()) {
            dialog.show(activityViewModel.getProgressDialogText());
        }

        addShareOnlyListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        VideoSharedBroadcast.delete(this, activityViewModel.getTempResultIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (activityViewModel.isNotAgreed()) {
            this.intent = intent;
            return;
        }
        gotIntent(intent);
    }

    void gotIntent(Intent intent) {
        if (intent == null) return;
        String content = MainActivityViewModel.intentString(intent);
        Uri uri = MainActivityViewModel.intentUriForEditing(intent);
        if (content != null)
            replaceFragment(VideoFragment.newInstance(content), VideoFragment.class.getName());
        else if (uri != null) {
            replaceFragment(EditFragment.newInstance(uri), EditFragment.class.getName());
        } else if (intent.getBooleanExtra(ACTIVE_DOWNLOAD, false)) {
            replaceFragment(DownloadFragment.newInstance(), DownloadFragment.class.getName());
        }
        setIntent(null);
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
        FirebaseManager.getInstance(this).getShareLink(link -> FirebaseManager.getInstance(this).getShareText(msg -> {
            intent.putExtra(Intent.EXTRA_TEXT, msg + " " + link);
            intent.setType("text/plain");
            startActivity(Intent.createChooser(intent, "Share via..."));
        }));
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

        if (isInstanceof(runningFragment, fragment, StatusFragment.class))
            manager.popBackStack();


        if (!isRunning && runningFragment == null) {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.addToBackStack(tag);
            transaction.replace(R.id.fragment_container, fragment, tag).commit();

        } else if (runningFragment != null) {
            transaction.show(runningFragment).commit();
            if (isInstanceof(runningFragment, fragment, VideoFragment.class)) {
                ((VideoFragment) runningFragment).startProcess(fragment.getArguments() != null ? fragment.getArguments().getString(URL_KEY) : null);
            }
        }
    }

    boolean isInstanceof(Fragment runningFragment, Fragment incomingFragment, Class<?> fragment) {
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

    }


    private void setupBadge(Integer size) {
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

    void safeDismissPopUp() {
        activityViewModel.setProgressDialogState(false, null);
        if (dialog != null)
            dialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        safeDismissPopUp();
        if (circularProgressDialog != null) circularProgressDialog.dismiss();
        circularProgressDialog = null;
        dialog = null;
        super.onDestroy();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Log.d(TAG, "onMenuItemClick: ");
        if (item.getItemId() == R.id.menu_download) {
            Log.d(TAG, "List size:" + activityViewModel.getDownloadDetailsList().size());
            replaceFragment(DownloadFragment.newInstance(), DownloadFragment.class.getName());
        }
        return false;
    }

    @Override
    public void show(String text) {
        activityViewModel.setProgressDialogState(true, text);
        if (dialog == null) dialog = new PopUpDialog(this);
        dialog.show(text);
    }

    @Override
    public void error(String reason, Throwable e) {
        safeDismissPopUp();
//        activityViewModel.setProgressDialogState(true,reason);
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
        Log.e(TAG, "reason" + reason + " error: ", e);


        if (e != null && !BuildConfig.DEBUG) {
            if (AppPref.getInstance(this).getBooleanValue(R.string.key_media_link, true)) {
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
    public void dismiss() {
        activityViewModel.setProgressDialogState(false, null);
        safeDismissPopUp();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_WRITE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (getPath() == null ||
                        (getPath().getScheme().equals("content") && Objects.requireNonNull(DocumentFile.fromTreeUri(getApplicationContext(), getPath())).exists())
                ) {
                    Log.e(TAG, "onRequestPermissionsResult: ");
                    openDirectoryChooser();
                    return;
                }
                download(null);
            } else {
                Toast.makeText(this, "Sorry We can't download", Toast.LENGTH_SHORT).show();
                activityViewModel.tempDetails.clear();
            }
        }
        if (requestCode == Statics.REQUEST_POST_NOTIFICATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                realDownload();
            else {
                Toast.makeText(this, "Unable to start download service", Toast.LENGTH_SHORT).show();
                activityViewModel.tempDetails.clear();
            }
        }
    }


    private void openDirectoryChooser() {
        Log.e(TAG, "openDirectoryChooser: ");
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        dialogBuilder.setTitle("Download Path");

        DialogInterface.OnClickListener positiveListener = (dialog, which) -> storageSwitcher.pick(locationResultLauncher);

        dialogBuilder.setCancelable(false);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            dialogBuilder.setMessage("No path chosen to download. Default path will be\n(Later you can change it from settings)\n\nInternal Storage\\Download");
            dialogBuilder.setPositiveButton("Change", positiveListener);
            dialogBuilder.setNegativeButton("Default", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(MainActivity.this, "Default path selected", Toast.LENGTH_SHORT).show();
                    setPath(null);
                }
            });

        } else {
            dialogBuilder.setMessage("No path chosen to download. Choose path to proceed downloading.\n(Later you can change it from settings)");
            dialogBuilder.setPositiveButton("Choose", positiveListener);
        }
        dialogBuilder.create().show();
    }


    public void agreedTerms() {
        AppPref.getInstance(this).setBooleanValue(R.string.key_terms_con, true);
        if (intent != null) gotIntent(intent);
    }


    private void setPath(Intent data) {

        AppPref.getInstance(this).setSavePath(data);

        for (int i = 0; i < activityViewModel.tempDetails.size(); i++) {
            activityViewModel.tempDetails.get(i).pathUri = getPath();
        }

        storageSwitcher = null;

        download(null);
    }


    private Uri getPath() {
        return AppPref.getInstance(this).getSavePath();
    }


    private String getStringValue(int id, String def) {
        return AppPref.getInstance(this).getStringValue(id, def);
    }

    private void setStringValue(int key, String value) {
        AppPref.getInstance(this).setStringValue(key, value);
    }

    public void download(ArrayList<DownloadDetails> details) {
        if (details != null) {
            if (details.get(0).src == null || details.get(0).videoURL == null || details.get(0).videoSize <= 0) {
                error("Unknown error occurred. Please try again", null);
                return;
            }
            activityViewModel.tempDetails.addAll(details);
        }
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
        // Skip this for loop if you use static module loading of FFmpeg-kit
        for (DownloadDetails d : activityViewModel.tempDetails)
            if ((d.audioURL != null || d.chunkUrl != null) &&
                    FileUtil.isFileNotExists(AppPref.getInstance(this).getCachePath(LIBRARY_PATH) + FFMPEG_VERSION + "lib.zip")
            ) {
                fetchSOFiles(moduleDownloadCallback);
                return;
            }
        if (activityViewModel.tempDetails.get(0).src.equals("WhatsApp")) {
            saveVideos();
        } else {
            realDownload();
        }
    }

    public void saveVideos() {
        new Thread(() -> {
            CountDownLatch countDownLatch = new CountDownLatch(activityViewModel.tempDetails.size());
            for (DownloadDetails details : activityViewModel.tempDetails) {
                Uri uri = FileUtil.pathToNewUri(this, details.pathUri, details.fileName, details.fileMime);
                details.pathUri = uri;
                new Thread(() -> FileUtil.copyFile(this, Uri.parse(details.videoURL), uri, countDownLatch::countDown)).start();
            }
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (DownloadDetails details : activityViewModel.tempDetails) {
                FileUtil.scanMedia(this, details.pathUri.toString(), new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String s, Uri uri) {
                        History history = new History(details, uri);
                        new Thread(() -> HistoryDatabase.getInstance(getApplicationContext()).historyDao().insertItem(history)).start();
                    }
                });

            }
            activityViewModel.tempDetails.clear();
            runOnUiThread(() -> Toast.makeText(this, "Saved...", Toast.LENGTH_SHORT).show());

        }).start();

    }

    public void fetchSOFiles(UtilityInterface.ModuleDownloadCallback callback) {
        final String abi = Build.SUPPORTED_ABIS[0];
        dialog.show("Preparing download");
        HashMap<String, String> abiHashMap = new HashMap<>();
        abiHashMap.put("armeabi-v7a", "https://raw.githubusercontent.com/Udhayarajan/SOserver/master/" + FFMPEG_VERSION + "armeabi-v7a.zip");
        abiHashMap.put("arm64-v8a", "https://raw.githubusercontent.com/Udhayarajan/SOserver/master/" + FFMPEG_VERSION + "arm64-v8a.zip");
        abiHashMap.put("x86", "https://raw.githubusercontent.com/Udhayarajan/SOserver/master/" + FFMPEG_VERSION + "x86.zip");
        abiHashMap.put("x86_64", "https://raw.githubusercontent.com/Udhayarajan/SOserver/master/" + FFMPEG_VERSION + "x86_64.zip");

        new MiniExecute(null).getSize(abiHashMap.get(abi), (size, bundle) -> runOnUiThread(() -> downloadAdditionalModule(size, abiHashMap.get(abi), callback)));

    }


    private void downloadAdditionalModule(long size, String url, UtilityInterface.ModuleDownloadCallback moduleDownloadCallback) {
        safeDismissPopUp();
        if (!activityViewModel.isDownloadingSOFile()) {
            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
            dialogBuilder.setTitle("Additional required");
            String src;
            try {
                src = activityViewModel.tempDetails.get(0).src + " downloader";
            } catch (IndexOutOfBoundsException e) {
                src = "Edit Option";
            }
            dialogBuilder.setMessage(String.format("Additional file (%s) needed to be download to use %s. Would you like to download it ?", UtilityClass.formatFileSize(size, false), src));
            dialogBuilder.setPositiveButton("Download", (dialog, which) -> {
                addDownloadListener();
                activityViewModel.downloadSO(url, () -> {
                    circularProgressDialog.dismiss();
                    circularProgressDialog = null;
                    moduleDownloadCallback.onDownloadEnded();
                });
            });
            dialogBuilder.setNegativeButton("Leave", (dialog, which) -> {
                Toast.makeText(MainActivity.this, "Download aborted", Toast.LENGTH_SHORT).show();
                activityViewModel.tempDetails.clear();
            });
            dialogBuilder.create().show();
        } else {
            addDownloadListener();
        }
    }

    UtilityInterface.ModuleDownloadCallback moduleDownloadCallback = new UtilityInterface.ModuleDownloadCallback() {
        @Override
        public void onDownloadEnded() {
            download(null);
        }
    };


    private void addDownloadListener() {
        circularProgressDialog = new CircularProgressDialog(this);
        activityViewModel.getDownloadProgressLiveData().observe(this, integer -> {
            if (circularProgressDialog != null) circularProgressDialog.setProgress(integer);
        });
        activityViewModel.getDownloadStatusLiveData().observe(this, s -> {
            if (circularProgressDialog != null) circularProgressDialog.setStatusTxt(s);
        });
    }

    boolean isValidDownloadPath() {
        try {
            DocumentFile file = DocumentFile.fromTreeUri(MainActivity.this, getPath());
            if (file != null) return file.exists();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return new File(getPath().getPath()).exists();
        }
        return false;
    }


    private void realDownload() {
        safeDismissPopUp();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, Statics.REQUEST_POST_NOTIFICATION);
                return;
            }
        }
        for (
                DownloadDetails details : activityViewModel.tempDetails) {
            Intent download = detailsToIntent(details);
            if (details.isShareOnlyDownload) {
                addShareOnlyListener();
            }
            ContextCompat.startForegroundService(this, download);
        }

        activityViewModel.tempDetails.clear();
    }

    @Override
    public void onChanged(Download download, @NonNull Reason reason) {
        Log.e(TAG, "onChanged: " + reason);
    }


    Intent detailsToIntent(DownloadDetails details) {

        Intent downloadIntent = new Intent(this, Downloader.class);

        int id = activityViewModel.getUniqueDownloadId();

        details.id = id;

        details.receiver = new DownloadReceiver(new Handler(Looper.getMainLooper()),
                this,
                id, activityViewModel);


        downloadIntent.putExtra(COMMUNICATOR, details);

        activityViewModel.addDownloadDetails(details);

        return downloadIntent;
    }


    @Override
    public void signInNeeded(UtilityClass.LoginDetailsProvider loginDetailsProvider) {

        runOnUiThread(() -> {
            safeDismissPopUp();
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                    .setTitle("Seems to be Private Video!")
                    .setCancelable(false)
                    .setMessage(loginDetailsProvider.getReason())
                    .setPositiveButton("Sign-in", (dialog1, which) -> openWebPage(loginDetailsProvider.getLoginURL(), loginDetailsProvider.getLoginDoneUrl(), null, cookies -> {
                        setStringValue(loginDetailsProvider.getCookiesKey(), cookies);
                        loginDetailsProvider.getIdentifier().loggedIn(cookies);
                    }))
                    .setNegativeButton("No,Thanks", (dialog12, which) -> error("URL invalid or Login Permission Dined", null));
            dialog.show();
        });
    }

    @Override
    public String getCookies(int cookiesKey) {
        return getStringValue(cookiesKey, null);
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

    public void clearHistory(ConfigurationCallback configurationCallback) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Warning")
                .setMessage("Download history will be cleared upon clearing cache.")
                .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setStringValue(R.string.key_clear_history_cache, null);
                        new Thread(() -> HistoryDatabase.getInstance(getApplication()).historyDao().deleteTable()).start();
                        configurationCallback.onProcessDone();
                    }
                })
                .setNegativeButton("Go,Back", null)
                .setCancelable(false)
                .create().show();
    }


    public void logOutInsta(ConfigurationCallback configurationCallback) {
        openWebPage("https://instagram.com/accounts/logout/", new String[]{"https://www.instagram.com/", "https://www.instagram.com/accounts/login/?next=/accounts/logout/", "https://www.instagram.com/accounts/login/?next=%2Faccounts%2Flogout%2F"}, getStringValue(R.string.key_instagram, null), new UtilityInterface.CookiesInterface() {
            @Override
            public void onReceivedCookies(String cookies) {
                Toast.makeText(MainActivity.this, "Logout Successful", Toast.LENGTH_SHORT).show();
                configurationCallback.onProcessDone();
            }
        });
    }

    public void logOutFB(ConfigurationCallback configurationCallback) {
        openWebPage("https://www.facebook.com/help/contact/logout?id=260749603972907",
                new String[]{"https://www.facebook.com/help/contact/260749603972907", "https://m.facebook.com/help/contact/260749603972907"}, getStringValue(R.string.key_facebook, null),
                cookies -> {
                    Toast.makeText(MainActivity.this, "Logout Successful", Toast.LENGTH_SHORT).show();
                    configurationCallback.onProcessDone();
                });
    }

    public void updateShareDownloadProgress(int progress, String msg) {
        if (circularProgressDialog == null) return;
        circularProgressDialog.setProgress(progress);
        Log.e(TAG, "updateShareDownloadProgress: " + msg);
        circularProgressDialog.setStatusTxt(msg);
    }

    private void shareDownloadedVideo(Bundle resultData) {
        if (circularProgressDialog != null) circularProgressDialog.dismiss();
        circularProgressDialog = null;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(resultData.getString(FILE_MIME));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(resultData.getString(OUTFILE_URI)));
        intent.putExtra(VideoSharedBroadcast.DETAILS_ID, resultData.getInt(VideoSharedBroadcast.DETAILS_ID));
        activityViewModel.setTempResultIntent(intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            oneTimeShareManager.addReceiver(intent, UtilityClass.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri.class), null);
        } else {
            oneTimeShareManager.launch(Intent.createChooser(intent, "Choose application to share"));
        }
    }

    private void addShareOnlyListener() {
        int id = activityViewModel.shareOnlyDownloadStatus();
        if (id == MainActivityViewModel.NO_SHARE_ONLY_FILES_DOWNLOADING) {
            return;
        }
        Observer<Bundle> observer = new Observer<Bundle>() {
            @Override
            public void onChanged(Bundle bundle) {
                shareDownloadedVideo(bundle);
                activityViewModel.removeShareOnlyDownloadListener(this);
            }
        };
        activityViewModel.getShareOnlyDownloadLiveData().observe(this, observer);
        circularProgressDialog = new CircularProgressDialog(this);
        ((DownloadReceiver) DownloadDetails.findDetails(id).receiver).getResultBundle().observe(this, bundle -> {
            String msg;
            int resultCode = bundle.getInt(RESULT_CODE);
            if (resultCode == PROGRESS_UPDATE_AUDIO) msg = "Downloading Audio";
            else if (resultCode == PROGRESS_UPDATE_VIDEO) msg = "Downloading Video";
            else if (resultCode == PROGRESS_UPDATE_MERGING) msg = "Merging";
            else if (resultCode == PROGRESS_UPDATE_RECODE_AUDIO) msg = "Recoding audio";
            else if (resultCode == PROGRESS_UPDATE_RECODE_VIDEO) msg = "Recoding video";
            else msg = bundle.getString(FETCH_MESSAGE);
            updateShareDownloadProgress(bundle.getInt(PROGRESS), msg);
        });
    }
}