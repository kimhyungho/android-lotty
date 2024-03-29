package com.anseolab.lotty.view.main.around

import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.anseolab.lotty.R
import com.anseolab.lotty.databinding.FragmentAroundBinding
import com.anseolab.lotty.extensions.throttle
import com.anseolab.lotty.utils.kakaomap.KakaoMapUtils
import com.anseolab.lotty.view.alert.searchaddress.SearchAddressDialogFragment
import com.anseolab.lotty.view.base.FragmentLauncher
import com.anseolab.lotty.view.base.ViewModelFragment
import com.jakewharton.rxbinding4.view.clicks
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.util.FusedLocationSource
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs
import kotlin.reflect.KClass

@AndroidEntryPoint
class AroundFragment : ViewModelFragment<FragmentAroundBinding, AroundViewModelType>(
    R.layout.fragment_around
) {
    private val _viewModel: AroundViewModel by viewModels()
    override val viewModel: AroundViewModelType get() = _viewModel

    private var oldLong: Double? = null
    private var oldLat: Double? = null

    private var selectedMarker: Marker? = null
    private val currentMarkers: MutableList<Marker> = mutableListOf()

    private var mNaverMap: NaverMap? = null
    private val mLocationSource = FusedLocationSource(this, PERMISSION_REQUEST_CODE)

    private val slideUpAnim by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_from_bottom)
    }
    private val slideDownAnim by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.slide_out_to_bottom).apply {
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    viewDataBinding.layoutStoreInfo.visibility = View.GONE
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }
    }

    override fun onWillAttachViewModel(
        viewDataBinding: FragmentAroundBinding,
        viewModel: AroundViewModelType
    ) {
        super.onWillAttachViewModel(viewDataBinding, viewModel)

        (childFragmentManager.findFragmentById(R.id.map) as? MapFragment)?.run {
            getMapAsync { naverMap ->
                mNaverMap = naverMap

                with(mNaverMap!!) {
                    locationSource = mLocationSource
                    locationTrackingMode = LocationTrackingMode.Follow
                    uiSettings.isRotateGesturesEnabled = false
                    uiSettings.isTiltGesturesEnabled = false
                    setOnMapClickListener { _, _ ->
                        selectedMarker?.icon = OverlayImage.fromResource(R.drawable.ic_clover)
                        selectedMarker = null
                        viewModel.input.onMapClick()
                    }

                    addOnCameraIdleListener {
                        if (oldLong == null) oldLong = this.cameraPosition.target.longitude
                        if (oldLat == null) oldLat = this.cameraPosition.target.longitude

                        if (abs(oldLong!!.minus(this.cameraPosition.target.longitude)) > 0.001 ||
                            abs(oldLat!!.minus(this.cameraPosition.target.latitude)) > 0.001
                        ) {
                            viewModel.input.onCameraIdleChange(
                                this.cameraPosition.target.longitude,
                                this.cameraPosition.target.latitude
                            )
                            oldLong = this.cameraPosition.target.longitude
                            oldLat = this.cameraPosition.target.latitude
                        }
                    }

                }
            }
        }

        with(viewDataBinding) {
            btnMyLocation.clicks()
                .bind {
                    val myLocation = mNaverMap!!.locationOverlay.position
                    mNaverMap!!.cameraPosition = CameraPosition(myLocation, 13.0)
                }

            tvSearch.clicks()
                .throttle()
                .bind {
                    SearchAddressDialogFragment.apply {
                        mListener = object : SearchAddressDialogFragment.Companion.Listener {
                            override fun onSearchButtonClick(address: String) {
                                viewModel.input.onSearchButtonClick(address)
                            }
                        }
                    }.getInstance().show(childFragmentManager, SearchAddressDialogFragment.name)
                }
            btnKakaoMap.clicks()
                .throttle()
                .bind {
                    val startPoint = mNaverMap!!.locationOverlay.position

                    if (KakaoMapUtils.checkInstalledKakaoMap(requireContext())) {
                        KakaoMapUtils.openKakaoMapForSearch(
                            requireContext(),
                            "복권 판매점",
                            startPoint.latitude,
                            startPoint.longitude
                        )
                    } else {
                        KakaoMapUtils.openPlayStoreForKakaoMap(requireContext())
                    }
                }

            btnNavigation.clicks()
                .throttle()
                .bind {
                    val startPoint = mNaverMap!!.locationOverlay.position
                    val destPoint = _viewModel.currentState.store

                    if (destPoint?.x == null) {
                        Toast.makeText(requireContext(), "목적지를 찾을 수 없습니다.", Toast.LENGTH_SHORT)
                            .show()
                        return@bind
                    }

                    if (KakaoMapUtils.checkInstalledKakaoMap(requireContext())) {
                        KakaoMapUtils.openKakaoMapForRoute(
                            requireContext(),
                            startPoint.latitude,
                            startPoint.longitude,
                            destPoint.y,
                            destPoint.x
                        )
                    } else {
                        KakaoMapUtils.openPlayStoreForKakaoMap(requireContext())
                    }
                }
        }

        with(viewModel.output) {
            showError.observe {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }

            showStoreInfo.observe {
                if (it) showStoreInformation()
                else hideStoreInformation()
            }

            stores.observe { stores ->
                val fistStore = stores.firstOrNull()

                if (fistStore != null && _viewModel.currentState.showStoreLocation) mNaverMap!!.cameraPosition =
                    CameraPosition(LatLng(fistStore.y, fistStore.x), 13.0)

                for (marker in currentMarkers) {
                    if (marker != selectedMarker)
                        marker.map = null
                }
                currentMarkers
                    .removeIf { marker -> marker != selectedMarker }

                for (store in stores) {
                    val storeLocation = LatLng(store.y, store.x)
                    if (selectedMarker?.position?.latitude != store.y && selectedMarker?.position?.longitude != store.x) {
                        val marker = Marker().apply {
                            position = storeLocation
                            icon = OverlayImage.fromResource(R.drawable.ic_clover)
                            map = mNaverMap
                            setOnClickListener {
                                viewModel.input.onMarkerClick(store)
                                selectedMarker?.icon =
                                    OverlayImage.fromResource(R.drawable.ic_clover)
                                selectedMarker = this
                                selectedMarker?.icon =
                                    OverlayImage.fromResource(R.drawable.ic_lucky_clover_48x60)
                                true
                            }
                        }
                        currentMarkers.add(marker)
                    }
                }
            }
        }
    }

    private fun hideStoreInformation() {
        viewDataBinding.layoutStoreInfo.startAnimation(slideDownAnim)
    }

    private fun showStoreInformation() {
        viewDataBinding.layoutStoreInfo.visibility = View.VISIBLE
        viewDataBinding.layoutStoreInfo.startAnimation(slideUpAnim)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (mLocationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!mLocationSource.isActivated) {
                return
            }
            mNaverMap!!.locationTrackingMode = LocationTrackingMode.Follow
        }
    }

    override fun onDestroyView() {
        mNaverMap?.removeOnCameraIdleListener {  }
        mNaverMap = null
        super.onDestroyView()
    }

    companion object : FragmentLauncher<AroundFragment>() {
        const val PERMISSION_REQUEST_CODE = 99

        override val fragmentClass: KClass<AroundFragment>
            get() = AroundFragment::class
    }
}