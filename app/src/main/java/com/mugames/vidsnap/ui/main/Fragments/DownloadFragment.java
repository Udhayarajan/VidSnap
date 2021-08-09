package com.mugames.vidsnap.ui.main.Fragments;

import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Utility.Bundles.DownloadDetails;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.ViewModels.DownloadViewModel;
import com.mugames.vidsnap.ViewModels.MainActivityViewModel;
import com.mugames.vidsnap.ui.main.Adapters.DownloadAdapter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;


public class DownloadFragment extends Fragment {

    String TAG = Statics.TAG + ":DownloadFragment";


    MainActivityViewModel activityViewModel;
    DownloadViewModel downloadViewModel;


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
        activityViewModel = new ViewModelProvider(getActivity()).get(MainActivityViewModel.class);
        downloadViewModel = new ViewModelProvider(this).get(DownloadViewModel.class);

        Log.d(TAG, "List size:"+activityViewModel.getDownloadDetailsList().size());

        view = inflater.inflate(R.layout.fragment_downloading, container, false);

        RecyclerView recyclerView;
        DownloadAdapter adapter;

        recyclerView = view.findViewById(R.id.download_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DownloadAdapter(activityViewModel, downloadViewModel, getViewLifecycleOwner());

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