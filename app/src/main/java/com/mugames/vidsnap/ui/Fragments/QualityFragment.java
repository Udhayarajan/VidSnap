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

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.Utility.UtilityInterface;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * <p>A fragment that shows a list of items as a modal bottom sheet.</p>
 * It offers users to select their video quality options
 */

public class QualityFragment extends BottomSheetDialogFragment {
    String TAG = Statics.TAG + ":QualityFragment";

    ArrayList<String> qualityList;
    ArrayList<String> sizesList;

    int selectedItem = -1;
    Bitmap thumbNail;
    EditText editText;

    private UtilityInterface.DownloadButtonCallBack callback;


    public static QualityFragment newInstance(ArrayList<String> qualitiesList, ArrayList<String> sizes) {
        final QualityFragment fragment = new QualityFragment();
        fragment.qualityList = qualitiesList;
        fragment.sizesList = sizes;
        return fragment;
    }

    public void setRequired(UtilityInterface.DownloadButtonCallBack callBack) {
        this.callback = callBack;
    }

    public void setThumbNail(Bitmap thumbNail) {
        this.thumbNail = thumbNail;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quality_list_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        final RecyclerView recyclerView = view.findViewById(R.id.formats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        qualityAdapter adapter = new qualityAdapter(qualityList.size(), qualityList, sizesList);

        recyclerView.setAdapter(adapter);
        Button download = view.findViewById(R.id.download);

        ImageView imageView = view.findViewById(R.id.thumbnail_img);
        imageView.setImageBitmap(thumbNail);
        editText = view.findViewById(R.id.edit_name);

        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                callback.onDownloadButtonPressed(editText.getText().toString(),bitmap);
                QualityFragment.this.dismiss();
            }
        });

    }



    public void setName(String name) {
        editText.setText(name);
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialog;
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        final TextView qualityLabel;
        final TextView sizeText;
        final RadioButton radioButton;

        ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.fragment_quality_list_dialog_item, parent, false));
            qualityLabel = itemView.findViewById(R.id.qualityLabel);
            sizeText = itemView.findViewById(R.id.size);
            radioButton = itemView.findViewById(R.id.radioButton);
        }
    }

    private class qualityAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final int mItemCount;
        final ArrayList<String> mQualities;
        final ArrayList<String> mSizes;
        ArrayList<RadioButton> radioButtons = new ArrayList<>();

        qualityAdapter(int itemCount, ArrayList<String> qualities, ArrayList<String> sizes) {
            mItemCount = itemCount;
            mQualities = qualities;
            mSizes = sizes;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.itemView.setTag(position);
            holder.radioButton.setTag(position);
            holder.qualityLabel.setText(mQualities.get(position));
            holder.sizeText.setText(String.format("%s MB", mSizes.get(position)));

            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    itemCheckChanged(v);
                }
            };

            holder.itemView.setOnClickListener(listener);

            if (radioButtons.size() == 0) {
                holder.radioButton.setChecked(true);
                callback.onSelectedItem(0, QualityFragment.this);
                selectedItem = 0;
            } else holder.radioButton.setChecked(false);

            holder.radioButton.setOnClickListener(listener);

            Object test = null;

            try {
                test = radioButtons.get(position);
            } catch (IndexOutOfBoundsException e) {
            }
            if (test == null)
                radioButtons.add(holder.radioButton);
            else {
                if (position == selectedItem)
                    radioButtons.get(position).setChecked(true);
                radioButtons.set(position, holder.radioButton);
            }
        }


        @Override
        public int getItemCount() {
            return mItemCount;
        }

        void itemCheckChanged(View v) {
            selectedItem = (int) v.getTag();
            for (RadioButton radioButton : radioButtons) {
                radioButton.setChecked(false);
            }
            radioButtons.get(selectedItem).setChecked(true);
            callback.onSelectedItem(selectedItem, QualityFragment.this);
        }
    }
}