package com.mugames.vidsnap.ui.main.Fragments;

import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mugames.vidsnap.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NoRecordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NoRecordFragment extends Fragment {

    private static final String REASON = "param1";

    private String reason;

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