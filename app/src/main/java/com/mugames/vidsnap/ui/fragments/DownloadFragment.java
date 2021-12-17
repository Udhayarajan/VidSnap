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

import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.mugames.vidsnap.R;
import com.mugames.vidsnap.utility.bundles.DownloadDetails;
import com.mugames.vidsnap.utility.Statics;
import com.mugames.vidsnap.ui.viewmodels.DownloadViewModel;
import com.mugames.vidsnap.ui.viewmodels.MainActivityViewModel;
import com.mugames.vidsnap.ui.adapters.DownloadAdapter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;


/**
 * It keeps track of active downloading task
 * Usually displaced when right top corner download icon is clicked
 */

public class DownloadFragment extends Fragment {

    String TAG = Statics.TAG + ":DownloadFragment";


    MainActivityViewModel activityViewModel;


    public DownloadFragment() {
    }


    public static DownloadFragment newInstance() {
        return new DownloadFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view;
        activityViewModel = new ViewModelProvider(getActivity(),ViewModelProvider.AndroidViewModelFactory.getInstance(getActivity().getApplication())).get(MainActivityViewModel.class);

        view = inflater.inflate(R.layout.fragment_downloading, container, false);

        RecyclerView recyclerView;
        DownloadAdapter adapter;

        recyclerView = view.findViewById(R.id.download_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DownloadAdapter( this);

        activityViewModel.getDownloadDetailsLiveData().observe(getViewLifecycleOwner(), new Observer<ArrayList<DownloadDetails>>() {
            @Override
            public void onChanged(ArrayList<DownloadDetails> downloadDetails) {
                adapter.submitList(downloadDetails);
                recyclerView.setAdapter(adapter);
            }
        });
        if (!activityViewModel.getDownloadDetailsList().isEmpty()) {
            recyclerView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.list_background));
        }
        adapter.submitList(activityViewModel.getDownloadDetailsList());
        recyclerView.setAdapter(adapter);


        return view;
    }

}