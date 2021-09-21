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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mugames.vidsnap.R;

/**
 * Will be removed in future
 */
public class NoRecordFragment extends Fragment {

    private static final String REASON = "param1";

    private String reason;

    //TODO:Add this when there is no Active Download or History is empty

    public NoRecordFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param reason Reason for this Fragment
     * @return A new instance of fragment NoRecordFragment.
     */

    public static NoRecordFragment newInstance(String reason) {
        NoRecordFragment fragment = new NoRecordFragment();
        Bundle args = new Bundle();
        args.putString(REASON, reason);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            reason = getArguments().getString(REASON);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_no_record, container, false);
        ((TextView) view.findViewById(R.id.error_reason)).setText(reason);
        view.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.notification_color));
        return view;
    }
}