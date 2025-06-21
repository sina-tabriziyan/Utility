package com.sina.library.views.dialog.btmsheet.simpledialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding

class SimpleDialog<B : ViewBinding>(
    private val bindingClass: Class<B>,
    private val setup: (B, DialogInterface) -> Unit
) : DialogFragment() {

    private var _binding: B? = null
    protected val binding get() = _binding!!

    fun isShowing() = dialog?.isShowing ==true

    @SuppressLint("UseGetLayoutInflater")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        val inflater = LayoutInflater.from(requireContext())

        // Inflate the ViewBinding dynamically
        val method = bindingClass.getMethod("inflate", LayoutInflater::class.java)
        _binding = method.invoke(null, inflater) as B
        dialog.setContentView(binding.root)

        // Apply setup function
        setup(binding, dialog)

        return dialog
    }

    override fun onStart() {
        super.onStart()
        // Ensure the dialog width matches the XML (match_parent) and height wraps content
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,  // Makes dialog width match XML
            ViewGroup.LayoutParams.WRAP_CONTENT  // Ensures height wraps content
        )

        // Ensures dialog uses default style (not transparent)
        dialog?.window?.setBackgroundDrawableResource(android.R.drawable.dialog_holo_light_frame)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun show(context: Context) {
        val fragmentManager = when (context) {
            is FragmentActivity -> context.supportFragmentManager
            is Fragment -> context.parentFragmentManager
            else -> throw IllegalArgumentException("Context must be an instance of FragmentActivity or Fragment")
        }
        show(fragmentManager, "CustomDialog")
    }
}