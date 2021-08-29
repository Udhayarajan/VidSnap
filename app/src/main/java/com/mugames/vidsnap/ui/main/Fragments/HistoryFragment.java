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