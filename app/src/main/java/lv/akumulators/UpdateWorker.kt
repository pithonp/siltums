package lv.akumulators

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.app.Notification
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Fonā ik 15 min nolasa /api/live, atjauno logrīku un sūta paziņojumus */
class UpdateWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val c = applicationContext
        val base = Prefs.url(c)
        if (base.isEmpty()) {
            AkuWidget.render(c, "Atver lietotni un ievadi adresi")
            return Result.success()
        }
        try {
            val txt = Api.fetchText(base)
            Api.parse(txt) // pārbauda, ka JSON derīgs
            Prefs.setLast(c, txt)
            AkuWidget.render(c)
            notifyIfNeeded(c, Api.parse(txt))
        } catch (e: Exception) {
            AkuWidget.render(c, "Nav savienojuma (vai esi mājas WiFi?)")
        }
        return Result.success()
    }

    private fun notifyIfNeeded(c: Context, l: Live) {
        check(c, "nAlarm", l.alarm, 1, "Jākurina!", "Akumulatora augša ${Fmt.t(l.top)}, atlicis ${Fmt.one(l.eUsable)} kWh")
        check(c, "nFloor", l.floorAlarm, 2, "Grīdas par karstu!", "Turpgaita ${Fmt.t(l.floor)} (max ${Fmt.t(l.floorMax)})")
    }

    /** Paziņo tikai, kad stāvoklis mainās no "nav" uz "ir" */
    private fun check(c: Context, key: String, now: Boolean, id: Int, title: String, text: String) {
        val was = Prefs.flag(c, key)
        Prefs.setFlag(c, key, now)
        if (!now || was) return
        if (Build.VERSION.SDK_INT >= 33 &&
            c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Akumulatora brīdinājumi", NotificationManager.IMPORTANCE_HIGH)
        )
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(id, n)
    }

    companion object {
        const val NAME = "aku-periodic"
        private const val CHANNEL = "aku_alarm"

        fun schedule(c: Context) {
            val req = PeriodicWorkRequest.Builder(UpdateWorker::class.java, 15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(c).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun runNow(c: Context) {
            WorkManager.getInstance(c).enqueueUniqueWork(
                "aku-now", ExistingWorkPolicy.REPLACE, OneTimeWorkRequest.from(UpdateWorker::class.java)
            )
        }
    }
}
