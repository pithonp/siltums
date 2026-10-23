package lv.akumulators

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Sākuma ekrāna logrīks: atlikušais siltums, laiks, temperatūras, kurināšana */
class AkuWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        render(ctx) // uzreiz no kešatmiņas
        UpdateWorker.schedule(ctx)
        UpdateWorker.runNow(ctx)
    }

    override fun onEnabled(ctx: Context) {
        UpdateWorker.schedule(ctx)
    }

    override fun onDisabled(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(UpdateWorker.NAME)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_REFRESH) UpdateWorker.runNow(ctx)
    }

    companion object {
        const val ACTION_REFRESH = "lv.akumulators.REFRESH"

        /** Uzzīmē logrīku no pēdējiem saglabātajiem datiem. error != null → nav savienojuma. */
        fun render(ctx: Context, error: String? = null) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, AkuWidget::class.java))
            if (ids.isEmpty()) return

            val v = RemoteViews(ctx.packageName, R.layout.widget)
            val json = Prefs.lastJson(ctx)
            val live = try { json?.let { Api.parse(it) } } catch (e: Exception) { null }

            if (live == null) {
                v.setTextViewText(R.id.kwh, "–")
                v.setTextViewText(R.id.pct, "")
                v.setProgressBar(R.id.bar, 100, 0, false)
                v.setTextViewText(R.id.status, error ?: "Atver lietotni un ievadi adresi")
                v.setTextViewText(R.id.temps, "")
                v.setTextViewText(R.id.floor, "")
                v.setInt(R.id.root, "setBackgroundResource", R.drawable.widget_bg)
            } else {
                v.setTextViewText(R.id.kwh, if (live.eUsable == null) "–" else "${Fmt.one(live.eUsable)} kWh")
                v.setTextViewText(R.id.pct, if (live.pct == null) "" else "${live.pct.toInt()} %")
                v.setProgressBar(R.id.bar, 100, (live.pct ?: 0.0).toInt().coerceIn(0, 100), false)

                val status = when {
                    live.charging -> {
                        val full = if (live.fullH != null) " · pilns ~${Fmt.hm(live.fullAt)}" else ""
                        "▲ Kurina ${Fmt.one(live.chgRate)} kW$full"
                    }
                    live.alarm -> "Jākurina! Augša ${Fmt.t(live.top)}"
                    live.remainH != null -> "Pietiks ~${Fmt.dur(live.remainH)} (līdz ${Fmt.dayHm(live.emptyAt)})"
                    else -> "Krāj datus patēriņa aprēķinam…"
                }
                v.setTextViewText(R.id.status, status)
                v.setTextViewText(R.id.temps, "Augša ${Fmt.t(live.top)}   Vidus ${Fmt.t(live.mid)}   Apakša ${Fmt.t(live.bot)}")
                val fl = if (live.floor != null) "Grīdas ${Fmt.t(live.floor)}" + (if (live.floorAlarm) " — PAR KARSTU!" else "") else ""
                val out = if (live.out != null) "   Ārā ${Fmt.t(live.out)}" else ""
                v.setTextViewText(R.id.floor, fl + out)

                val bg = when {
                    live.alarm || live.floorAlarm -> R.drawable.widget_bg_alarm
                    live.charging -> R.drawable.widget_bg_charging
                    else -> R.drawable.widget_bg
                }
                v.setInt(R.id.root, "setBackgroundResource", bg)
            }

            val last = Prefs.lastTime(ctx)
            val upd = if (last > 0) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(last)) else ""
            v.setTextViewText(R.id.updated, if (error != null && live != null) "⚠ $upd" else upd)

            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), flags)
            v.setOnClickPendingIntent(R.id.root, open)
            val refresh = PendingIntent.getBroadcast(
                ctx, 1, Intent(ctx, AkuWidget::class.java).setAction(ACTION_REFRESH), flags
            )
            v.setOnClickPendingIntent(R.id.refresh, refresh)

            mgr.updateAppWidget(ids, v)
        }
    }
}
