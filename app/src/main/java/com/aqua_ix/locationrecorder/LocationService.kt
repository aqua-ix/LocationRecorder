package com.aqua_ix.locationrecorder

import android.app.IntentService
import android.content.Intent
import com.google.android.gms.location.LocationResult

class LocationService : IntentService("LocationService") {
    override fun onHandleIntent(intent: Intent?) {
        val result = LocationResult.extractResult(intent)
        if (result != null) {
            insertLocations(this, result.locations) // データベースに保存する
        }
    }
}