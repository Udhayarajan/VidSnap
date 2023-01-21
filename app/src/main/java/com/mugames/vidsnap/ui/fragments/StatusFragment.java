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

package com.mugames.vidsnap.ui.fragments;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static com.mugames.vidsnap.utility.Statics.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.extractor.status.WhatsApp;
import com.mugames.vidsnap.network.HttpRequest;
import com.mugames.vidsnap.network.Response;
import com.mugames.vidsnap.storage.AppPref;
import com.mugames.vidsnap.ui.activities.MainActivity;
import com.mugames.vidsnap.ui.adapters.DownloadableAdapter;
import com.mugames.vidsnap.ui.adapters.SingleInstagramUserStoryProfileAdapter;
import com.mugames.vidsnap.ui.viewmodels.StatusFragmentViewModel;
import com.mugames.vidsnap.utility.InstagramReelsTrayResponseModel;
import com.mugames.vidsnap.utility.User;
import com.mugames.vidsnap.utility.UtilityClass;
import com.mugames.vidsnap.utility.bundles.DownloadDetails;
import com.mugames.vidsnapkit.JsonExtKt;
import com.mugames.vidsnapkit.dataholders.Result;
import com.mugames.vidsnapkit.extractor.Instagram;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A fragment that is opened when user selected status from {@link HomeFragment}
 */
public class StatusFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    LinearLayout urlLayout;
    Button saveButton;

    TextInputEditText urlEditText;
    Button statusAnalyse;

    StatusFragmentViewModel viewModel;

    DownloadableAdapter adapter;
    RecyclerView recyclerView;

    ActivityResultLauncher<String> permissionResult;
    ActivityResultLauncher<Intent> permissionLauncher;
    Spinner socialMediaSpinner;

    Stack<RecyclerView.Adapter<RecyclerView.ViewHolder>> adaptersStack = new Stack<>();

    boolean isAnApiRequestToReelsTray = false;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public StatusFragment() {
    }

    public static StatusFragment newInstance() {
        return new StatusFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionResult = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result) viewModel.searchForStatus(null, (MainActivity) getActivity());
            else {
                Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                socialMediaSpinner.setSelection(0);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    viewModel.searchForStatus(null, (MainActivity) getActivity());
                    if (result.getData() != null) {
                        AppPref.getInstance(requireContext()).setWhatsAppUri(result.getData());
                    }
                }
            });
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status_list, container, false);

        viewModel = new ViewModelProvider(this).get(StatusFragmentViewModel.class);
        viewModel.getFormatsLiveData().observe(getViewLifecycleOwner(), formats -> onAnalyzeCompleted());
        viewModel.getFailedResultLiveData().observe(getViewLifecycleOwner(), this::handleFailedResult);

        Context context = view.getContext();
        recyclerView = view.findViewById(R.id.status_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 2));

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (adaptersStack.size() <= 1) {
                    setEnabled(false);
                    requireActivity().onBackPressed();
                    return;
                }
                RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = adaptersStack.pop();
                recyclerView.setAdapter(adapter);
            }
        });

        socialMediaSpinner = view.findViewById(R.id.status_media);
        ArrayAdapter<CharSequence> mediaAdapter = ArrayAdapter.createFromResource(context, R.array.social_media_type, android.R.layout.simple_spinner_item);
        mediaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        socialMediaSpinner.setAdapter(mediaAdapter);
        socialMediaSpinner.setOnItemSelectedListener(this);

        urlLayout = view.findViewById(R.id.status_url_getter);

        saveButton = view.findViewById(R.id.card_selected);
        urlEditText = view.findViewById(R.id.status_url);
        statusAnalyse = view.findViewById(R.id.status_analysis);

        saveButton.setOnClickListener(v -> new Thread(this::save).start());

        statusAnalyse.setOnClickListener(v -> viewModel.downloadInstagramStory(urlEditText.getText().toString(), AppPref.getInstance(requireContext()).getStringValue(R.string.key_instagram, null)));

        return view;
    }

    private void handleFailedResult(Result.Failed failed) {
        ((MainActivity) requireActivity()).error(
                failed + "\nReason: " + failed.getError().getMessage(), failed.getError().getE()
        );
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        urlLayout.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);
        adaptersStack.clear();
        if (position == 1) {
            checkPermissionForWhatsApp();
        } else if (position == 2) {
            isAnApiRequestToReelsTray = false;
            checkCookiesOfInstagram();
        }
    }

    private void checkCookiesOfInstagram() {
        String cookies = ((MainActivity) requireActivity()).getCookies(R.string.key_instagram);
        if (cookies == null) {
            socialMediaSpinner.setSelection(0);
            urlLayout.setVisibility(View.GONE);
            ((MainActivity) requireActivity()).signInNeeded(new UtilityClass.LoginDetailsProvider("Story download requires login", "https://www.instagram.com/accounts/login/", new String[]{"https://www.instagram.com/"}, R.string.key_instagram, cookies1 -> {
                socialMediaSpinner.setSelection(2);
            }));
        } else {
            urlLayout.setVisibility(View.VISIBLE);
            instaLoggedIn(cookies, false);
        }
    }

    private void instaLoggedIn(String cookies, boolean useApiRequest) {
        ((MainActivity) requireActivity()).show("Please wait....");
        Hashtable<String, String> header = new Hashtable<>();
        header.put("Cookie", cookies);
        header.put("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 12_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Instagram 105.0.0.11.118 (iPhone11,8; iOS 12_3_1; en_US; en-US; scale=2.00; 828x1792; 165586599)");
        new HttpRequest(
                (MainActivity) requireActivity(),
                useApiRequest ? "https://i.instagram.com/api/v1/feed/reels_tray/" : "https://www.instagram.com",
                null, header, HttpRequest.GET, null,
                cookies1 -> {
                    requireActivity().runOnUiThread(() -> instaReelsTrayData(cookies1));
                }
        ).start();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void instaReelsTrayData(@NonNull Response response) {
        ((MainActivity) requireActivity()).dismiss();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            String html = response.getResponse();
            String content = null;
            if (!isAnApiRequestToReelsTray) {
                Matcher contentMatcher = Pattern.compile("handleWithCustomApplyEach\\(ScheduledApplyEach,(\\{\"req.*)\\);\\}\\);\\}\\);").matcher(String.valueOf(html));
                if (contentMatcher.find()) {
                    content = contentMatcher.group(1);
                }

                if (content == null && !isAnApiRequestToReelsTray) {
                    isAnApiRequestToReelsTray = true;
                    instaLoggedIn(((MainActivity) requireActivity()).getCookies(R.string.key_instagram), true);
                    return;
                }
                JSONObject jsonObject = new JSONObject(content);
                JSONArray jsonArray = jsonObject.getJSONArray("require").getJSONArray(0);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONArray array = UtilityClass.JSONGetter.getArray_or_Null(jsonArray, i);
                    if (array != null && (jsonObject = JsonExtKt.getNullableJSONObject(array, 0)) != null) {
                        jsonObject = jsonObject.getJSONObject("data").getJSONObject("__bbox");
                        if (jsonObject.getJSONObject("request").getString("url").equals("/api/v1/feed/reels_tray/")) {
                            content = jsonObject.getJSONObject("result").getString("response");
                            break;
                        }
                    }
                }
            } else content = html;
            if (html == null) {
                AppPref.getInstance(requireContext()).setStringValue(R.string.key_instagram, null);
                checkCookiesOfInstagram();
                return;
            }
            Log.d(TAG, "instaReelsTrayData: " + content);
            InstagramReelsTrayResponseModel object = objectMapper.readValue(content, InstagramReelsTrayResponseModel.class);
            RecyclerView.Adapter adapter = new SingleInstagramUserStoryProfileAdapter(object, user -> {
                if (user == null) {
                    Snackbar.make(requireView(), "Special Event Stories are not able to be downloaded", Snackbar.LENGTH_LONG).show();
                    return null;
                }
                profileClicked(user);
                return null;
            }, user -> {
                profileLongClicked(user);
                return null;
            });
            adaptersStack.push(recyclerView.getAdapter());
            recyclerView.setAdapter(adapter);
        } catch (JsonProcessingException | JSONException exception) {
            Log.e(TAG, "instaReelsTrayData: ", exception);
        }
    }

    void profileClicked(User user) {
        ((MainActivity) requireActivity()).show("@" + user.getUsername());
        viewModel.downloadInstagramStory(String.format("https://www.instagram.com/%s/", user.getUsername()), ((MainActivity) requireActivity()).getCookies(R.string.key_instagram));
    }

    void profileLongClicked(@Nullable User user) {

    }

    void checkPermissionForWhatsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (AppPref.getInstance(requireContext()).getWhatsAppUri() == null)
                getPermissionRandAbove();
            else viewModel.searchForStatus(null, (MainActivity) getActivity());
        } else if (ContextCompat.checkSelfPermission(requireContext(), READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            permissionResult.launch(READ_EXTERNAL_STORAGE);
        else viewModel.searchForStatus(null, (MainActivity) getActivity());
    }


    @RequiresApi(api = Build.VERSION_CODES.R)
    private void getPermissionRandAbove() {
        Intent intent = ((StorageManager) requireContext().getSystemService(Context.STORAGE_SERVICE)).getPrimaryStorageVolume().createOpenDocumentTreeIntent();
        Uri uri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:" + WhatsApp.WHATSAPP + "/WhatsApp/" + WhatsApp.SUFFIX_PATH);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext());
        dialogBuilder.setMessage("Your device version requires special storage permission to access WhatsApp's folder. Allow us to proceed towards Status Saving\n\nClick USE THIS FOLDER->ALLOW from prompt").setTitle("Permission Required!").setPositiveButton("Open", (dialogInterface, i) -> permissionLauncher.launch(intent)).setNegativeButton("Cancel", (dialogInterface, i) -> {
            Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
            socialMediaSpinner.setSelection(0);
        }).create().show();
    }


    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onAnalyzeCompleted() {
        ((MainActivity) requireActivity()).dismiss();
        RecyclerView.Adapter adapter = new DownloadableAdapter(this, viewModel.getFormats());
        this.adapter = (DownloadableAdapter) adapter;
        this.adapter.getSelectedList().observe(getViewLifecycleOwner(), this::selectedListChanged);
        adaptersStack.push(recyclerView.getAdapter());
        recyclerView.setAdapter(adapter);
    }

    private void selectedListChanged(ArrayList<Integer> selectedValue) {
        viewModel.setSelectedList(selectedValue);
        if (selectedValue.size() > 0) {
            saveButton.setVisibility(View.VISIBLE);
            saveButton.setText(String.format("SAVE (%s of %s)", selectedValue.size(), viewModel.getFormats().getFileCount()));
        } else {
            saveButton.setVisibility(View.GONE);
        }
    }

    private void save() {
        ArrayList<DownloadDetails> downloadDetails = new ArrayList<>();
        for (int i = 0; i < viewModel.getSelectedList().size(); i++) {
            int index = viewModel.getSelectedList().get(i);
            DownloadDetails details = new DownloadDetails();
            details.videoSize = viewModel.getFormats().videoSizes.get(index);
            details.videoURL = viewModel.getFormats().mainFileURLs.get(index);
            details.src = viewModel.getFormats().src;
            details.fileMime = viewModel.getFormats().fileMime.get(index);
            details.fileType = MimeTypeMap.getSingleton().getExtensionFromMimeType(details.fileMime);
            if (viewModel.getFormats().title == null)
                details.fileName = getFileName(Uri.parse(details.videoURL));
            else {
                //TODO Add name property for non-whatsapp status
            }
            details.pathUri = AppPref.getInstance(getContext()).getSavePath();
            FutureTarget<Bitmap> target = Glide.with(requireActivity().getApplicationContext()).asBitmap().load(viewModel.getFormats().thumbNailsURL.get(index)).submit();

            try {
                details.setThumbNail(getContext(), target.get());
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "save: ", e);
            }
            downloadDetails.add(details);
        }
        requireActivity().runOnUiThread(() -> {
            ((MainActivity) requireActivity()).download(downloadDetails);
            adapter.clearSelection();
        });
    }

    private String getFileName(Uri uri) {
        String name = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = requireActivity().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst())
                    name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                cursor.close();
            }
        } else if (uri.getScheme().equals("file")) name = uri.getLastPathSegment();
        return name.split("\\.")[0];
    }

}