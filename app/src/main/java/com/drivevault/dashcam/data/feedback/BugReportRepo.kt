package com.drivevault.dashcam.data.feedback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

private val Context.feedbackDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "feedback_bug_reports"
)

class BugReportRepo(private val context: Context) {

    private val gson = Gson()
    private val key = stringPreferencesKey("bug_reports_list")

    val bugReports: Flow<List<BugReport>> = context.feedbackDataStore.data.map { prefs ->
        parseReports(prefs[key])
    }

    suspend fun saveBugReport(report: BugReport) {
        context.feedbackDataStore.edit { prefs ->
            val current = parseReports(prefs[key]).toMutableList()
            val existing = current.indexOfFirst { it.number == report.number }
            if (existing >= 0) {
                current[existing] = report
            } else {
                current.add(0, report)
            }
            prefs[key] = gson.toJson(current)
        }
    }

    suspend fun updateBugReports(reports: List<BugReport>) {
        context.feedbackDataStore.edit { prefs ->
            prefs[key] = gson.toJson(reports)
        }
    }

    suspend fun getBugReportsList(): List<BugReport> {
        return try {
            var result: List<BugReport> = emptyList()
            context.feedbackDataStore.edit { prefs ->
                result = parseReports(prefs[key])
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseReports(json: String?): List<BugReport> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<BugReport>>() {}.type
            gson.fromJson<List<BugReport>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
