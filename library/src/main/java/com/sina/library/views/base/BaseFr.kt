package com.sina.library.views.base

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin
import org.koin.core.parameter.parametersOf
import java.lang.reflect.ParameterizedType
import java.util.Locale

abstract class BaseFr<VB : ViewBinding, VM : BaseVM<*, *>> : Fragment() {
    protected open val TAG: String get() = this::class.java.simpleName
    private var _binding: VB? = null
    protected val binding get() = _binding!!
    private val flowJobs = mutableListOf<Job>()
    lateinit var navControllerFragment: NavController
    private var currentToast: Toast? = null


    protected val viewModel: VM by lazy {
        val viewModelClass = (javaClass.genericSuperclass as ParameterizedType)
            .actualTypeArguments[1] as Class<VM>
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val argumentsMap =
                    arguments?.keySet()?.associateWith { arguments?.get(it) } ?: emptyMap()
                val savedStateHandle = SavedStateHandle(argumentsMap)
                return getKoin().get(viewModelClass.kotlin) { parametersOf(savedStateHandle) }
            }
        })[viewModelClass]
    }

    fun setNavController(controller: NavController) {
        this.navControllerFragment = controller
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setupViews()
        observeViewModel()
    }

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


    override fun onDestroyView() {
        super.onDestroyView()
        viewLifecycleOwner.lifecycleScope.coroutineContext.cancelChildren()
        flowJobs.forEach { it.cancel() }
        flowJobs.clear()
        _binding = null
    }

    fun <T> launchWhenCreated(flow: Flow<T>, collector: suspend (T) -> Unit) {
        flowJobs.add(viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                flow.collect { value -> collector(value) }
            }
        })
    }

    abstract fun setupViews()
    abstract fun observeViewModel()

    private fun setLayoutDirection(languageCode: String) {
        binding.root.layoutDirection = if (languageCode == "fa") View.LAYOUT_DIRECTION_RTL
        else View.LAYOUT_DIRECTION_LTR
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    open fun refreshUIForLanguage(language: String) {
        // Optional: Implement in child Fragments if needed
    }

    fun updateResources(context: Context, language: String): Context? {
        val locale = Locale(language)
        Locale.setDefault(locale)
        return context.createConfigurationContext(context.resources.configuration.apply {
            setLocale(locale)
            setLayoutDirection(locale)
        })
    }

    fun changeDirection(language: String?, mainLayout: ViewGroup) {
        val locale =
            if (language.equals("fa", ignoreCase = true)) Locale("fa") else Locale(language)
        Locale.setDefault(locale)
        resources.configuration.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            requireActivity().createConfigurationContext(resources.configuration)

        resources.updateConfiguration(resources.configuration, resources.displayMetrics)
        language?.let { updateResources(requireContext(), it) }
        ContextUtils.updateLocale(requireContext(), Locale(language))

        val isRtl = language.equals("fa", ignoreCase = true)
        requireActivity().window.decorView.layoutDirection =
            if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        mainLayout.layoutDirection =
            if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
    }

}
