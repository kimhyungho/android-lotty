package com.anseolab.lotty.view.alert.scanner

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.Toast
import com.anseolab.lotty.R
import com.anseolab.lotty.databinding.FragmentScannerDialogBinding
import com.anseolab.lotty.providers.permissions.PermissionProvider
import com.anseolab.lotty.view.base.BaseDialogFragment
import com.anseolab.lotty.view.base.FragmentLauncher
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.reflect.KClass

@AndroidEntryPoint
class ScannerAlertDialog : BaseDialogFragment<FragmentScannerDialogBinding>(
    R.layout.fragment_scanner_dialog
) {
    private lateinit var codeScanner: CodeScanner
    private val handler = object : Handler(Looper.getMainLooper()) {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewDataBinding) {
            with(webView) {
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = false
                settings.javaScriptEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
            }

            with(scannerView) {
                codeScanner = CodeScanner(requireContext(), this)
                codeScanner.decodeCallback = DecodeCallback {
                    if (it.text.startsWith("http://m.dhlottery.co.kr")) {
                        webView.post { webView.loadUrl(it.text) }
                    } else {
                        handler.post {
                            Toast.makeText(
                                requireContext(),
                                "복권 qr이 아닙니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                scannerView.setOnClickListener {
                    codeScanner.startPreview()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onDestroyView() {
        codeScanner.releaseResources()
        super.onDestroyView()
    }

    companion object : FragmentLauncher<ScannerAlertDialog>() {
        override val fragmentClass: KClass<ScannerAlertDialog>
            get() = ScannerAlertDialog::class
    }
}