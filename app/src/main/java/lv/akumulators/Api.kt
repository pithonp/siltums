package lv.akumulators

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Dati no ESP /api/live */
data class Live(
    val eUsable: Double?,
    val pct: Double?,
    val capacity: Double?,
    val remainH: Double?,
    val emptyAt: Long,
    val rate: Double?,
    val charging: Boolean,
    val chgRate: Double?,
    val chgKwh: Double?,
    val fullH: Double?,
    val fullAt: Long,
    val top: Double?,
    val mid: Double?,
    val bot: Double?,
    val out: Double?,
    val floor: Double?,
    val alarm: Boolean,
    val floorAlarm: Boolean,
    val floorMax: Double?,
    val tMin: Double?,
)

object Api {
    fun fetchText(base: String): String {
        val conn = URL("$base/api/live").openConnection() as HttpURLConnection
        conn.connectTimeout = 6000
        conn.readTimeout = 6000
        try {
            if (conn.responseCode != 200) throw RuntimeException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun num(o: JSONObject?, k: String): Double? =
        if (o == null || !o.has(k) || o.isNull(k)) null else o.optDouble(k).takeIf { !it.isNaN() }

    fun parse(txt: String): Live {
        val j = JSONObject(txt)
        val t = j.optJSONObject("t")
        return Live(
            eUsable = num(j, "eUsable"),
            pct = num(j, "pct"),
            capacity = num(j, "capacity"),
            remainH = num(j, "remainH"),
            emptyAt = j.optLong("emptyAt", 0),
            rate = num(j, "rate"),
            charging = j.optBoolean("charging", false),
            chgRate = num(j, "chgRate"),
            chgKwh = num(j, "chgKwh"),
            fullH = num(j, "fullH"),
            fullAt = j.optLong("fullAt", 0),
            top = num(t, "top"),
            mid = num(t, "mid"),
            bot = num(t, "bot"),
            out = num(t, "out"),
            floor = num(t, "floor"),
            alarm = j.optBoolean("alarm", false),
            floorAlarm = j.optBoolean("floorAlarm", false),
            floorMax = num(j, "floorMax"),
            tMin = num(j, "tMin"),
        )
    }
}

object Fmt {
    fun t(v: Double?) = if (v == null) "–" else String.format(Locale.US, "%.1f°", v)
    fun one(v: Double?) = if (v == null) "–" else String.format(Locale.US, "%.1f", v)
    fun dur(h: Double?): String {
        if (h == null) return "–"
        if (h >= 48) return String.format(Locale.US, "%.1f dnn", h / 24)
        val hh = h.toInt()
        val mm = ((h - hh) * 60).toInt()
        return "$hh h ${mm.toString().padStart(2, '0')} min"
    }
    fun hm(epoch: Long): String =
        if (epoch <= 0) "–" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epoch * 1000))
    fun dayHm(epoch: Long): String =
        if (epoch <= 0) "–" else SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(epoch * 1000))
}

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("aku", Context.MODE_PRIVATE)

    fun url(c: Context): String = sp(c).getString("url", "") ?: ""
    fun setUrl(c: Context, u: String) {
        var s = u.trim().trimEnd('/')
        if (s.isNotEmpty() && !s.startsWith("http")) s = "http://$s"
        sp(c).edit().putString("url", s).apply()
    }

    fun lastJson(c: Context): String? = sp(c).getString("last", null)
    fun lastTime(c: Context): Long = sp(c).getLong("lastT", 0)
    fun setLast(c: Context, json: String) =
        sp(c).edit().putString("last", json).putLong("lastT", System.currentTimeMillis()).apply()

    fun flag(c: Context, k: String) = sp(c).getBoolean(k, false)
    fun setFlag(c: Context, k: String, v: Boolean) = sp(c).edit().putBoolean(k, v).apply()
}
