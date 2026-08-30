package com.example.futurediary.data.sync

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.futurediary.ui.util.NotificationHelper

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        NotificationHelper.showReminderNotification(applicationContext)
        return Result.success()
    }
}
