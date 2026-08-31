package com.agitq.android

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidView(modifier=Modifier.fillMaxSize(), factory={
                WebView(it).apply {
                    settings.javaScriptEnabled=true
                    settings.domStorageEnabled=true
                    settings.loadWithOverviewMode=true
                    settings.useWideViewPort=true
                    webViewClient=WebViewClient()
                    loadUrl("https://YOUR-USERNAME.github.io/AgiTQ-Android/")
                }
            })
        }
    }
}
