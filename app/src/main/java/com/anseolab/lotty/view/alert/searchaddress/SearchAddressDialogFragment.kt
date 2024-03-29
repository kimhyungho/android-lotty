package com.anseolab.lotty.view.alert.searchaddress

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import com.anseolab.lotty.R
import com.anseolab.lotty.databinding.FragmentSearchAddressDialogBinding
import com.anseolab.lotty.extensions.throttle
import com.anseolab.lotty.view.adapter.RecentAddressListAdapter
import com.anseolab.lotty.view.base.FragmentLauncher
import com.anseolab.lotty.view.base.ViewModelDialogFragment
import com.jakewharton.rxbinding4.view.clicks
import dagger.hilt.android.AndroidEntryPoint
import kotlin.reflect.KClass

@AndroidEntryPoint
class SearchAddressDialogFragment :
    ViewModelDialogFragment<FragmentSearchAddressDialogBinding, SearchAddressViewModelType>(
        R.layout.fragment_search_address_dialog
    ) {
    private val _viewModel: SearchAddressViewModel by viewModels()
    override val viewModel: SearchAddressViewModelType get() = _viewModel

    private val recentAddressListAdapter = RecentAddressListAdapter().apply {
        listener = object : RecentAddressListAdapter.Listener {
            override fun onTextClick(address: String) {
                mListener?.onSearchButtonClick(address)
                dismiss()
            }

            override fun onRemoveButtonClick(address: String) {
                viewModel.input.onAddressRemoveButtonClick(address)
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        dialog?.window?.attributes?.windowAnimations = R.style.SlideAnimation
    }

    override fun onWillAttachViewModel(
        viewDataBinding: FragmentSearchAddressDialogBinding,
        viewModel: SearchAddressViewModelType
    ) {
        super.onWillAttachViewModel(viewDataBinding, viewModel)

        with(viewDataBinding) {
            rvRecent.adapter = recentAddressListAdapter

            btnClear.clicks()
                .throttle()
                .bind {
                    viewModel.input.onAddressClearButtonClick()
                }

            ibBack.clicks()
                .bind {
                    dismiss()
                }

            etAddress.setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_SEARCH -> {
                        val address = _viewModel.currentState.address
                        mListener?.onSearchButtonClick(address)
                        dismiss()
                    }
                }
                true
            }
        }
    }

    override fun onDestroyView() {
        mListener = null
        super.onDestroyView()
    }

    companion object : FragmentLauncher<SearchAddressDialogFragment>() {
        override val fragmentClass: KClass<SearchAddressDialogFragment>
            get() = SearchAddressDialogFragment::class

        var mListener: Listener? = null

        interface Listener {
            fun onSearchButtonClick(address: String)
        }
    }

}