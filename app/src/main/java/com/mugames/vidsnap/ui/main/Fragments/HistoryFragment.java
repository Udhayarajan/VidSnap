package com.mugames.vidsnap.ui.main.Fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mugames.vidsnap.DataBase.History;
import com.mugames.vidsnap.ViewModels.HistoryViewModel;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.ui.main.Adapters.HistoryRecyclerViewAdapter;

import java.util.List;


public class HistoryFragment extends Fragment {

    String TAG = Statics.TAG + ":HistoryFragment";

    HistoryViewModel historyViewModel;


    public HistoryFragment() {
    }

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        historyViewModel = new ViewModelProvider(this,ViewModelProvider.AndroidViewModelFactory.getInstance(getActivity().getApplication())).get(HistoryViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view;
        HistoryRecyclerViewAdapter adapter = new HistoryRecyclerViewAdapter(getViewLifecycleOwner(),historyViewModel);
        view = inflater.inflate(R.layout.fragment_history, container, false);
        Context context = view.getContext();
        RecyclerView recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.list_background));
        historyViewModel.getAllValues().observe(getViewLifecycleOwner(), new Observer<List<History>>() {
            @Override
            public void onChanged(List<History> histories) {
                adapter.submitList(histories);
                recyclerView.setAdapter(adapter);
            }
        });
//        historyViewModel.getAllValues().observe(getViewLifecycleOwner(), new Observer<List<History>>() {
//            @Override
//            public void onChanged(List<History> histories) {
////                if(!histories.isEmpty()){
////
////                }else {
////                    FragmentManager manager = getActivity().getSupportFragmentManager();
////                    FragmentTransaction trans = manager.beginTransaction();
////                    trans.remove(HistoryFragment.this );
////                    trans.commit();
////                    manager.popBackStack();
////                    ((MainActivity)getActivity()).replaceFragment(NoRecordFragment.newInstance("There is no download done yet"),NoRecordFragment.class.getName());
////                }
//            }
//        });
        return view;
    }

//    @Override
//    public void onSaveInstanceState(@NonNull Bundle outState) {
//        super.onSaveInstanceState(outState);
////        outState.putParcelableArrayList(LIST_HISTORY,list);
//    }
}