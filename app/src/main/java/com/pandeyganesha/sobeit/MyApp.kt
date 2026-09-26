package com.pandeyganesha.kaamsutra

import android.app.Application
import com.pandeyganesha.kaamsutra.data.AppDatabase
import com.pandeyganesha.kaamsutra.data.DatabaseProvider

class MyApp : Application() {
    lateinit var db: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        db = DatabaseProvider.getDatabase(this)
    }
}