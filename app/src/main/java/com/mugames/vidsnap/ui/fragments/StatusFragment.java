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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.provider.Settings;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.extractor.status.WhatsApp;
import com.mugames.vidsnap.storage.AppPref;
import com.mugames.vidsnap.storage.FileUtil;
import com.mugames.vidsnap.utility.bundles.DownloadDetails;
import com.mugames.vidsnap.utility.UtilityInterface;
import com.mugames.vidsnap.ui.activities.MainActivity;
import com.mugames.vidsnap.ui.adapters.DownloadableAdapter;
import com.mugames.vidsnap.ui.viewmodels.StatusFragmentViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * A fragment that is opened when user selected status from {@link HomeFragment}
 */
public class StatusFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    LinearLayout urlLayout;
    Button saveButton;

    StatusFragmentViewModel viewModel;

    DownloadableAdapter adapter;
    RecyclerView recyclerView;

    ActivityResultLauncher<String> permissionResult;
    ActivityResultLauncher<Intent> permissionLauncher;
    Spinner socialMediaSpinner;


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
        permissionResult = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                result -> {
                    if (result)
                        viewModel.searchForStatus(null, (MainActivity) getActivity());
                    else{
                        Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                        socialMediaSpinner.setSelection(0);
                    }
                }
        );

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissionLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if(result.getResultCode()== Activity.RESULT_OK){
                            viewModel.searchForStatus(null, (MainActivity) getActivity());
                            if (result.getData() != null) {
                                AppPref.getInstance(requireContext()).setWhatsAppUri(result.getData());
                            }
                        }
                    }
            );
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status_list, container, false);

        viewModel = new ViewModelProvider(this).get(StatusFragmentViewModel.class);
        viewModel.getFormatsLiveData().observe(getViewLifecycleOwner(),formats -> onAnalyzeCompleted());

        Context context = view.getContext();
        recyclerView = view.findViewById(R.id.status_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 2));

        socialMediaSpinner = view.findViewById(R.id.status_media);
        ArrayAdapter<CharSequence> mediaAdapter = ArrayAdapter.createFromResource(context, R.array.social_media_type, android.R.layout.simple_spinner_item);
        mediaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        socialMediaSpinner.setAdapter(mediaAdapter);
        socialMediaSpinner.setOnItemSelectedListener(this);

        urlLayout = view.findViewById(R.id.status_url_getter);
        urlLayout.setVisibility(View.GONE);

        saveButton = view.findViewById(R.id.card_selected);

        saveButton.setOnClickListener(v -> new Thread(this::save).start());

        return view;
    }


    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        Log.d("TAG", "onItemSelected: " + position);
        if (position == 0) {
            urlLayout.setVisibility(View.GONE);
            saveButton.setVisibility(View.GONE);
        } else if (position == 1) {
            urlLayout.setVisibility(View.GONE);
            saveButton.setVisibility(View.GONE);
            checkPermissionForWhatsApp();
        } else if (position ==2){}else {
            urlLayout.setVisibility(View.VISIBLE);
        }
    }

    void checkPermissionForWhatsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (AppPref.getInstance(requireContext()).getWhatsAppUri()==null)
                getPermissionRandAbove();
            else viewModel.searchForStatus(null, (MainActivity) getActivity());
        } else if (ContextCompat.checkSelfPermission(requireContext(), READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            permissionResult.launch(READ_EXTERNAL_STORAGE);
        else viewModel.searchForStatus(null, (MainActivity) getActivity());
    }


    @RequiresApi(api = Build.VERSION_CODES.R)
    private void getPermissionRandAbove() {
        Intent intent = ((StorageManager)requireContext().getSystemService(Context.STORAGE_SERVICE)).getPrimaryStorageVolume().createOpenDocumentTreeIntent();
        Uri uri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents","primary:"+WhatsApp.WHATSAPP+"/WhatsApp/"+WhatsApp.SUFFIX_PATH);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,uri);
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext());
        dialogBuilder.setMessage("Your device version requires special storage permission to access WhatsApp's folder. Allow us to proceed towards Status Saving\n\nClick USE THIS FOLDER->ALLOW from prompt")
                .setTitle("Permission Required!")
                .setPositiveButton("Open", (dialogInterface, i) -> permissionLauncher.launch(intent))
                .setNegativeButton("Cancel", (dialogInterface, i) -> {
                    Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    socialMediaSpinner.setSelection(0);
                })
                .create().show();
    }


    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public void onAnalyzeCompleted() {
        adapter = new DownloadableAdapter(this, viewModel.getFormats());
        adapter.getSelectedList().observe(getViewLifecycleOwner(), this::selectedListChanged);
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

    private void save(){
        ArrayList<DownloadDetails> downloadDetails = new ArrayList<>();
        for (int i = 0; i < viewModel.getSelectedList().size(); i++) {
            int index = viewModel.getSelectedList().get(i);
            DownloadDetails details = new DownloadDetails();
            details.videoSize = viewModel.getFormats().videoSizes.get(index);
            details.videoURL = viewModel.getFormats().mainFileURLs.get(index);
            details.src = viewModel.getFormats().src;
            details.fileMime = viewModel.getFormats().fileMime.get(index);
            details.fileType = MimeTypeMap.getSingleton().getExtensionFromMimeType(details.fileMime);
            if(viewModel.getFormats().title==null) details.fileName = getFileName(Uri.parse(details.videoURL));
            else {
                //TODO Add name property for non-whatsapp status
            }
            details.pathUri = AppPref.getInstance(getContext()).getSavePath();
            FutureTarget<Bitmap> target = Glide.with(requireActivity().getApplicationContext())
                    .asBitmap()
                    .load(viewModel.getFormats().thumbNailsURL.get(index))
                    .submit();

            try {
                details.setThumbNail(getContext(), target.get());
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "save: ", e );
            }
            downloadDetails.add(details);
        }
        requireActivity().runOnUiThread(()-> {
            ((MainActivity) requireActivity()).download(downloadDetails);
            adapter.clearSelection();
        });
    }

    private String getFileName(Uri uri) {
        String name = null;
        if(uri.getScheme().equals("content")){
            Cursor cursor = requireActivity().getContentResolver().query(uri,null,null,null,null);
            if(cursor!=null) {
                if (cursor.moveToFirst())
                    name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                cursor.close();
            }
        }
        else if(uri.getScheme().equals("file")) name = uri.getLastPathSegment();
        return name.split("\\.")[0];
    }


}