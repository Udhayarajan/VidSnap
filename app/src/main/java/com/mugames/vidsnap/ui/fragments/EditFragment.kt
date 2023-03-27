/*
 *  This file is part of VidSnap.
 *
 *  VidSnap is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *  VidSnap is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with VidSnap.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.mugames.vidsnap.ui.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.mugames.vidsnap.R
import com.mugames.vidsnap.postprocessor.FFMPEG
import com.mugames.vidsnap.storage.AppPref
import com.mugames.vidsnap.storage.FileUtil
import com.mugames.vidsnap.ui.activities.MainActivity
import com.mugames.vidsnap.ui.viewmodels.EditFragmentViewModel
import com.mugames.vidsnap.ui.viewmodels.EmptyIntent
import com.mugames.vidsnap.ui.viewmodels.MainActivityViewModel
import com.mugames.vidsnap.utility.OneTimeShareManager
import com.mugames.vidsnap.utility.Statics.TAG
import com.mugames.vidsnap.utility.UtilityClass
import kotlinx.coroutines.launch


private const val ARG_PARAM1 = "param1"

/**
 * A simple [Fragment] subclass.
 * Use the [EditFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EditFragment : Fragment() {
    private val viewModel: EditFragmentViewModel by activityViewModels()
    private lateinit var activityViewModel: MainActivityViewModel

    private lateinit var oneTimeShareManager: OneTimeShareManager

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val intent = it.data
            intent?.data.also { uri ->
                Log.d(TAG, ": $uri")
                uri?.let { it1 ->
                    proceedWithUri(it1)
                } ?: run {
                    view?.let { v -> Snackbar.make(v, "Uri is null", Snackbar.LENGTH_LONG).show() }
                }
            }
        }

    private fun proceedWithUri(uri: Uri) {
        val fileUri = FileUtil.getPathFromContentUri(this.context, uri)?.toUri()
            ?: FileUtil.getPathFromProviderUri(context, uri)?.toUri()
            ?: return
        Log.d(TAG, "File uri $fileUri: ")
        lifecycleScope.launch {
            viewModel.shareableMultiVideoIntent.collect { intent ->
                if (intent is EmptyIntent) return@collect
                activityViewModel.tempResultIntent = intent
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    oneTimeShareManager.addReceiver(
                        intent,
                        list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                    )
                } else {
                    oneTimeShareManager.launch(intent)
                }
            }
        }
        view?.let { Snackbar.make(it, "Processing...", Snackbar.LENGTH_LONG).show() }
        viewModel.trimVideo(fileUri)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_edit, container, false)
        view.findViewById<Button>(R.id.edit_frag_button)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "video/*"
            }
            resultLauncher.launch(Intent.createChooser(intent, "Select from"))
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fun getUri() = arguments?.let {
            UtilityClass.getParcelable(it, ARG_PARAM1, Uri::class.java)
                ?.let { it1 -> proceedWithUri(it1) }
        } ?: run {
            view?.let { v -> Snackbar.make(v, "Uri is null", Snackbar.LENGTH_LONG).show() }
        }
        activityViewModel =
            ViewModelProvider(requireActivity())[MainActivityViewModel::class.java]
        oneTimeShareManager = OneTimeShareManager(requireActivity()) {
            viewModel.deleteAll(it.data?.getParcelableArrayExtra(Intent.EXTRA_STREAM))
        }
        if (FileUtil.isFileNotExists(
                AppPref.getInstance(requireContext())
                    .getCachePath(AppPref.LIBRARY_PATH) + FFMPEG.FFMPEG_VERSION + "lib.zip"
            )
        ) (requireActivity() as MainActivity).fetchSOFiles {
            getUri()
        }
        else getUri()
    }


    companion object {
        @JvmStatic
        fun newInstance(uri: Uri) =
            EditFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PARAM1, uri)
                }
            }
    }
}