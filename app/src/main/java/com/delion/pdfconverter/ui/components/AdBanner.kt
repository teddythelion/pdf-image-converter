package com.delion.pdfconverter.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
// Test banner ad unit. Replace with your real ID before publishing.
private const val BANNER_AD_UNIT_ID = "ca-app-pub-9665203597204603/1011640439"

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BANNER_AD_UNIT_ID
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("AdBanner", "Ad loaded successfully")
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e("AdBanner", "Ad failed: ${error.message}")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}