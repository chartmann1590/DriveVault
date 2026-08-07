package com.drivevault.dashcam.data.review

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.tasks.await

private val Context.reviewPromptDataStore: DataStore<Preferences> by preferencesDataStore(name = "review_prompt_prefs")

private object Keys {
    val LIBRARY_OPEN_COUNT = intPreferencesKey("review_prompt_library_open_count")
    val REQUESTED = booleanPreferencesKey("review_prompt_requested")
}

/** Clip library opens with at least one saved clip, before we ever ask for a review. */
private const val OPENS_BEFORE_FIRST_ASK = 2

/**
 * Prompts the official Play In-App Review dialog after the rider has actually seen proof the
 * dashcam is working — real saved clips in their library, not just an app-open. Google's own
 * quota caps how often the dialog can appear regardless of what we request, so this only needs
 * to avoid asking too early and never ask twice.
 */
object ReviewPrompter {
    suspend fun maybeRequestReview(activity: Activity) {
        var shouldRequest = false
        activity.applicationContext.reviewPromptDataStore.edit { prefs ->
            val alreadyRequested = prefs[Keys.REQUESTED] ?: false
            val count = (prefs[Keys.LIBRARY_OPEN_COUNT] ?: 0) + 1
            prefs[Keys.LIBRARY_OPEN_COUNT] = count
            if (!alreadyRequested && count >= OPENS_BEFORE_FIRST_ASK) {
                prefs[Keys.REQUESTED] = true
                shouldRequest = true
            }
        }
        if (!shouldRequest) return

        runCatching {
            val manager = ReviewManagerFactory.create(activity)
            val reviewInfo = manager.requestReviewFlow().await()
            manager.launchReviewFlow(activity, reviewInfo).await()
        }
    }
}
