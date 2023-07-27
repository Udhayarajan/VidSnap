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
package com.mugames.vidsnap.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.mugames.vidsnap.R
import com.mugames.vidsnap.database.HistoryRepository
import com.mugames.vidsnap.databinding.FragmentHistoryBinding
import com.mugames.vidsnap.ui.adapters.HistoryRecyclerViewAdapter
import com.mugames.vidsnap.ui.viewmodels.HistoryViewModel
import com.mugames.vidsnap.ui.viewmodels.factory.HistoryViewModelFactory
import com.mugames.vidsnap.utility.Statics
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * This fragment keeps track of Download history data
 * Usually get displayed when Menu->Downloads is clicked
 */
class HistoryFragment : Fragment() {
    private var TAG = Statics.TAG + ":HistoryFragment"
    private val historyViewModel by viewModels<HistoryViewModel>(
        factoryProducer = { HistoryViewModelFactory(this, HistoryRepository(context)) }
    )
    private lateinit var deleteLauncher: ActivityResultLauncher<IntentSenderRequest>

    private lateinit var binding: FragmentHistoryBinding

    private lateinit var adapter: HistoryRecyclerViewAdapter

    private lateinit var readPermissionResultLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        deleteLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                historyViewModel.deletePendingUri(requireContext().contentResolver)
            } else Toast.makeText(requireContext(), "Unable to delete", Toast.LENGTH_SHORT)
                .show()
        }

        readPermissionResultLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result: Boolean ->
                if (result) {
                    loadData()
                } else {
                    Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show()
                    requireActivity().onBackPressed()
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        adapter = HistoryRecyclerViewAdapter(viewLifecycleOwner, historyViewModel)

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.list_background
            )
        )

        binding.loadingIndicator.show()
        recyclerView.adapter = adapter
        adapter.addLoadStateListener {
            if (it.source.refresh is LoadState.Loading || it.source.prepend is LoadState.Loading || it.source.append is LoadState.Loading) {
                binding.loadingIndicator.show()
            } else
                binding.loadingIndicator.hide()
            swapViewVisibility(adapter.itemCount != 0)
        }

        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            readPermissionResultLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            loadData()
        }
        historyViewModel.intentSender.observe(viewLifecycleOwner) { intentSender ->
            if (intentSender != null) {
                deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
        }
        swapViewVisibility(false)
        binding.noRecordContainer.errorReason.text = "No old downloads found"
        return binding.root
    }

    private fun swapViewVisibility(hasElement: Boolean) {
        if (hasElement) {
            binding.recyclerView.visibility = View.VISIBLE
            binding.recyclerView.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.list_background
                )
            )
            binding.noRecordContainer.noRecordFragmentParent.visibility = View.GONE
        } else {
            binding.recyclerView.visibility = View.GONE
            binding.noRecordContainer.noRecordFragmentParent.visibility = View.VISIBLE
            binding.root.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.notification_color
                )
            )
        }
    }

    private fun loadData() {
        val values = historyViewModel.pagedValue
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                values.collectLatest {
                    adapter.submitData(it)
                }
            }
        }

//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                adapter.loadStateFlow.collect {
////
//                }
//            }
//        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): HistoryFragment {
            return HistoryFragment()
        }
    }
}