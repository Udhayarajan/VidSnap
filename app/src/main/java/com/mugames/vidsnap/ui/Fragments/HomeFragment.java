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

package com.mugames.vidsnap.ui.Fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mugames.vidsnap.ui.Activities.MainActivity;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Utility.Statics;

/**
 * This fragment contains button like Video, Edit, Image, Status
 */
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
        switch (v.getId()){
            case R.id.button_video:
                activity.replaceFragment(VideoFragment.newInstance(null),VideoFragment.class.getName());
                break;
            case R.id.button_status:
                activity.replaceFragment(StatusFragment.newInstance(),StatusFragment.class.getName());
                break;
            case R.id.button_edit:
            case R.id.button_pic:
                Toast.makeText(getActivity(),"Coming Soon",Toast.LENGTH_SHORT).show();
        }
    }
}