package com.akeshari.splitblind.di

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseDatabase(@ApplicationContext context: Context): FirebaseDatabase {
        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:000000000000:android:0000000000000000")
                .setDatabaseUrl("https://splitblind-default-rtdb.asia-southeast1.firebasedatabase.app")
                .setProjectId("splitblind")
                .setApiKey("AIzaSyDummyKeyForManualInit")
                .build()
            FirebaseApp.initializeApp(context, options)
        }
        val db = FirebaseDatabase.getInstance("https://splitblind-default-rtdb.asia-southeast1.firebasedatabase.app")
        db.setPersistenceEnabled(true)
        return db
    }
}
