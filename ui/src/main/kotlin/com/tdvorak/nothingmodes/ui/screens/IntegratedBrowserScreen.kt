package com.tdvorak.nothingmodes.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tdvorak.nothingmodes.nothing.CustomGlyphStore
import com.tdvorak.nothingmodes.nothing.GlyphMuseumApi
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingPrimaryButton
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.TopBarAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * In-app browser for the Glyph Museum and the community library site.
 *
 * Integration points:
 * - `nothingmodes://import` links are forwarded to the app's own intent
 *   filter, so the site's OPEN IN APP buttons work inside this WebView.
 * - On a Glyph Museum `/post/<id>` page an import bar appears and pulls the
 *   design straight into [CustomGlyphStore] via [GlyphMuseumApi].
 * - JSON downloads (http(s) and blob: exports) are captured and imported
 *   instead of landing in a Downloads folder.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun IntegratedBrowserScreen(
    startUrl: String,
    title: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var browserFailed by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(startUrl) }
    var progress by remember { mutableIntStateOf(0) }
    var importing by remember { mutableStateOf(false) }

    fun openExternal() {
        // A chooser, not a bare VIEW — post links resolve to
        // this app's own import filter, so the user must be
        // able to pick a real browser.
        val view =
            Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                .addCategory(Intent.CATEGORY_BROWSABLE)
        runCatching {
            context.startActivity(Intent.createChooser(view, null))
        }
    }

    val museumPostId = remember(currentUrl) { GlyphMuseumApi.postIdFrom(Uri.parse(currentUrl)) }

    fun importJson(json: String, nameHint: String?) {
        val name = CustomGlyphStore(context).import(json, nameHint)
        Toast
            .makeText(
                context,
                if (name != null) "Glyph design imported as $name" else "Not a glyph design file",
                Toast.LENGTH_LONG,
            ).show()
    }

    fun buildWebView(ctx: Context): WebView =
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true

            addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun importJson(json: String) {
                        Handler(Looper.getMainLooper()).post { importJson(json, null) }
                    }
                },
                "NothingModesImport",
            )

            webViewClient =
                object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: android.webkit.WebResourceRequest,
                    ): Boolean {
                        val uri = request.url
                        val scheme = uri.scheme?.lowercase()
                        if (scheme == "http" || scheme == "https") return false
                        // Custom schemes (nothingmodes://import) and external
                        // apps (intent://, market://) resolve through the system —
                        // our own intent filters catch what belongs to us.
                        val intent =
                            if (scheme == "intent") {
                                runCatching {
                                    Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                                }.getOrNull()
                            } else {
                                Intent(Intent.ACTION_VIEW, uri)
                            }
                        intent?.let {
                            runCatching {
                                it.addCategory(Intent.CATEGORY_BROWSABLE)
                                context.startActivity(it)
                            }
                        }
                        return true
                    }

                    override fun onPageStarted(
                        view: WebView,
                        url: String?,
                        favicon: Bitmap?,
                    ) {
                        url?.let { currentUrl = it }
                    }

                    override fun doUpdateVisitedHistory(
                        view: WebView,
                        url: String?,
                        isReload: Boolean,
                    ) {
                        url?.let { currentUrl = it }
                    }
                }

            webChromeClient =
                object : WebChromeClient() {
                    override fun onProgressChanged(
                        view: WebView,
                        newProgress: Int,
                    ) {
                        progress = newProgress
                    }
                }

            setDownloadListener { url, _, _, mimeType, _ ->
                when {
                    url.startsWith("blob:") ->
                        // Blob exports live in page context — pull the
                        // text through JS and hand it to the bridge.
                        evaluateJavascript(
                            "fetch('$url').then(r=>r.text())" +
                                ".then(t=>NothingModesImport.importJson(t))" +
                                ".catch(()=>{})",
                            null,
                        )
                    mimeType?.contains("json") == true ||
                        URLUtil.guessFileName(url, null, mimeType).endsWith(".json") ->
                        scope.launch {
                            val text = fetchText(url)
                            if (text != null) {
                                importJson(
                                    text,
                                    URLUtil
                                        .guessFileName(url, null, mimeType)
                                        .substringBeforeLast('.'),
                                )
                            } else {
                                Toast
                                    .makeText(
                                        context,
                                        "Download failed",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }
                    else ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                }
            }

            loadUrl(startUrl)
            webView = this
        }

    BackHandler {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else onBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = title,
                onBack = onBack,
                actions =
                    listOf(
                        TopBarAction(label = "External") { openExternal() },
                    ),
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AnimatedVisibility(visible = progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                    )
                }
                AnimatedVisibility(visible = museumPostId != null) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(NothingSpacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "GLYPH MUSEUM POST",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = NothingFonts.mono(),
                        )
                        NothingPrimaryButton(
                            text = if (importing) "Importing…" else "Import",
                            enabled = !importing,
                            onClick = {
                                val postId = museumPostId ?: return@NothingPrimaryButton
                                importing = true
                                scope.launch {
                                    val name = GlyphMuseumApi.importPost(context, postId)
                                    importing = false
                                    Toast
                                        .makeText(
                                            context,
                                            if (name != null) {
                                                "Imported as $name"
                                            } else {
                                                "Could not import this post"
                                            },
                                            Toast.LENGTH_LONG,
                                        ).show()
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (browserFailed) {
            // WebView provider missing or being updated — offer the external
            // browser instead of crashing the whole screen.
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(NothingSpacing.lg),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "In-app browser unavailable",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "This device has no usable WebView provider.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                    modifier = Modifier.padding(vertical = NothingSpacing.md),
                )
                NothingPrimaryButton(text = "Open in browser", onClick = { openExternal() })
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize().padding(padding),
                factory = { ctx ->
                    runCatching { buildWebView(ctx) }.getOrElse {
                        Handler(Looper.getMainLooper()).post { browserFailed = true }
                        View(ctx)
                    }
                },
            )
        }
    }
}

private suspend fun fetchText(url: String): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }
