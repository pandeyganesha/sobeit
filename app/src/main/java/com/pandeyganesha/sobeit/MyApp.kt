package com.pandeyganesha.sobeit

import android.app.Application
import com.pandeyganesha.sobeit.data.AppDatabase
import com.pandeyganesha.sobeit.data.DatabaseProvider

class MyApp : Application() {
    lateinit var db: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        db = DatabaseProvider.getDatabase(this)
    }
}