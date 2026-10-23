package lv.akumulators

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout

/** Lietotne: ESP tīmekļa lapa pilnekrānā + adreses iestatīšana */
class MainActivity : Activity() {
    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView, req: WebResourceRequest, err: WebResourceError) {
                if (req.isForMainFrame) showOffline(err.description?.toString() ?: "")
            }
        }
        setContentView(web)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        if (Prefs.url(this).isEmpty()) askUrl() else load()
        UpdateWorker.schedule(this)
    }

    private fun load() {
        web.loadUrl(Prefs.url(this))
        UpdateWorker.runNow(this)
    }

    private fun showOffline(why: String) {
        val html = """
            <html><body style="font-family:sans-serif;padding:24px;color:#333">
            <h2>Nav savienojuma</h2>
            <p>Neizdevās atvērt <b>${Prefs.url(this)}</b></p>
            <p>Pārbaudi, vai telefons ir mājas WiFi tīklā un ierīce ir ieslēgta.</p>
            <p style="color:#999;font-size:13px">$why</p>
            <p>Izvēlnē: <b>Atjaunot</b> vai <b>Adrese</b>.</p>
            </body></html>""".trimIndent()
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    private fun askUrl() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            hint = "192.168.0.186"
            setText(Prefs.url(this@MainActivity).removePrefix("http://"))
        }
        val box = LinearLayout(this).apply {
            setPadding(48, 16, 48, 0)
            addView(input, LinearLayout.LayoutParams(-1, -2))
        }
        AlertDialog.Builder(this)
            .setTitle("Akumulatora adrese")
            .setMessage("IP adrese, kas redzama lapas augšā (vai akumulators.local)")
            .setView(box)
            .setPositiveButton("Saglabāt") { _, _ ->
                Prefs.setUrl(this, input.text.toString())
                load()
            }
            .setCancelable(Prefs.url(this).isNotEmpty())
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Atjaunot")
        menu.add(0, 2, 1, "Adrese")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> load()
            2 -> askUrl()
        }
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
