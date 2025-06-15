/**
 * Created by ST on 5/3/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.views.customview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.sina.library.R

class TypingDotsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val dot1: AppCompatTextView
    private val dot2: AppCompatTextView
    private val dot3: AppCompatTextView

    init {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.view_typing_dots, this, true)
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)
        startAnimation()
    }

    private fun startAnimation() {
        dot1.postDelayed({ dot1.isVisible = true }, 0)
        dot2.postDelayed({ dot2.isVisible = true }, 300)
        dot3.postDelayed({ dot3.isVisible = true }, 600)

        postDelayed({
            dot1.isVisible = false
            dot2.isVisible = false
            dot3.isVisible = false
            startAnimation() // repeat
        }, 1200)
    }
}
