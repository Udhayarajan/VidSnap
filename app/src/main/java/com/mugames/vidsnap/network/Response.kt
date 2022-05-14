package com.mugames.vidsnap.network
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

class Response {
    var exception: Throwable? = null
        private set
    var response: String? = null
        private set


    constructor(exception: Throwable?) {
        this.exception = exception
    }

    constructor(response: String?) {
        this.response = response
    }

    constructor(exception: Throwable?, response: String?) {
        this.exception = exception
        this.response = response
    }
}
