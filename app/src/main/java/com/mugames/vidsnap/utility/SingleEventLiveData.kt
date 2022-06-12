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

package com.mugames.vidsnap.utility

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Udhaya
 * Created on 12-06-2022
 */

class SingleEventLiveData<T> : MutableLiveData<T>() {
    private val pending = AtomicBoolean(false)

    override fun setValue(value: T?) {
        value?.let {
            pending.set(true)
            super.setValue(value)
        }
    }

    override fun postValue(value: T?) {
        value?.let {
            pending.set(true)
            super.postValue(value)
        }
    }

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner, Observer {
            it?.let {
                if (pending.compareAndSet(true, false)) {
                    observer.onChanged(it)
                    postValue(null)
                }
            }
        })
    }
}