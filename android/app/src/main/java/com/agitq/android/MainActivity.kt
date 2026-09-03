package com.agitq.android

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {
    private lateinit var dashboard: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dashboard = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    if (!request.isForMainFrame) return false
                    return openOutsideApp(request.url)
                }
            }
            loadUrl(DASHBOARD_URL)
        }
        setContentView(
            dashboard,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    override fun onDestroy() {
        if (::dashboard.isInitialized) {
            dashboard.stopLoading()
            dashboard.destroy()
        }
        super.onDestroy()
    }

    private fun openOutsideApp(uri: Uri): Boolean {
        if (isDashboardUri(uri)) return false

        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            })
            true
        } catch (_: ActivityNotFoundException) {
            // 외부에서 처리할 앱이 없어도 AgiTQ WebView 안에서 열지는 않는다.
            true
        }
    }

    private fun isDashboardUri(uri: Uri): Boolean {
        return uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(DASHBOARD_HOST, ignoreCase = true) &&
            (uri.path == DASHBOARD_PATH || uri.path?.startsWith("$DASHBOARD_PATH/") == true)
    }

    companion object {
        private const val DASHBOARD_URL = "https://0x7f-core.github.io/AgiTQ-Android/"
        private const val DASHBOARD_HOST = "0x7f-core.github.io"
        private const val DASHBOARD_PATH = "/AgiTQ-Android"
    }
}
