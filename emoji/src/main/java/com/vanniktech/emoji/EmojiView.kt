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
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.vanniktech.emoji.emoji.Emoji
import com.vanniktech.emoji.listeners.OnEmojiBackspaceClickListener
import com.vanniktech.emoji.listeners.OnEmojiClickListener
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class EmojiView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
  private var emojiTabs = arrayOfNulls<ImageButton>(0)
  private lateinit var theming: EmojiTheming
  private lateinit var emojiPagerAdapter: EmojiPagerAdapter
  private var editText: EditText? = null
  private var onEmojiClickListener: OnEmojiClickListener? = null
  private var onEmojiBackspaceClickListener: OnEmojiBackspaceClickListener? = null
  private var emojiTabLastSelectedIndex = -1
  private lateinit var variantPopup: EmojiVariantPopup
  private lateinit var recentEmoji: RecentEmoji
  private lateinit var searchEmoji: SearchEmoji
  private lateinit var variantEmoji: VariantEmoji

  init {
    inflate(context, R.layout.emoji_view, this)
    orientation = VERTICAL
  }

  /**
   * Call this method to set up the EmojiView.
   * Once you're done with it, please call [.tearDown].
   */
  fun setUp(
    rootView: View,
    onEmojiClickListener: OnEmojiClickListener?,
    onEmojiBackspaceClickListener: OnEmojiBackspaceClickListener?,
    editText: EditText?,
    theming: EmojiTheming = EmojiTheming(),
    recentEmoji: RecentEmoji = RecentEmojiManager(rootView.context),
    searchEmoji: SearchEmoji = SearchEmojiManager(),
    variantEmoji: VariantEmoji = VariantEmojiManager(rootView.context),
    pageTransformer: ViewPager.PageTransformer? = null,
  ) {
    val context = context
    this.editText = editText
    this.theming = theming
    this.recentEmoji = recentEmoji
    this.searchEmoji = searchEmoji
    this.variantEmoji = variantEmoji
    this.onEmojiBackspaceClickListener = onEmojiBackspaceClickListener
    this.onEmojiClickListener = onEmojiClickListener
    variantPopup = EmojiVariantPopup(rootView) { emojiImageView: EmojiImageView, emoji: Emoji ->
      handleEmojiClick(emoji)
      emojiImageView.updateEmoji(emoji) // To reflect new variant in the UI.
      dismissVariantPopup()
    }
    setBackgroundColor(theming.backgroundColor(context))
    val emojisPager: ViewPager = findViewById(R.id.emojiViewPager)
    val emojiDivider = findViewById<View>(R.id.emojiViewDivider)
    emojiDivider.setBackgroundColor(theming.dividerColor(context))
    if (pageTransformer != null) {
      emojisPager.setPageTransformer(true, pageTransformer)
    }
    emojisPager.addOnPageChangeListener(object : OnPageChangeListener {
      override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) = Unit
      override fun onPageScrollStateChanged(position: Int) = Unit
      override fun onPageSelected(position: Int) = selectPage(position)
    })
    handleEmojiTabs(context, emojisPager)
    emojisPager.adapter = emojiPagerAdapter
    val startIndex = if (emojiPagerAdapter.hasRecentEmoji()) if (emojiPagerAdapter.numberOfRecentEmojis() > 0) 0 else 1 else 0
    emojisPager.currentItem = startIndex
    selectPage(startIndex)
  }

  internal fun selectPage(index: Int) {
    val context = context
    if (emojiTabLastSelectedIndex != index) {
      if (index == 0) {
        emojiPagerAdapter.invalidateRecentEmojis()
      }
      if (emojiTabLastSelectedIndex >= 0 && emojiTabLastSelectedIndex < emojiTabs.size) {
        emojiTabs[emojiTabLastSelectedIndex]!!.isSelected = false
        emojiTabs[emojiTabLastSelectedIndex]!!.setColorFilter(theming.primaryColor(context), PorterDuff.Mode.SRC_IN)
      }
      emojiTabs[index]?.isSelected = true
      emojiTabs[index]?.setColorFilter(theming.secondaryColor(context), PorterDuff.Mode.SRC_IN)
      emojiTabLastSelectedIndex = index
    }
  }

  private fun handleEmojiTabs(
    context: Context,
    emojisPager: ViewPager
  ) {
    val categories = EmojiManager.categories()
    val emojisTab = findViewById<LinearLayout>(R.id.emojiViewTab)
    emojiPagerAdapter = EmojiPagerAdapter(
      object : EmojiPagerDelegate {
        override fun onEmojiClick(emoji: Emoji) = handleEmojiClick(emoji)

        override fun onEmojiLongClick(view: EmojiImageView, emoji: Emoji) {
          variantPopup.show(view, emoji)
        }
      },
      recentEmoji, variantEmoji, theming
    )
    val hasBackspace = editText != null || onEmojiBackspaceClickListener != null
    val hasSearch = searchEmoji !is NoSearchEmoji

    val endIndexes = (if (hasSearch) 1 else 0) + if (hasBackspace) 1 else 0
    val recentAdapterItemCount = emojiPagerAdapter.recentAdapterItemCount()
    emojiTabs = arrayOfNulls(recentAdapterItemCount + categories.size + endIndexes)
    if (emojiPagerAdapter.hasRecentEmoji()) {
      emojiTabs[0] = inflateButton(context, R.drawable.emoji_recent, R.string.emoji_category_recent, emojisTab)
    }
    val searchIndex = if (hasSearch) emojiTabs.size - (if (hasBackspace) 2 else 1) else null
    val backspaceIndex = if (hasBackspace) emojiTabs.size - 1 else null
    for (i in categories.indices) {
      emojiTabs[i + recentAdapterItemCount] = inflateButton(context, categories[i].icon, categories[i].categoryName, emojisTab)
    }
    if (searchIndex != null) {
      emojiTabs[searchIndex] = inflateButton(context, R.drawable.emoji_search, R.string.emoji_search, emojisTab)
      emojiTabs[searchIndex]!!.setOnClickListener {
        EmojiSearchDialog.show(
          getContext(),
          {
            handleEmojiClick(it)

            // Maybe the search was opened from the recent tab and hence we'll invalidate.
            emojiPagerAdapter.invalidateRecentEmojis()
          },
          searchEmoji,
          theming
        )
      }
    }
    if (backspaceIndex != null) {
      emojiTabs[backspaceIndex] = inflateButton(context, R.drawable.emoji_backspace, R.string.emoji_backspace, emojisTab)
      emojiTabs[backspaceIndex]?.setOnTouchListener(
        RepeatListener(INITIAL_INTERVAL, NORMAL_INTERVAL.toLong()) {
          editText?.let(Utils::backspace)
          onEmojiBackspaceClickListener?.onEmojiBackspaceClick()
        }
      )
    }
    for (i in 0 until emojiTabs.size - endIndexes) {
      emojiTabs[i]?.setOnClickListener(EmojiTabsClickListener(emojisPager, i))
    }
  }

  private fun inflateButton(
    context: Context,
    @DrawableRes icon: Int,
    @StringRes categoryName: Int,
    parent: ViewGroup
  ): ImageButton {
    val button = LayoutInflater.from(context).inflate(R.layout.emoji_view_category, parent, false) as ImageButton
    button.setImageDrawable(AppCompatResources.getDrawable(context, icon))
    button.setColorFilter(theming.primaryColor(context), PorterDuff.Mode.SRC_IN)
    button.contentDescription = context.getString(categoryName)
    parent.addView(button)
    return button
  }

  private fun handleEmojiClick(emoji: Emoji) {
    editText?.let { Utils.input(it, emoji) }
    recentEmoji.addEmoji(emoji)
    variantEmoji.addVariant(emoji)
    onEmojiClickListener?.onEmojiClick(emoji)
  }

  /**
   * Counterpart of [setUp]
   */
  fun tearDown() {
    dismissVariantPopup()
    Executors.newSingleThreadExecutor().submit {
      recentEmoji.persist()
      variantEmoji.persist()
    }
  }

  private fun dismissVariantPopup() = variantPopup.dismiss()

  internal class EmojiTabsClickListener(
    private val emojisPager: ViewPager,
    private val position: Int,
  ) : OnClickListener {
    override fun onClick(v: View) {
      emojisPager.currentItem = position
    }
  }

  internal companion object {
    private val INITIAL_INTERVAL = TimeUnit.SECONDS.toMillis(1) / 2
    private const val NORMAL_INTERVAL = 50
  }
}
