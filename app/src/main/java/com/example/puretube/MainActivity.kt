package com.example.puretube

import android.Manifest
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.net.Uri
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayInputStream

class WebAppInterface(private val context: Context) {
    @JavascriptInterface
    fun launchMapsSplitScreen(mapType: String) {
        val packageName = if (mapType == "vietmap") "vn.vietmap.vietmaplive" else "com.google.android.apps.maps"
        try {
            val i = context.packageManager.getLaunchIntentForPackage(packageName)
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                context.startActivity(i)
            } else {
                if (mapType == "gmap") {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
                    intent.setPackage(packageName)
                    intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    context.startActivity(intent)
                }
            }
        } catch (e: Exception) {
            // Ignored if app is completely missing
        }
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var fullscreenContainer: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    // Extended list of ad-serving domains to block
    private val adHosts = setOf(
        "googlesyndication.com",
        "googleads.g.doubleclick.net",
        "pagead2.googlesyndication.com",
        "ad.doubleclick.net",
        "ads.google.com",
        "www.googleadservices.com",
        "tpc.googlesyndication.com",
        "googleadservices.com",
        "static.doubleclick.net",
        "s0.2mdn.net",
        "yt3.ggpht.com",
        "play.google.com",
        "adservice.google.com",
        "adservice.google.com.vn",
        "google-analytics.com",
        "securepubads.g.doubleclick.net"
    )

    // Aggressive JavaScript to remove ad overlays and skip ad videos
    private val adBlockScript = """
        (function() {
            'use strict';
            
            // Spoof Page Visibility API so video keeps rendering in PiP
            Object.defineProperty(document, 'hidden', {get: function() {return false;}});
            Object.defineProperty(document, 'visibilityState', {get: function() {return 'visible';}});
            document.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
            window.addEventListener('blur', function(e) { e.stopImmediatePropagation(); }, true);
            
            function skipAds() {
                var skipButtons = document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, [class*="skip-button"], [id^="skip-button"], .ytm-custom-ad-skip-button');
                skipButtons.forEach(function(btn) { btn.click(); });
                
                var video = document.querySelector('video');
                if (video) {
                    var isAdShowing = document.querySelector('.ytp-ad-player-overlay, .ad-showing, .ytm-custom-ad-snapshot');
                    if (isAdShowing) {
                        video.currentTime = video.duration || 999;
                    }
                }
                
                var adElements = document.querySelectorAll('.ytp-ad-overlay-container, .ytp-ad-text-overlay, .ad-container, .video-ads, .ytp-ad-module, .ytp-ad-player-overlay, .ytp-ad-action-interstitial, .ytp-ad-promo-overlay, ytm-promoted-video-renderer, [class*="ad-show"], [class*="ytd-promoted"], [class*="sparkles"], ytd-promoted-sparkles-web-renderer, #player-ads, .ytd-banner-promo-renderer, .ytd-statement-banner-renderer, ytd-ad-slot-renderer, ytd-in-feed-ad-layout-renderer, #masthead-ad, ytd-primetime-promo-renderer, .masthead-ad-control, ytm-promoted-sparkles-text-search-renderer, ytm-promoted-sparkles-web-renderer, ytm-companion-ad-renderer');
                adElements.forEach(function(el) { el.style.display = 'none'; el.remove(); });
                
                // Car Screen Optimizations: Hide distracting elements (Shorts tab, comments, promos)
                var carElements = document.querySelectorAll('ytm-pivot-bar-renderer, ytm-comment-header-renderer, .comment-section, ytm-promoted-video-renderer, ytm-mealbar-promo-renderer');
                carElements.forEach(function(el) { el.style.display = 'none'; });
                
                // Inject Auto Split-Screen Buttons for Car Use - Smart UI
                if (!document.getElementById('maps-container') && window.location.href.includes('youtube.com')) {
                    var container = document.createElement('div');
                    container.id = 'maps-container';
                    container.style.cssText = 'position:fixed; bottom:25px; right:20px; z-index:99999; display:flex; flex-direction:row; gap:10px; opacity:0.3; transition:all 0.4s ease; transform:scale(0.9); transform-origin:right bottom;';
                    
                    var fadeTimeout;
                    function showButtons() {
                        container.style.opacity = '0.9';
                        container.style.transform = 'scale(1)';
                        clearTimeout(fadeTimeout);
                        fadeTimeout = setTimeout(function(){
                            container.style.opacity = '0.3';
                            container.style.transform = 'scale(0.9)';
                        }, 3500);
                    }
                    document.addEventListener('touchstart', showButtons, {passive: true});
                    document.addEventListener('mousemove', showButtons, {passive: true});
                    
                    var btnGmap = document.createElement('div');
                    btnGmap.innerHTML = '🗺️ G-Maps';
                    btnGmap.style.cssText = 'background:rgba(66, 133, 244, 0.85); backdrop-filter:blur(8px); color:white; padding:10px 16px; border-radius:24px; font-weight:bold; font-family:sans-serif; box-shadow:0 4px 10px rgba(0,0,0,0.3); font-size:13px; cursor:pointer; border:1px solid rgba(255,255,255,0.15);';
                    btnGmap.onclick = function(e) { e.stopPropagation(); if(window.AndroidApp) window.AndroidApp.launchMapsSplitScreen("gmap"); };
                    
                    var btnVietmap = document.createElement('div');
                    btnVietmap.innerHTML = '🚀 Vietmap';
                    btnVietmap.style.cssText = 'background:rgba(0, 150, 136, 0.85); backdrop-filter:blur(8px); color:white; padding:10px 16px; border-radius:24px; font-weight:bold; font-family:sans-serif; box-shadow:0 4px 10px rgba(0,0,0,0.3); font-size:13px; cursor:pointer; border:1px solid rgba(255,255,255,0.15);';
                    btnVietmap.onclick = function(e) { e.stopPropagation(); if(window.AndroidApp) window.AndroidApp.launchMapsSplitScreen("vietmap"); };
                    
                    container.appendChild(btnGmap);
                    container.appendChild(btnVietmap);
                    document.body.appendChild(container);
                    showButtons();
                }
            }
            setInterval(skipAds, 250);
            var observer = new MutationObserver(skipAds);
            observer.observe(document.body || document.documentElement, {childList: true, subtree: true});
        })();
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on for Car Driving
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Request POST_NOTIFICATIONS for background service on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        fullscreenContainer = FrameLayout(this)
        webView = WebView(this)
        fullscreenContainer.addView(webView)
        setContentView(fullscreenContainer)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }
        // Enable cookies (for YouTube login if needed)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        
        // Add Javascript Interface for Car Features
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidApp")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                
                for (adHost in adHosts) {
                    if (url.contains(adHost)) {
                        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(byteArrayOf()))
                    }
                }
                
                if (url.contains("/pagead/") || 
                    url.contains("/pcs/activeview") ||
                    url.contains("google_ads") ||
                    url.contains("/ad_data") ||
                    url.contains("doubleclick") ||
                    url.contains("/api/stats/ads") ||
                    url.contains("&adformat=") ||
                    url.contains("?adformat=") ||
                    url.contains("?ad_type=") ||
                    url.contains("&ad_type=")) {
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(byteArrayOf()))
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript(adBlockScript, null)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                customView = view
                customViewCallback = callback
                fullscreenContainer.addView(view)
                webView.visibility = View.GONE
            }

            override fun onHideCustomView() {
                fullscreenContainer.removeView(customView)
                customView = null
                customViewCallback?.onCustomViewHidden()
                webView.visibility = View.VISIBLE
            }
        }

        webView.loadUrl("https://m.youtube.com")
        startBackgroundService()
    }

    private fun startBackgroundService() {
        val serviceIntent = Intent(this, BackgroundAudioService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onBackPressed() {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            fullscreenContainer.removeView(customView)
            customView = null
            webView.visibility = View.VISIBLE
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Only enter PiP if watching a video
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) && webView.url?.contains("/watch") == true) {
                try {
                    val params = PictureInPictureParams.Builder().build()
                    enterPictureInPictureMode(params)
                } catch (e: Exception) {}
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // We no longer block v.pause() here! 
        // This allows Chromium to natively pause the video if a Zalo Call comes in (Audio Focus loss).
        // Background play still works because we spoofed the Page Visibility API in JS.
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        stopService(Intent(this, BackgroundAudioService::class.java))
        fullscreenContainer.removeAllViews() // Prevent Memory Leak
        webView.destroy()
        super.onDestroy()
    }
}

