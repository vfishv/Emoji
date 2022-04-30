/*
 * Copyright (C) 2016 - Niklas Baudy, Ruben Gees, Mario Đanić and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.vanniktech.emoji

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import androidx.annotation.CallSuper
import androidx.annotation.DimenRes
import androidx.annotation.Px
import androidx.appcompat.widget.AppCompatButton

class EmojiButton @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
) : AppCompatButton(context, attrs), EmojiDisplayable {
  @Px private var emojiSize: Float

  init {
    emojiSize = Utils.initTextView(this, attrs, R.styleable.EmojiButton, R.styleable.EmojiButton_emojiSize)
  }

  @CallSuper override fun setText(rawText: CharSequence?, type: BufferType) {
    if (isInEditMode) {
      super.setText(rawText, type)
      return
    }
    val spannableStringBuilder = SpannableStringBuilder(rawText ?: "")
    val fontMetrics = paint.fontMetrics
    val defaultEmojiSize = fontMetrics.descent - fontMetrics.ascent
    EmojiManager.replaceWithImages(context, spannableStringBuilder, if (emojiSize != 0f) emojiSize else defaultEmojiSize)
    super.setText(spannableStringBuilder, type)
  }

  override fun getEmojiSize(): Float = emojiSize

  override fun setEmojiSize(@Px pixels: Int) = setEmojiSize(pixels, true)

  override fun setEmojiSize(@Px pixels: Int, shouldInvalidate: Boolean) {
    emojiSize = pixels.toFloat()
    if (shouldInvalidate) {
      text = text
    }
  }

  override fun setEmojiSizeRes(@DimenRes res: Int) = setEmojiSizeRes(res, true)

  override fun setEmojiSizeRes(@DimenRes res: Int, shouldInvalidate: Boolean) =
    setEmojiSize(resources.getDimensionPixelSize(res), shouldInvalidate)
}
