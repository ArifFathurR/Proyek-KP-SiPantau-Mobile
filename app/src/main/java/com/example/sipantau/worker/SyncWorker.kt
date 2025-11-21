package com.example.sipantau.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sipantau.repository.LaporanRepository
import com.example.sipantau.utils.NetworkUtil

class SyncWorker(appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!NetworkUtil.isOnline(ctx)) return Result.success() // nothing to do
        val repo = LaporanRepository(ctx)
        return try {
            repo.syncAllPending()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
