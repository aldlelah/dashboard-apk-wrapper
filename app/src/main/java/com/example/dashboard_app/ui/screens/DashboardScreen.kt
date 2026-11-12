package com.example.dashboard_app.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.dashboard_app.ui.components.LoadingOverlay
import com.example.dashboard_app.ui.components.OfflineErrorView
import com.example.dashboard_app.ui.components.TopProgressBar
import com.example.dashboard_app.ui.theme.Slate900
import com.example.dashboard_app.utils.NetworkObserver

private const val DASHBOARD_URL = "https://aldalilah-900eb.web.app"

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    initialUrl: String = DASHBOARD_URL
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val networkObserver = remember { NetworkObserver(context) }
    val isConnected by networkObserver.observeNetwork().collectAsState(initial = networkObserver.isNetworkAvailable())

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webViewBundle by rememberSaveable { mutableStateOf<Bundle?>(null) }

    var loadProgress by remember { mutableIntStateOf(0) }
    var isInitialLoading by remember { mutableStateOf(true) }
    var hasPageError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // System Back Press Handler
    BackHandler(enabled = webViewInstance?.canGoBack() == true && !hasPageError) {
        webViewInstance?.goBack()
    }

    // Lifecycle Awareness (Pause / Resume / Destroy)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webViewInstance?.onResume()
                Lifecycle.Event.ON_PAUSE -> webViewInstance?.onPause()
                Lifecycle.Event.ON_DESTROY -> {
                    webViewInstance?.let { wv ->
                        // Save bundle state before cleanup
                        val bundle = Bundle()
                        wv.saveState(bundle)
                        webViewBundle = bundle

                        wv.stopLoading()
                        wv.removeAllViews()
                        wv.destroy()
                    }
                    webViewInstance = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-reload when network switches back to online after an error
    LaunchedEffect(isConnected) {
        if (isConnected && hasPageError && webViewInstance != null) {
            hasPageError = false
            errorMessage = null
            webViewInstance?.reload()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Slate900),
        containerColor = Slate900
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Slate900)
        ) {
            if (!isConnected && (hasPageError || webViewInstance?.url == null)) {
                OfflineErrorView(
                    errorMessage = "لا يوجد اتصال بالإنترنت. يرجى التحقق من الشبكة وإعادة المحاولة.",
                    onRetry = {
                        if (networkObserver.isNetworkAvailable()) {
                            hasPageError = false
                            errorMessage = null
                            if (webViewInstance != null) {
                                webViewInstance?.reload()
                            }
                        }
                    }
                )
            } else if (hasPageError) {
                OfflineErrorView(
                    errorMessage = errorMessage,
                    onRetry = {
                        hasPageError = false
                        errorMessage = null
                        webViewInstance?.reload() ?: run {
                            isInitialLoading = true
                        }
                    }
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            // يمسح أي كاش قديم محفوظ من إصدارات سابقة من هذا التطبيق (بما فيها الكاش
                            // الذي تسبب في عرض نسخة قديمة من اللوحة رغم نشر تحديث جديد).
                            clearCache(true)

                            // Enable Cookies & Third-Party Cookies for Firebase Auth cross-domain sessions
                            val cookieManager = android.webkit.CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            // Performance & Storage Settings
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true // CRITICAL: Required for Firebase Auth & LocalStorage
                                databaseEnabled = true
                                javaScriptCanOpenWindowsAutomatically = true
                                setSupportZoom(false)
                                builtInZoomControls = false
                                displayZoomControls = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                allowFileAccess = true
                                allowContentAccess = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                // اللوحة يجب أن تعرض دائماً أحدث نشر مباشرة من الخادم، بدون أي احتمال
                                // لعرض نسخة HTML قديمة مخزّنة محلياً (كما حدث فعلياً مع نسخة قديمة من
                                // index.html بقيت ظاهرة رغم نشر تحديث جديد على Firebase Hosting).
                                cacheMode = WebSettings.LOAD_NO_CACHE
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    return if (url.startsWith("http://") || url.startsWith("https://")) {
                                        // Keep navigations within the app WebView
                                        false
                                    } else {
                                        // Handle external intents (tel:, mailto:, whatsapp:, etc.)
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            ctx.startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        true
                                    }
                                }

                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: Bitmap?
                                ) {
                                    super.onPageStarted(view, url, favicon)
                                    hasPageError = false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isInitialLoading = false
                                    android.webkit.CookieManager.getInstance().flush()
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    // Only trigger error layout for main frame navigation failures
                                    if (request?.isForMainFrame == true) {
                                        hasPageError = true
                                        errorMessage = "فشل تحميل الصفحة. (رمز الخطأ: ${error?.errorCode ?: -1})"
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    loadProgress = newProgress
                                    if (newProgress >= 100) {
                                        isInitialLoading = false
                                    }
                                }
                            }

                            // Restore state on configuration change / recreation
                            if (webViewBundle != null) {
                                restoreState(webViewBundle!!)
                            } else {
                                loadUrl(initialUrl)
                            }

                            webViewInstance = this
                        }
                    },
                    update = { wv ->
                        webViewInstance = wv
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Sleek top progress indicator during page navigation
                TopProgressBar(progress = loadProgress)

                // Initial load dark overlay indicator
                LoadingOverlay(isLoading = isInitialLoading && loadProgress < 30)
            }
        }
    }
}
