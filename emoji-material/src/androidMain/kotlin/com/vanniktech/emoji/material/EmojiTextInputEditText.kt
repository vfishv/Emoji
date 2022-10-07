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
 */

package com.vanniktech.emoji.material

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.annotation.CallSuper
import androidx.annotation.DimenRes
import androidx.annotation.Px
import com.google.android.material.textfield.TextInputEditText
import com.vanniktech.emoji.EmojiDisplayable
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.init
import com.vanniktech.emoji.replaceWithImages

open class EmojiTextInputEditText @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  @AttrRes defStyleAttr: Int = com.google.android.material.R.attr.editTextStyle,
) : TextInputEditText(context, attrs, defStyleAttr), EmojiDisplayable {
  @Px private var emojiSize: Float

  init {
    emojiSize = init(attrs, R.styleable.EmojiTextInputEditText, R.styleable.EmojiTextInputEditText_emojiSize)
  }

  @CallSuper override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
    if (isInEditMode) {
      return
    }
    val fontMetrics = paint.fontMetrics
    val defaultEmojiSize = fontMetrics.descent - fontMetrics.ascent
    EmojiManager.replaceWithImages(context, getText(), if (emojiSize != 0f) emojiSize else defaultEmojiSize)
  }

  override fun getEmojiSize() = emojiSize

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
