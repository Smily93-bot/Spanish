package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.example.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Rewarded video ads ("watch a short video, get a diamond"). One ad is kept loaded in the
 * background so it can start straight away. Ads are requested as child-directed and rated G.
 */
object RewardedAds {
    private var ad: RewardedAd? = null
    private var loading = false

    fun init(context: Context) {
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build()
        )
        MobileAds.initialize(context) {}
        load(context)
    }

    private fun load(context: Context) {
        if (ad != null || loading) return
        loading = true
        RewardedAd.load(
            context.applicationContext,
            context.getString(R.string.admob_rewarded_id),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: RewardedAd) {
                    ad = loaded
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                }
            }
        )
    }

    /**
     * Plays the video. [onReward] runs once it has been watched; [onClosed] when it is gone.
     * Returns false (and starts loading one) when no video is ready yet.
     */
    fun show(context: Context, onReward: () -> Unit, onClosed: () -> Unit = {}): Boolean {
        val activity = context.findActivity() ?: return false
        val current = ad ?: run {
            load(context)
            return false
        }
        ad = null
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                onClosed()
                load(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                onClosed()
                load(activity)
            }
        }
        current.show(activity) { onReward() }
        return true
    }

    private fun Context.findActivity(): Activity? {
        var c: Context? = this
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }
}
