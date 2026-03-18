package com.example.puretube

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var fullscreenContainer: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    // List of ad-serving domains to block
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
        "yt3.ggpht.com",      // some ad-related
        "play.google.com",     // app install ads
    )

    // JavaScript to remove ad overlays and skip ad videos
    private val adBlockScript = """
        (function() {
            'use strict';
            
            // Function to skip ads
            function skipAds() {
                // Click "Skip Ad" button if present
                var skipButtons = document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, [class*="skip-button"]');
                skipButtons.forEach(function(btn) { btn.click(); });
                
                // Remove ad overlays
                var adOverlays = document.querySelectorAll('.ytp-ad-overlay-container, .ytp-ad-text-overlay, .ad-container, .video-ads, .ytp-ad-module, .ytp-ad-player-overlay, .ytp-ad-action-interstitial');
                adOverlays.forEach(function(el) { el.remove(); });
                
                // Force skip video ads by setting currentTime
                var video = document.querySelector('video');
                if (video && document.querySelector('.ytp-ad-player-overlay')) {
                    video.currentTime = video.duration || 999;
                }
                
                // Remove banner ads and promoted content
                var bannerAds = document.querySelectorAll('[class*="ad-show"], [class*="ytd-promoted"], [class*="sparkles-light-cta"], ytd-promoted-sparkles-web-renderer, #player-ads, .ytd-banner-promo-renderer, .ytd-statement-banner-renderer, ytd-ad-slot-renderer, ytd-in-feed-ad-layout-renderer');
                bannerAds.forEach(function(el) { el.remove(); });
                
                // Remove masthead ads
                var mastheadAds = document.querySelectorAll('#masthead-ad, ytd-primetime-promo-renderer');
                mastheadAds.forEach(function(el) { el.remove(); });
            }
            
            // Run every 500ms to catch dynamically loaded ads
            setInterval(skipAds, 500);
            
            // Also run on DOM changes
            var observer = new MutationObserver(skipAds);
            observer.observe(document.body || document.documentElement, {childList: true, subtree: true});
        })();
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create layout programmatically
        fullscreenContainer = FrameLayout(this)
        webView = WebView(this)
        fullscreenContainer.addView(webView)
        setContentView(fullscreenContainer)

        // Configure WebView settings
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

        // Set up WebView client to block ads at network level
        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                
                // Block requests to known ad domains
                for (adHost in adHosts) {
                    if (url.contains(adHost)) {
                        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(byteArrayOf()))
                    }
                }
                
                // Block common ad URL patterns
                if (url.contains("/pagead/") || 
                    url.contains("/pcs/activeview") ||
                    url.contains("google_ads") ||
                    url.contains("/ad_data") ||
                    url.contains("doubleclick") ||
                    url.contains("/api/stats/ads")) {
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(byteArrayOf()))
                }
                
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inject ad-blocking JavaScript after page loads
                view?.evaluateJavascript(adBlockScript, null)
            }
        }

        // Set up WebChromeClient for fullscreen video support
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

        // Load YouTube mobile
        webView.loadUrl("https://m.youtube.com")
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
        // Enter PiP mode when user presses Home while video is playing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                enterPictureInPictureMode(PictureInPictureParams.Builder().build())
            } catch (e: Exception) {
                // PiP not supported or not in valid state
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Keep audio playing in background by injecting JS to prevent pause
        webView.evaluateJavascript("""
            (function() {
                var videos = document.querySelectorAll('video');
                videos.forEach(function(v) {
                    v.play();
                    // Override the pause function to prevent YouTube from pausing
                    v._originalPause = v.pause;
                    v.pause = function() { /* blocked */ };
                });
            })();
        """.trimIndent(), null)
    }

    override fun onResume() {
        super.onResume()
        // Restore original pause function
        webView.evaluateJavascript("""
            (function() {
                var videos = document.querySelectorAll('video');
                videos.forEach(function(v) {
                    if (v._originalPause) {
                        v.pause = v._originalPause;
                    }
                });
            })();
        """.trimIndent(), null)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
