package com.sina.library.views.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.bundle.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sina.library.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin
import org.koin.core.parameter.parametersOf
import java.lang.reflect.ParameterizedType
import java.util.Locale
import kotlin.reflect.KClass

abstract class BaseSht<VB : ViewBinding, VM : BaseVM<*, *>> : BottomSheetDialogFragment() {
    protected open val TAG: String get() = this::class.java.simpleName
    private var _binding: VB? = null
    protected val binding get() = _binding!!
    private var currentToast: Toast? = null
    private var resizePercentage: Int? = null // Null means use default XML height
    protected open val navGraphScope: String? = null
    private val flowJobs = mutableListOf<Job>() // ✅ Store Flow collection jobs

    protected val viewModel: VM by lazy { createViewModel() }
    abstract fun setupViews()
    abstract fun observeViewModel()
    private fun createViewModel(): VM {
        val type =
            (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<VM>
        val kClass = type.kotlin as KClass<VM>
        return navGraphScope?.let {
            getKoin().getScope(it).get(kClass)
        } ?: run {
            val savedStateHandle = createSavedStateHandle() // ✅ Get SavedStateHandle
            getKoin().get(kClass, parameters = { parametersOf(savedStateHandle) })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeViewModel()
    }

    private fun setLayoutDirection(languageCode: String) {
        binding.root.layoutDirection = if (languageCode == "fa") View.LAYOUT_DIRECTION_RTL
        else View.LAYOUT_DIRECTION_LTR
    }

    open fun refreshUIForLanguage(language: String) {
        // Optional: Implement in child Fragments if needed
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    lateinit var navControllerFragment: NavController

    fun setNavController(controller: NavController) {
        this.navControllerFragment = controller
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    fun showToast(msgRes: String) {
        Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show()
    }

    @Suppress("UNCHECKED_CAST")
    private fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB {
        val type =
            (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<VB>
        return type.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        ).invoke(null, inflater, container, false) as VB
    }

    private fun createSavedStateHandle(): SavedStateHandle {
        val args = arguments ?: Bundle()
        return SavedStateHandle(args.toMap()) // Converts Bundle to SavedStateHandle
    }

    private fun Bundle.toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        keySet().forEach { key -> map[key] = get(key) }
        return map
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    fun resize(percent: Int) {
        if (percent in 10..100) resizePercentage = percent
        else throw IllegalArgumentException("resize percentage must be between 10 and 100")
    }

    fun <T> launchWhenCreated(flow: Flow<T>, collector: suspend (T) -> Unit) {
        flowJobs.add(viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                flow.collect { value -> collector(value) }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.let { bottomSheet ->
                resizePercentage?.let { percent ->
                    bottomSheet.layoutParams.height =
                        (resources.displayMetrics.heightPixels * (percent / 100.0)).toInt()
                }

                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun dismissDialog() {
        dismiss()
        dismissNow()
    }
}