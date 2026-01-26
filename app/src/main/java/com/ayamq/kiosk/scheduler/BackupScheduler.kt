package com.ayamq.kiosk.scheduler

import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Utility object to schedule JobScheduler tasks
 */
object BackupScheduler {

    private const val TAG = "BackupScheduler"

    /**
     * Schedule backup job
     * Job will run when:
     * - Device is idle
     * - Device is charging
     * - Network is available
     */
    fun scheduleBackupJob(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as android.app.job.JobScheduler

            val jobInfo = android.app.job.JobInfo.Builder(
                BackupJobService.JOB_ID,
                android.content.ComponentName(context, BackupJobService::class.java)
            )
                .setRequiresCharging(true) // Run only when charging
                .setRequiresDeviceIdle(true) // Run only when device is idle
                .setRequiredNetworkType(android.app.job.JobInfo.NETWORK_TYPE_ANY) // Requires network
                .setPeriodic(24 * 60 * 60 * 1000L) // Run once per day
                .setPersisted(true) // Persist across reboots
                .build()

            val result = jobScheduler.schedule(jobInfo)
            if (result == android.app.job.JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "Backup job scheduled successfully")
            } else {
                Log.e(TAG, "Failed to schedule backup job")
            }
        }
    }


    /**
     * Manually trigger backup immediately
     */
    fun triggerBackupNow(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as android.app.job.JobScheduler

            // Create one-time job that runs immediately
            val jobInfo = android.app.job.JobInfo.Builder(
                BackupJobService.JOB_ID + 1, // Different ID for immediate job
                android.content.ComponentName(context, BackupJobService::class.java)
            )
                .setOverrideDeadline(0) // Run immediately
                .build()

            val result = jobScheduler.schedule(jobInfo)
            if (result == android.app.job.JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "Immediate backup triggered")
            } else {
                Log.e(TAG, "Failed to trigger immediate backup")
            }
        }
    }


    /**
     * Get backup file path for menu items
     */
    fun getMenuBackupFile(context: Context): java.io.File {
        return java.io.File(context.filesDir, "menu_backup.json")
    }

    /**
     * Get backup file path for orders
     */
    fun getOrdersBackupFile(context: Context): java.io.File {
        return java.io.File(context.filesDir, "orders_backup.json")
    }

    /**
     * Check if backup files exist
     */
    fun hasBackupFiles(context: Context): Boolean {
        return getMenuBackupFile(context).exists() && getOrdersBackupFile(context).exists()
    }
}