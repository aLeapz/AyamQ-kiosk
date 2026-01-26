package com.ayamq.kiosk.scheduler

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.ayamq.kiosk.util.BackupEngine
import com.ayamq.kiosk.util.NetworkUtils
import com.ayamq.kiosk.util.PreferenceManager
import com.ayamq.kiosk.util.BackupUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


/**
 * JobScheduler-based backup service
 * Runs backup operations when device conditions are met
 * - Device is idle
 * - Device is charging
 * - Network is available (for future cloud sync)
 *
 * Currently backs up to local JSON files
 * Can be extended to sync with cloud storage (Firebase, AWS, etc.)
 */
class BackupJobService : JobService() {

    companion object {
        private const val TAG = "BackupJobService"
        const val JOB_ID = 1001
    }

    private val jobScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Backup job started")

        jobScope.launch {
            try {
                Log.d(TAG, "Running scheduled backup")

                // 1. BACKUP LOKAL (SATU-SATUNYA LOGIC ADA DI BACKUPENGINE)
                BackupEngine.performBackup(applicationContext)

                // 2. UPLOAD JIKA ADA INTERNET
                if (NetworkUtils.isNetworkAvailable(applicationContext)) {
                    BackupUploader.uploadAllBackups(applicationContext)
                } else {
                    Log.d(TAG, "No network, backup saved locally only")
                }

                // 3. SIMPAN WAKTU BACKUP
                PreferenceManager(applicationContext)
                    .saveLastBackupTime(System.currentTimeMillis())

                Log.d(TAG, "Backup job finished successfully")
                jobFinished(params, false)

            } catch (e: Exception) {
                Log.e(TAG, "Backup job failed", e)
                jobFinished(params, true) // reschedule
            }
        }

        // Job masih berjalan async
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Backup job stopped")
        jobScope.cancel()
        return true // reschedule
    }
}
