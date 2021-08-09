package com.mugames.vidsnap.ui.main.Fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mugames.vidsnap.ui.main.Activities.MainActivity;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Utility.Statics;


public class HomeFragment extends Fragment implements View.OnClickListener {

    String TAG= Statics.TAG+":HomeFragment";
    MainActivity activity;
    public HomeFragment() {
        // Required empty public constructor
    }


    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();

        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v= inflater.inflate(R.layout.fragment_home, container, false);
        activity= (MainActivity) getActivity();
        v.findViewById(R.id.button_video).setOnClickListener(this);
        v.findViewById(R.id.button_edit).setOnClickListener(this);
        v.findViewById(R.id.button_pic).setOnClickListener(this);
        v.findViewById(R.id.button_status).setOnClickListener(this);


        return v;
    }

    @Override
    public void onClick(View v) {
//        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        switch (v.getId()){
            case R.id.button_video:
                activity.replaceFragment(VideoFragment.newInstance(null),VideoFragment.class.getName());
                break;
            case R.id.button_edit:
            case R.id.button_pic:
            case R.id.button_status:
                Toast.makeText(getActivity(),"Coming Soon",Toast.LENGTH_SHORT).show();
        }
//        transaction.commit();
    }
}