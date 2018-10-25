package com.alamkanak.weekview.sample.apiclient

import retrofit.Callback
import retrofit.http.GET

interface MyJsonService {

    @GET("/1kpjf")
    fun listEvents(eventsCallback: Callback<MutableList<Event>>)

}
