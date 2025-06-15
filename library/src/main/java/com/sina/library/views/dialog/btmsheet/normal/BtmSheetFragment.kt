/**
 * Created by ST on 2/11/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.views.dialog.btmsheet.normal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BtmSheetFragment<B : ViewBinding>(
    private val bindingClass: Class<B>,
    private val setup: (B) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: B? = null
    protected val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val method = bindingClass.getMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java)
        _binding = method.invoke(null, inflater, container, false) as B
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setup(binding)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun <B : ViewBinding> newInstance(
            bindingClass: Class<B>,
            setup: (B) -> Unit
        ): BtmSheetFragment<B> {
            return BtmSheetFragment(bindingClass, setup)
        }
    }
}
