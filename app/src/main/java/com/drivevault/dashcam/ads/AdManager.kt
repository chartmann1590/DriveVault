package com.drivevault.dashcam.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.drivevault.dashcam.recording.RecordingService
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

object AdManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var bannerAdView: AdView? = null
    private var interstitialAd: InterstitialAd? = null
    private var isInitialized = false
    private var interstitialAdUnitId: String = ""

    val adsSuppressed: StateFlow<Boolean> = RecordingService.recordingState
        .map { it.isRecording }
        .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    fun initialize(context: Context, interstitialUnitId: String) {
        if (isInitialized) return
        isInitialized = true
        interstitialAdUnitId = interstitialUnitId

        MobileAds.initialize(context) {}
        loadInterstitial(context)
    }

    fun createBannerView(activity: Activity, bannerUnitId: String): AdView {
        bannerAdView?.let { return it }

        val adView = AdView(activity).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = bannerUnitId
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        bannerAdView = adView
        loadBanner()
        return adView
    }

    fun loadBanner() {
        val adView = bannerAdView ?: return
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    fun showBanner() {
        bannerAdView?.visibility = android.view.View.VISIBLE
    }

    fun hideBanner() {
        bannerAdView?.visibility = android.view.View.GONE
    }

    private fun loadInterstitial(context: Context) {
        if (interstitialAdUnitId.isEmpty()) return

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            interstitialAdUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            loadInterstitial(context)
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            interstitialAd = null
                        }

                        override fun onAdShowedFullScreenContent() {
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitial(activity: Activity): Boolean {
        if (adsSuppressed.value) return false
        val ad = interstitialAd ?: return false
        ad.show(activity)
        return true
    }

    fun resume() {
        bannerAdView?.resume()
    }

    fun pause() {
        bannerAdView?.pause()
    }

    fun destroy() {
        bannerAdView?.destroy()
        bannerAdView = null
        interstitialAd = null
    }
}
