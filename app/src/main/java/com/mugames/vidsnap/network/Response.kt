package com.mugames.vidsnap.network

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
