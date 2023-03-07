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

package com.mugames.vidsnap.ui.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mugames.vidsnap.database.History;
import com.mugames.vidsnap.databinding.FragmentHistoryBinding;
import com.mugames.vidsnap.ui.activities.MainActivity;
import com.mugames.vidsnap.ui.viewmodels.HistoryViewModel;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.utility.Statics;
import com.mugames.vidsnap.ui.adapters.HistoryRecyclerViewAdapter;

import java.util.List;

/**
 * This fragment keeps track of Download history data
 * Usually get displayed when Menu->Downloads is clicked
 */
public class HistoryFragment extends Fragment {

    String TAG = Statics.TAG + ":HistoryFragment";

    HistoryViewModel historyViewModel;
    ActivityResultLauncher<IntentSenderRequest> deleteLauncher;
    FragmentHistoryBinding binding;

    HistoryRecyclerViewAdapter adapter;

    private ActivityResultLauncher<String> readPermissionResultLauncher;

    public HistoryFragment() {
    }

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        historyViewModel = new ViewModelProvider(this,
                (ViewModelProvider.Factory) ViewModelProvider
                        .AndroidViewModelFactory
                        .getInstance(requireActivity().getApplication())
        ).get(HistoryViewModel.class);

        deleteLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), (ActivityResultCallback<ActivityResult>) result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                historyViewModel.deletePendingUri();
            } else Toast.makeText(requireContext(), "Unable to delete", Toast.LENGTH_SHORT).show();
        });

        readPermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result) {
                loadData();
            } else {
                Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                requireActivity().onBackPressed();
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        adapter = new HistoryRecyclerViewAdapter(getViewLifecycleOwner(), historyViewModel);

        Context context = view.getContext();
        RecyclerView recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.list_background));
        recyclerView.setAdapter(adapter);
        binding.loadingIndicator.show();
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            readPermissionResultLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            loadData();
        }

        historyViewModel.getIntentSender().observe(getViewLifecycleOwner(), intentSender -> {
            if (intentSender != null) {
                deleteLauncher.launch(new IntentSenderRequest.Builder(intentSender).build());
            }
        });
        swapViewVisibility(false);
        binding.noRecordContainer.errorReason.setText("No old downloads found");
        return view;
    }

    private void swapViewVisibility(boolean hasElement) {
        if (hasElement) {
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.recyclerView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.list_background));
            binding.noRecordContainer.noRecordFragmentParent.setVisibility(View.GONE);
        } else {
            binding.recyclerView.setVisibility(View.GONE);
            binding.noRecordContainer.noRecordFragmentParent.setVisibility(View.VISIBLE);
            binding.getRoot().setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.notification_color));
        }
    }

    private void loadData() {
        historyViewModel.getAllValues().observe(getViewLifecycleOwner(), new Observer<List<History>>() {
            @Override
            public void onChanged(List<History> histories) {
                binding.loadingIndicator.hide();
                swapViewVisibility(!histories.isEmpty());
                adapter.submitList(histories);
            }
        });
    }
}