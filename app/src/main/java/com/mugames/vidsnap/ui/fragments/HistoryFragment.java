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

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view;
        HistoryRecyclerViewAdapter adapter = new HistoryRecyclerViewAdapter(getViewLifecycleOwner(), historyViewModel);
        view = inflater.inflate(R.layout.fragment_history, container, false);
        Context context = view.getContext();
        RecyclerView recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.list_background));
        recyclerView.setAdapter(adapter);
        historyViewModel.getAllValues().observe(getViewLifecycleOwner(), new Observer<List<History>>() {
            @Override
            public void onChanged(List<History> histories) {
                adapter.submitList(histories);
            }
        });

        historyViewModel.getIntentSender().observe(getViewLifecycleOwner(), intentSender -> {
            if (intentSender != null) {
                deleteLauncher.launch(new IntentSenderRequest.Builder(intentSender).build());
            }
        });

        return view;
    }

}