/**
 * Created by st on 2/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.views.extensions

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.drawToBitmap
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import coil.transform.CircleCropTransformation
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sina.library.data.model.ScreenShot
import com.sina.library.network.client.provideUnsafeImageClient
import com.sina.library.utility.R
import com.sina.library.views.customview.FontIcon
import com.sina.library.views.extensions.StringExtension.fromURI
import okhttp3.Headers
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A collection of extension functions to enhance the usage of Views and UI components.
 * Designed to be used in multiple projects as part of a library.
 */
object ViewExtensions {

    // ---------------- View Visibility & Enabling ----------------

    /** Show or hide a view using VISIBLE or GONE */
    fun View.show(isVisible: Boolean) {
        visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    /** Show or hide a view using VISIBLE or INVISIBLE */
    fun View.setInvisible(isInvisible: Boolean) {
        visibility = if (isInvisible) View.INVISIBLE else View.VISIBLE
    }

    /** Enable or disable a view */
    fun View.enable(isEnabled: Boolean) {
        this.isEnabled = isEnabled
        alpha = if (isEnabled) 1f else 0.5f
    }

    /** Toggle visibility between VISIBLE and GONE */
    fun View.toggleVisibility() {
        visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    /** Toggle visibility between VISIBLE and INVISIBLE */
    fun View.toggleInvisibility() {
        visibility = if (visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
    }

    // ---------------- Click Listeners ----------------

    /** Simplified click listener */
    inline fun View.onClick(crossinline action: () -> Unit) = setOnClickListener { action() }

    /** Prevent double clicks with a delay */
    fun View.preventDoubleClick(delay: Long = 500L, action: () -> Unit) {
        isEnabled = false
        postDelayed({ isEnabled = true }, delay)
        action()
    }

    // ---------------- Keyboard Extensions ----------------

    /** Show or hide the keyboard inside a Fragment */
    fun Fragment.toggleKeyboard(editText: AppCompatEditText, show: Boolean) {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (show) imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED)
        else imm.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    // ---------------- SearchView Extensions ----------------

    /** Listen for query text changes in SearchView */
    inline fun SearchView.onQueryChange(crossinline listener: (String) -> Unit) {
        setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true
            override fun onQueryTextChange(newText: String?) =
                listener(newText.orEmpty()).let { true }
        })
    }

    // ---------------- RecyclerView Extensions ----------------

    /** Scroll to a specific position with optional smooth scrolling */
    fun RecyclerView.scrollToPosition(position: Int, smooth: Boolean = true) {
        post {
            (layoutManager as? LinearLayoutManager)?.let {
                if (smooth) it.smoothScrollToPosition(this, RecyclerView.State(), position)
                else it.scrollToPosition(position)
            }
        }
    }

    /** Get the first visible item in RecyclerView */
    fun RecyclerView.getFirstVisibleItem(): Int {
        return (layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() ?: -1
    }

    // ---------------- FloatingActionButton Extensions ----------------

    /** Auto-hide FloatingActionButton on scroll */
    fun FloatingActionButton.autoHideOnScroll(recyclerView: RecyclerView) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var isVisible = true
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && isVisible) {
                    hide()
                    isVisible = false
                } else if (dy < 0 && !isVisible) {
                    show()
                    isVisible = true
                }
            }
        })
    }

    // ---------------- Toolbar Extensions ----------------

    /** Setup a Toolbar with a title and back button */
    fun Toolbar.setup(title: String, enableBackButton: Boolean = true) {
        this.title = title
        if (enableBackButton) {
            setNavigationOnClickListener { (context as? Activity)?.onBackPressed() }
        }
    }

    // ---------------- Drawable to Bitmap ----------------

    /** Convert a Drawable to Bitmap */
    fun Drawable.toBitmap(): Bitmap? {
        return (this as? BitmapDrawable)?.bitmap ?: Bitmap.createBitmap(
            intrinsicWidth,
            intrinsicHeight,
            Bitmap.Config.ARGB_8888
        ).apply {
            val canvas = Canvas(this)
            setBounds(0, 0, width, height)
            draw(canvas)
        }
    }

    // ---------------- SeekBar Extensions ----------------

    /** Listen for SeekBar progress changes */
    inline fun SeekBar.onProgressChanged(crossinline action: (progress: Int, fromUser: Boolean) -> Unit) {
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) action(progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // ---------------- DrawerLayout Extensions ----------------

    /** Execute an action when the DrawerLayout is opened */
    inline fun DrawerLayout.onOpened(crossinline action: () -> Unit) {
        addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) = action()
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    // ---------------- Animation Extensions ----------------

    /** Animate a view’s translation with parameters */
    fun View.animateTranslation(targetX: Float, duration: Long = 500L) {
        ObjectAnimator.ofFloat(this, "translationX", targetX).apply {
            this.duration = duration
            start()
        }
    }


    fun View.setDrawableClickListener(
        onDrawableStartClick: (() -> Unit)? = null,
        onDrawableTopClick: (() -> Unit)? = null,
        onDrawableEndClick: (() -> Unit)? = null,
        onDrawableBottomClick: (() -> Unit)? = null
    ) {
        setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val editText = v as AppCompatEditText
                val compoundDrawables = editText.compoundDrawables
                compoundDrawables.forEachIndexed { index, drawable ->
                    drawable?.let {
                        val bounds = it.bounds
                        val x = event.x.toInt()
                        val y = event.y.toInt()
                        when (index) {
                            0 -> { // DrawableStart
                                if (x >= editText.paddingLeft && x <= editText.paddingLeft + bounds.width() &&
                                    y >= editText.paddingTop && y <= editText.height - editText.paddingBottom
                                ) {
                                    onDrawableStartClick?.invoke()
                                    editText.performClick() // Notify accessibility services
                                    return@setOnTouchListener true
                                }
                            }

                            1 -> { // DrawableTop
                                if (x >= editText.paddingLeft && x <= editText.width - editText.paddingRight &&
                                    y >= editText.paddingTop && y <= editText.paddingTop + bounds.height()
                                ) {
                                    onDrawableTopClick?.invoke()
                                    editText.performClick()
                                    return@setOnTouchListener true
                                }
                            }

                            2 -> { // DrawableEnd
                                if (x >= editText.width - editText.paddingRight - bounds.width() && x <= editText.width - editText.paddingRight &&
                                    y >= editText.paddingTop && y <= editText.height - editText.paddingBottom
                                ) {
                                    onDrawableEndClick?.invoke()
                                    editText.performClick()
                                    return@setOnTouchListener true
                                }
                            }

                            3 -> { // DrawableBottom
                                if (x >= editText.paddingLeft && x <= editText.width - editText.paddingRight &&
                                    y >= editText.height - editText.paddingBottom - bounds.height() && y <= editText.height - editText.paddingBottom
                                ) {
                                    onDrawableBottomClick?.invoke()
                                    editText.performClick()
                                    return@setOnTouchListener true
                                }
                            }
                        }
                    }
                }
            }
            false
        }
    }

    fun getStringResourceByName(context: Context, aString: String): String {
        val resId = context.resources.getIdentifier(aString, "string", context.packageName)

        // Check if resId is valid
        return if (resId != 0) {
            context.getString(resId)
        } else {
            // Handle the case when the resource is not found, for example, return a default value or log an error
            "Resource not found"
        }
    }

    /**
     * Extension function for EditText to listen for text changes and detect typing status.
     *
     * @param onTextChanged Callback function invoked with the current text and typing status.
     */
    fun EditText.onTextTypingStatusChanged(onTextChanged: (text: String, isTyping: Boolean) -> Unit) {
        val typingDelayMillis = 1000L // Delay to determine typing has stopped
        val handler = Handler(Looper.getMainLooper())
        var isTyping = false

        val typingTimeout = Runnable {
            isTyping = false
            onTextChanged(this.text.toString(), isTyping)
        }

        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // No action needed here
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed here
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                handler.removeCallbacks(typingTimeout)
                if (!isTyping) {
                    isTyping = true
                    onTextChanged(s?.toString().orEmpty(), isTyping)
                }
                handler.postDelayed(typingTimeout, typingDelayMillis)
            }
        })
    }


    fun Spinner.onItemSelected(action: (parent: AdapterView<*>, view: View?, position: Int, id: Long) -> Unit) {
        this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                action(parent, view, position, id)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No action needed
            }
        }
    }

    fun TextView.setIconBuilder(icon: String, color: Int) {
        val teamTypeface = Typeface.createFromAsset(this.context.assets, "teamyarfont.ttf")
        val iconTeamyar = String(Character.toChars(icon.toInt(16)))
        this.apply {
            typeface = teamTypeface
            text = iconTeamyar
            setTextColor(ContextCompat.getColor(this.context, color))
        }
    }

    fun AppBarLayout.onAppBarLayoutStateChange(
        onCollapsing: () -> Unit,
        onNotCollapsing: () -> Unit
    ) {
        addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (kotlin.math.abs(verticalOffset) == appBarLayout.totalScrollRange) onCollapsing()  // Call the collapsing lambda
            else onNotCollapsing()  // Call the not collapsing lambda
        }
    }

    fun RecyclerView.scrollToPositionWithHighlight(
        position: Int,
        highlightColor: Int,
        originalColor: Int,
        duration: Long = 2000L // Duration of the animation (2 seconds)
    ) {
        // Scroll to the desired position
        scrollToPosition(position)

        // Wait for the RecyclerView to complete its layout
        post {
            val viewHolder = findViewHolderForAdapterPosition(position) ?: return@post

            // Animate the background color transition
            ObjectAnimator.ofObject(
                viewHolder.itemView,
                "backgroundColor",
                ArgbEvaluator(),
                highlightColor,
                originalColor
            ).apply {
                this.duration = duration
                start()
            }
        }
    }

    fun TextView.isBold(isBold: Boolean) {
        val style = if (isBold) Typeface.BOLD else Typeface.NORMAL
        this.setTypeface(null, style)
    }


    inline fun DrawerLayout.onDrawerOpened(crossinline action: () -> Unit) {
        addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) {
                action()
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    fun convertDrawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        var bitmap: Bitmap? = null
        try {
            bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.minimumHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bitmap
    }

    fun AppCompatEditText.onTyping(action: (Boolean) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No need to handle this
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Only trigger if text is actually typed (not set programmatically)
                if (before != count) {
                    action(s?.isNotEmpty() == true)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // No need to handle this
            }
        })
    }

    fun AppCompatEditText.onEmpty(action: (Boolean) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No need to handle this
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // When text changes, check if it's empty
                action(s.isNullOrEmpty())
            }

            override fun afterTextChanged(s: Editable?) {
                // No need to handle this
            }
        })
    }

    fun TextView.startCustomScrolling() {
        val textWidth = paint.measureText(text.toString())
        if (textWidth > width) { // Only scroll if text overflows
            val animator = ObjectAnimator.ofFloat(this, "translationX", width.toFloat(), -textWidth)
            animator.duration = 5000L
            animator.repeatCount = ObjectAnimator.INFINITE
            animator.interpolator = LinearInterpolator()
            animator.start()
        }
    }


    fun TextView.enableSlideShow() {
        this.isSingleLine = true // Ensure text is a single line
        this.ellipsize = TextUtils.TruncateAt.MARQUEE // Enable marquee
        this.marqueeRepeatLimit = -1 // Infinite scrolling
        this.isFocusable = true // Required for marquee
        this.isFocusableInTouchMode = true // Required for marquee
        this.isSelected = true // Crucial to trigger marquee effect
        this.setHorizontallyScrolling(true) // Enable horizontal scrolling
    }

    fun AppCompatEditText.editTextTyping(typing: () -> Unit, clearTyping: () -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) clearTyping()
                else typing()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    fun View.outcomeDirection() {
        this.layoutDirection = View.LAYOUT_DIRECTION_RTL
    }

    fun View.incomeDirection() {
        this.layoutDirection = View.LAYOUT_DIRECTION_LTR
    }


    inline fun SeekBar.setOnProgressChangedListener(crossinline action: (progress: Int, fromUser: Boolean) -> Unit) {
        this.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) action(progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // No action needed
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // No action needed
            }
        })
    }

    fun String.extractMessageAndImage(): Pair<String?, String?> {
        val doc = Jsoup.parse(this)
        val message = doc.select("p").first()?.text()
        val imageSrc = doc.select("img").first()?.attr("src")
        return Pair(message, imageSrc)
    }

    // Extension function for setting visibility in a cleaner way
    fun View.setVisibleGone(isVisible: Boolean) {
        visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    fun View.setVisibleInvisible(isVisible: Boolean) {
        visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
    }

    fun Fragment.showKeyboardAndFocusEditText(editText: AppCompatEditText) {
        editText.requestFocus()
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    fun Fragment.closeKeyboardAndFocusEditText(editText: AppCompatEditText) {
        editText.requestFocus()
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    fun String.copyToClipboard(activity: Activity) {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("copy", this.fromURI()))
    }

    fun FloatingActionButton.hideAndShowByScrolling(recyclerView: RecyclerView) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private val scrollThreshold = 8
            private var scrolledDistance = 0
            private var controlsVisible = true

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    scrolledDistance = 0
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (controlsVisible && scrolledDistance > scrollThreshold && dy > 0) {
                    hide()
                    controlsVisible = false
                    scrolledDistance = 0
                } else if (!controlsVisible && scrolledDistance < -scrollThreshold && dy < 0) {
                    show()
                    controlsVisible = true
                    scrolledDistance = 0
                }
                if ((controlsVisible && dy > 0) || (!controlsVisible && dy < 0)) {
                    scrolledDistance += dy
                }
            }
        })
    }

    fun RecyclerView.scrollToBottom() {
        val layoutManager = this.layoutManager as? LinearLayoutManager
        if (layoutManager != null)
            this.post { layoutManager.smoothScrollToPosition(this, RecyclerView.State(), 0) }
    }

    fun RecyclerView.scrollToFirst() {
        val adapter = this.adapter
        val layoutManager = this.layoutManager as? LinearLayoutManager
        if (adapter != null && layoutManager != null) {
            val lastPosition = adapter.itemCount - 1
            if (lastPosition >= 0) this.post {
                layoutManager.smoothScrollToPosition(
                    this,
                    RecyclerView.State(),
                    lastPosition
                )
            }
        }
    }

    fun FloatingActionButton.snapToBottomState(
        recyclerView: RecyclerView,
        adapter: RecyclerView.Adapter<*>
    ) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE)
                    visibility =
                        if ((recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() == 0 || recyclerView.childCount == adapter.itemCount) View.GONE else View.VISIBLE
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                visibility =
                    if (dy < 0 || recyclerView.canScrollVertically(1)) View.VISIBLE else View.GONE
            }
        })
    }

    fun View.onClickListener(action: () -> Unit) {
        setOnClickListener { action() }
    }

    fun View.disable() {
        isEnabled = false
    }

    fun View.enable() {
        isEnabled = true
    }

    fun View.gone() {
        visibility = View.GONE
    }

    fun View.visible() {
        visibility = View.VISIBLE
    }

    fun View.invisible() {
        visibility = View.INVISIBLE
    }

    fun Uri.createBitmap(context: Context): Bitmap? {
        val contentResolver: ContentResolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, this)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, this)
        }
    }

    fun cropImageInPictures(context: Context, view: View, screenShotData: ScreenShot): Uri? {
        val originalBitmap = view.drawToBitmap()

        val srcWidth = originalBitmap.width
        val srcHeight = originalBitmap.height

        if (screenShotData.width == srcWidth && screenShotData.height == srcHeight) {
            return null
        }

        val scale =
            (screenShotData.width.toFloat() / srcWidth).coerceAtLeast(screenShotData.height.toFloat() / srcHeight)
        val m = Matrix()
        m.setScale(scale, scale)

        val srcCroppedW = (screenShotData.width / scale).roundToInt()
        val srcCroppedH = (screenShotData.height / scale).roundToInt()
        var srcX = (srcWidth * screenShotData.hCenterPercent - srcCroppedW / 2).toInt()
        var srcY = (srcHeight * screenShotData.vCenterPercent - srcCroppedH / 2).toInt()

        srcX = srcX.coerceAtMost(srcWidth - srcCroppedW).coerceAtLeast(0)
        srcY = srcY.coerceAtMost(srcHeight - srcCroppedH).coerceAtLeast(0)

        val croppedBitmap =
            Bitmap.createBitmap(originalBitmap, srcX, srcY, srcCroppedW, srcCroppedH)
        originalBitmap.recycle()

        // ✅ Fix: Use MediaStore to save the image without modifying _data
        val contentValues = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "mapimage_${screenShotData.latitude}_${screenShotData.longitude}.jpg"
            )
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/${screenShotData.rootFolder}/${screenShotData.childFolder}"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val contentResolver = context.contentResolver
        val imageUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return null // ✅ Ensure URI is not null

        try {
            contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            contentValues.clear()
            contentValues.put(
                MediaStore.Images.Media.IS_PENDING,
                0
            ) // ✅ Mark the image as available
            contentResolver.update(imageUri, contentValues, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
            contentResolver.delete(imageUri, null, null) // ✅ Remove incomplete file if error occurs
            return null
        }

        croppedBitmap.recycle()

        return imageUri
    }

    fun saveImageToFileSystem(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        rootFolder: String,
        childFolder: String
    ): Uri? {
        val rootDir = File(Environment.getExternalStorageDirectory(), rootFolder)
        val subDir = File(rootDir, childFolder)

        if (!subDir.exists()) {
            val success = subDir.mkdirs()
            if (!success) {
                Log.e("SaveImage", "Failed to create directory: ${subDir.absolutePath}")
                return null
            }
        }

        val file = File(subDir, fileName)
        return try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/jpeg"),
                null
            )
            file.toUri()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveImageToMediaStore(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        primaryFolder: String,
        subFolder: String
    ): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "$primaryFolder/$subFolder"
            ) // ✅ Save inside Pictures/Teamyar
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val contentResolver = context.contentResolver
        val uri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return null

        return try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            contentResolver.delete(uri, null, null)
            null
        }
    }

    fun cropImage(context: Context, view: View, screenShotData: ScreenShot): Uri? {
        val originalBitmap = view.drawToBitmap()

        val srcWidth = originalBitmap.width
        val srcHeight = originalBitmap.height

        if (screenShotData.width == srcWidth && screenShotData.height == srcHeight) {
            Log.e("CropImage", "No need to crop, returning null")
            return null
        }

        val scale =
            (screenShotData.width.toFloat() / srcWidth).coerceAtLeast(screenShotData.height.toFloat() / srcHeight)
        val m = Matrix()
        m.setScale(scale, scale)

        val srcCroppedW = (screenShotData.width / scale).roundToInt()
        val srcCroppedH = (screenShotData.height / scale).roundToInt()
        var srcX = (srcWidth * screenShotData.hCenterPercent - srcCroppedW / 2).toInt()
        var srcY = (srcHeight * screenShotData.vCenterPercent - srcCroppedH / 2).toInt()

        srcX = srcX.coerceAtMost(srcWidth - srcCroppedW).coerceAtLeast(0)
        srcY = srcY.coerceAtMost(srcHeight - srcCroppedH).coerceAtLeast(0)

        val croppedBitmap =
            Bitmap.createBitmap(originalBitmap, srcX, srcY, srcCroppedW, srcCroppedH)
        originalBitmap.recycle()

        val fileName = "mapimage_${screenShotData.latitude}_${screenShotData.longitude}.jpg"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // ✅ Android 10+ (Scoped Storage, save inside `Pictures/Teamyar`)
            saveImageToMediaStore(
                context,
                croppedBitmap,
                fileName,
                "Pictures",
                "${screenShotData.rootFolder}/${screenShotData.childFolder}"
            )
        } else {
            // ✅ Android 9 and below (Direct File System)
            saveImageToFileSystem(
                context,
                croppedBitmap,
                fileName,
                screenShotData.rootFolder,
                screenShotData.childFolder
            )
        }
    }

    fun List<Pair<View, Any>>.actionEach(action: (Any) -> Unit) {
        forEach { (button, associatedAction) ->
            button.setOnClickListener { action(associatedAction) }
        }
    }


    inline fun SearchView.onQueryTextChanged(crossinline listener: (String) -> Unit) {
        this.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true

            override fun onQueryTextChange(newText: String?): Boolean {
                // If the text is null or empty, clear the search
                listener.invoke(newText.orEmpty())
                return true
            }
        })
    }

    fun TextView.setAppropriateTextAlignment(locale: String) {
        if (locale == "fa" || locale == "Persian" || locale == "Fa") {
            this.textDirection = View.TEXT_DIRECTION_RTL
        } else {
            this.textDirection = View.TEXT_DIRECTION_LTR
        }
    }

    fun SearchView.applyTextColor(colorRes: Int) {
        val color = ContextCompat.getColor(context, colorRes)
        val searchTextViewId = resources.getIdentifier("android:id/search_src_text", null, null)
        val searchTextView = findViewById<EditText>(searchTextViewId)
        searchTextView?.setTextColor(color)
        searchTextView?.setHintTextColor(color)
    }


    fun SearchView.applyStyling(textColor: Int, hintTextColor: Int, closeIconColor: Int) {
        maxWidth = Integer.MAX_VALUE
        val searchTextView = findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
        val searchHintIcon = findViewById<ImageView>(androidx.appcompat.R.id.search_button)
        val searchClose = findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)

        searchTextView.apply {
            setTextColor(textColor)
            setHintTextColor(hintTextColor)
        }
        searchClose.setColorFilter(closeIconColor)
        searchHintIcon.setColorFilter(closeIconColor)

    }

    fun TextView.highlightTextNew(textToHighlight: String, highlightColor: Int, defaultColor: Int) {
        // Get the full text from the TextView
        val fullText = this.text.toString()

        // If the search keyword is empty, reset to default color
        if (textToHighlight.isEmpty()) {
            this.setTextColor(defaultColor) // Reset text color
            this.text = fullText
            return
        }

        // Find the index of the text to highlight
        val startIndex = fullText.indexOf(textToHighlight, ignoreCase = true)

        // Apply the highlight only if the textToHighlight is found and not empty
        if (startIndex != -1) {
            val endIndex = startIndex + textToHighlight.length
            val spannableString = SpannableString(fullText)
            val colorSpan = ForegroundColorSpan(highlightColor)
            spannableString.setSpan(
                colorSpan,
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            this.text = spannableString
        } else {
            // If text is not found, reset text color to default
            this.setTextColor(defaultColor)
            this.text = fullText
        }
    }

    fun TextView.setAbbreviatedText(
        context: Context,
        text: String,
        maxSizeTablet: Int = 60,
        maxSizePhone: Int = 30
    ) {
        val metrics = context.resources.displayMetrics
        val maxLength =
            if (sqrt(
                    (metrics.widthPixels / metrics.xdpi.toDouble()).pow(2.0)
                            + (metrics.heightPixels / metrics.ydpi.toDouble()).pow(2.0)
                ) > 7
            )
                maxSizeTablet else maxSizePhone
        this.text = if (text.length > maxLength) {
            "${text.substring(0, maxLength)}..."
        } else {
            text
        }
    }


    fun ImageView.loadUnsafeImage(
        path: String,
        baseUrl: String,
        sid: String,
        version: String,
        isMail: Boolean,
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post { loadUnsafeImage(path, baseUrl, sid, version, isMail) }
            return
        }

        val context = this.context.applicationContext ?: return
        try {
            val okHttpClient = provideUnsafeImageClient(
                address = "https://${baseUrl}",
                sid = sid,
                version = version
            )

            val imageLoader = ImageLoader.Builder(this.context)
                .callFactory { okHttpClient }
                .build()

            val request = ImageRequest.Builder(context)
                .data(path)
                .crossfade(true)
                .scale(Scale.FILL)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .apply {
                    placeholder(
                        ContextCompat.getDrawable(
                            context,
                            if (isMail) R.drawable.no_user_male else R.drawable.no_user_female
                        )
                    )
                    error(
                        ContextCompat.getDrawable(
                            context,
                            if (isMail) R.drawable.no_user_male else R.drawable.no_user_female
                        )
                    )
                }
                .transformations(CircleCropTransformation())
                .target(this)
                .build()

            imageLoader.enqueue(request)
        } catch (e: Exception) {
            Log.e("CoilError", "Failed to load image", e)
        }

    }


    fun Chip.loadUnsafeChipIcon(
        imgPath: String,
        baseUrl: String,
        sid: String,
        version: String,
        isMail: Boolean
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post { loadUnsafeChipIcon(imgPath, baseUrl, sid, version, isMail) }
            return
        }

        val context = this.context.applicationContext ?: return

        try {
            val okHttpClient = provideUnsafeImageClient(
                address = "https://$baseUrl",
                sid = sid,
                version = version
            )

            val imageLoader = ImageLoader.Builder(context)
                .callFactory {
                    okHttpClient
                }
                .build()

            val request = ImageRequest.Builder(context)
                .data(imgPath)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .transformations(CircleCropTransformation())
                .target(onSuccess = { drawable: Drawable ->
                    chipIcon = drawable

                }, onStart = {
                    chipIcon = ContextCompat.getDrawable(
                        context,
                        if (isMail) R.drawable.no_user_male else R.drawable.no_user_female
                    )
                }, onError = {
                    chipIcon = ContextCompat.getDrawable(
                        context,
                        if (isMail) R.drawable.no_user_male else R.drawable.no_user_female
                    )
                }).build()
            imageLoader.enqueue(request)
        } catch (e: Exception) {
            Log.e("CoilError", "Failed to load chip icon", e)
        }
    }

    fun ImageView.showImageWithCoil(
        path: String,
        sharedPrefValue: String
    ) {
        val headers = Headers.Builder()
            .add("Cookie", sharedPrefValue)
            .build()

        val request = ImageRequest.Builder(context)
            .data(path)
            .headers(headers)
            .crossfade(true)
            .scale(Scale.FILL)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .placeholder(R.drawable.ic_video_placeholder)
            .error(R.drawable.ic_video_error)
            .target(this)
            .build()

        val imageLoader = ImageLoader(context)
        imageLoader.enqueue(request)
    }

    fun PlayerView.loadThumbnailIntoPlayerView(videoUri: Uri) {
        // Initialize ExoPlayer
        val player = ExoPlayer.Builder(this.context).build()

        // Set the player to the PlayerView
        this.player = player

        // Create a MediaItem with the video Uri
        val mediaItem = MediaItem.fromUri(videoUri)

        // Set the MediaItem to the player
        player.setMediaItem(mediaItem)

        // Prepare the player but don't start playing
        player.prepare()

        // Optionally, you can seek to a specific position (e.g., 1 second) to display a different thumbnail
        player.seekTo(0) // Seek to the start (or any other time)

        // Release the player when it's no longer needed (like when the view is detached or recycled)
        this.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                player.release()
            }
        })
    }

    fun View.setVisibleOrGone(dataModify: String?) {
        Log.e("TAG", "setVisibleOrGone: $dataModify")
        this.visibility = if (dataModify.equals("0")) View.GONE else View.VISIBLE
    }

    fun FontIcon.makeMovable() {
        var dX = 0f
        var dY = 0f
        var startX = 0f
        var startY = 0f
        val clickThreshold = 10 // Threshold to distinguish click from move

        this.post {
            // Store the initial position relative to the parent view
            val parent = this.parent as? View ?: return@post
            var parentWidth = parent.width
            var parentHeight = parent.height

            // Add a listener to detect layout changes (e.g. when keyboard appears)
            parent.viewTreeObserver.addOnGlobalLayoutListener {
                // Update parent dimensions in case they change
                parentWidth = parent.width
                parentHeight = parent.height
            }

            val initialX = this.x
            val initialY = this.y
            val snapThreshold = 150 // Threshold to snap back in pixels

            this.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = view.x - event.rawX
                        dY = view.y - event.rawY
                        startX = event.rawX
                        startY = event.rawY
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // Calculate new position
                        var newX = event.rawX + dX
                        var newY = event.rawY + dY

                        // Get view's width and height
                        val viewWidth = view.width
                        val viewHeight = view.height

                        // Prevent the button from going out of parent's bounds
                        newX = newX.coerceIn(0f, (parentWidth - viewWidth).toFloat())
                        newY = newY.coerceIn(0f, (parentHeight - viewHeight).toFloat())

                        view.animate()
                            .x(newX)
                            .y(newY)
                            .setDuration(0)
                            .start()

                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        // Calculate the total movement distance
                        val deltaX = Math.abs(event.rawX - startX)
                        val deltaY = Math.abs(event.rawY - startY)

                        // If the movement is smaller than the click threshold, treat it as a click
                        if (deltaX < clickThreshold && deltaY < clickThreshold) {
                            performClick() // Trigger the click event
                        } else {
                            // Calculate distance to initial position
                            val distanceX = Math.abs(view.x - initialX)
                            val distanceY = Math.abs(view.y - initialY)

                            // If the button is close to the initial position, snap back
                            if (distanceX < snapThreshold && distanceY < snapThreshold) {
                                view.animate()
                                    .x(initialX)
                                    .y(initialY)
                                    .setDuration(300)
                                    .start()
                            }
                        }

                        true
                    }

                    else -> false
                }
            }
        }
    }

    fun showToast(context: Context, msgRes: Int) {
        val typeface = Typeface.createFromAsset(context.assets, "IRANSansMobile(NoEn)_Light.ttf")
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast, null)
        val text = layout.findViewById<TextView>(R.id.txt_message)
        text.typeface = typeface
        text.text = context.getString(msgRes)

        val toast = Toast(context)
        toast.setGravity(Gravity.BOTTOM, 0, 100)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }

    fun showToast(context: Context, msgRes: String) {
        val typeface = Typeface.createFromAsset(context.assets, "IRANSansMobile(NoEn)_Light.ttf")
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast, null)
        val text = layout.findViewById<TextView>(R.id.txt_message)
        text.setTypeface(typeface)
        text.text = msgRes

        val toast = Toast(context)
        toast.setGravity(Gravity.BOTTOM, 0, 100)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }

    fun FontIcon.showAgain() {
        // Reset visibility
        visibility = View.VISIBLE

        // Animation to show the button
        animate()
            .alpha(1f)
            .setDuration(800)
            .start()
    }
}