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

package com.mugames.vidsnap.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mugames.vidsnap.databinding.SingleInstagramUserLayoutBinding
import com.mugames.vidsnap.utility.InstagramReelsTrayResponseModel
import com.mugames.vidsnap.utility.Owner
import com.mugames.vidsnap.utility.Tray
import com.mugames.vidsnap.utility.User
import kotlin.reflect.typeOf

/**
 * @author Udhaya
 * Created on 12-01-2023
 */

class SingleInstagramUserStoryProfileAdapter(
    private val data: InstagramReelsTrayResponseModel,
    private val onClick: (user: User?) -> Unit,
    private val onLongClick: (user: User?) -> Unit
) : RecyclerView.Adapter<SingleInstagramUserStoryProfileAdapter.ViewHolder>() {

    class ViewHolder(
        private val binding: SingleInstagramUserLayoutBinding,
        private val onClick: (user: User?) -> Unit,
        private val onLongClick: (user: User?) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(userOwner: Any) {
            val (profileUrl, userName) = when (userOwner) {
                is User -> listOf(userOwner.profile_pic_url, userOwner.username)
                is Owner -> listOf(userOwner.profile_pic_url, userOwner.name)
                else -> throw Exception("Unable to find type passed ${userOwner.javaClass.name}")
            }
            Glide.with(binding.profile).load(profileUrl).into(binding.profile)
            binding.name.text = "@${userName.toString()}"
            binding.layout.setOnClickListener { onClick(userOwner as? User) }
            binding.layout.setOnLongClickListener {
                onLongClick(userOwner as User)
                return@setOnLongClickListener true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            SingleInstagramUserLayoutBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ), onClick, onLongClick
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data.tray[position].user ?: data.tray[position].owner!!)
    }

    override fun getItemCount() = data.tray.size
}