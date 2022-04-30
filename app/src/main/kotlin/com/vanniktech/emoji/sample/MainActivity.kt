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
package com.vanniktech.emoji.sample

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater.Factory2
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.provider.FontRequest
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.FontRequestEmojiCompatConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.EmojiPopup
import com.vanniktech.emoji.facebook.FacebookEmojiProvider
import com.vanniktech.emoji.google.GoogleEmojiProvider
import com.vanniktech.emoji.googlecompat.GoogleCompatEmojiProvider
import com.vanniktech.emoji.ios.IosEmojiProvider
import com.vanniktech.emoji.material.MaterialEmojiLayoutFactory
import com.vanniktech.emoji.sample.databinding.ActivityMainBinding
import com.vanniktech.emoji.traits.EmojiTrait
import com.vanniktech.emoji.twitter.TwitterEmojiProvider

// We don't care about duplicated code in the sample.
class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding
  private lateinit var chatAdapter: ChatAdapter
  private lateinit var emojiPopup: EmojiPopup
  private var emojiCompat: EmojiCompat? = null
  private var searchInPlaceEmojiTrait: EmojiTrait? = null
  private var disableKeyboardInputEmojiTrait: EmojiTrait? = null

  @SuppressLint("SetTextI18n")
  override fun onCreate(savedInstanceState: Bundle?) {
    layoutInflater.factory2 = MaterialEmojiLayoutFactory(delegate as Factory2)
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    chatAdapter = ChatAdapter()
    setUpShowcaseButtons()

    emojiPopup = EmojiPopup.Builder.fromRootView(binding.rootView)
      .setOnEmojiBackspaceClickListener { Log.d(TAG, "Clicked on Backspace") }
      .setOnEmojiClickListener { emoji -> Log.d(TAG, "Clicked on Emoji " + emoji.unicode) }
      .setOnEmojiPopupShownListener { binding.chatEmoji.setImageResource(R.drawable.ic_keyboard) }
      .setOnSoftKeyboardOpenListener { px -> Log.d(TAG, "Opened soft keyboard with height $px") }
      .setOnEmojiPopupDismissListener { binding.chatEmoji.setImageResource(R.drawable.emoji_ios_category_smileysandpeople) }
      .setOnSoftKeyboardCloseListener { Log.d(TAG, "Closed soft keyboard") }
      .setKeyboardAnimationStyle(R.style.emoji_fade_animation_style)
      .setPageTransformer(PageTransformer())
      // .setRecentEmoji(NoRecentEmoji.INSTANCE) // Uncomment this to hide recent emojis.
      .build(binding.chatEditText)

    binding.chatSend.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary), PorterDuff.Mode.SRC_IN)
    binding.chatEmoji.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary), PorterDuff.Mode.SRC_IN)
    binding.forceEmojisOnly.setOnCheckedChangeListener { _, isChecked: Boolean ->
      if (isChecked) {
        binding.chatEditText.clearFocus()
        binding.chatEmoji.visibility = View.GONE
        disableKeyboardInputEmojiTrait = binding.chatEditText.installDisableKeyboardInput(emojiPopup)
      } else {
        binding.chatEmoji.visibility = View.VISIBLE
        disableKeyboardInputEmojiTrait?.uninstall()
      }
    }
    binding.inPlaceEmojis.setOnCheckedChangeListener { _, isChecked: Boolean ->
      if (isChecked) {
        searchInPlaceEmojiTrait = binding.chatEditText.installSearchInPlace(emojiPopup)
      } else {
        searchInPlaceEmojiTrait?.uninstall()
      }
    }
    binding.chatEmoji.setOnClickListener { emojiPopup.toggle() }
    binding.chatSend.setOnClickListener {
      val text = binding.chatEditText.text.toString().trim { it <= ' ' }
      if (text.isNotEmpty()) {
        chatAdapter.add(text)
        binding.chatEditText.setText("")
      }
    }
    binding.recyclerView.adapter = chatAdapter
    binding.recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
  }

  @SuppressLint("SetTextI18n")
  private fun setUpShowcaseButtons() {
    binding.emojis.setOnClickListener {
      emojiPopup.dismiss()
      startActivity(Intent(this, EmojisActivity::class.java))
    }
    binding.customView.setOnClickListener {
      emojiPopup.dismiss()
      startActivity(Intent(this, CustomViewActivity::class.java))
    }
    binding.dialogButton.setOnClickListener {
      emojiPopup.dismiss()
      MainDialog.show(this)
    }
    binding.button.text = "Switch between Emoji Provider \uD83D\uDE18\uD83D\uDE02\uD83E\uDD8C"
    binding.button.setOnClickListener {
      val menu = PopupMenu(this, binding.button, Gravity.BOTTOM)
      menu.inflate(R.menu.menu_emoji_provider)
      menu.setOnMenuItemClickListener { menuItem: MenuItem ->
        when (menuItem.itemId) {
          R.id.menuEmojiProviderIos -> {
            EmojiManager.destroy()
            EmojiManager.install(IosEmojiProvider())
            recreate()
            return@setOnMenuItemClickListener true
          }
          R.id.menuEmojiProviderGoogle -> {
            EmojiManager.destroy()
            EmojiManager.install(GoogleEmojiProvider())
            recreate()
            return@setOnMenuItemClickListener true
          }
          R.id.menuEmojiProviderTwitter -> {
            EmojiManager.destroy()
            EmojiManager.install(TwitterEmojiProvider())
            recreate()
            return@setOnMenuItemClickListener true
          }
          R.id.menuEmojiProviderFacebook -> {
            EmojiManager.destroy()
            EmojiManager.install(FacebookEmojiProvider())
            recreate()
            return@setOnMenuItemClickListener true
          }
          R.id.menuEmojiProviderGoogleCompat -> {
            if (emojiCompat == null) {
              emojiCompat = EmojiCompat.init(
                FontRequestEmojiCompatConfig(
                  this,
                  FontRequest(
                    "com.google.android.gms.fonts",
                    "com.google.android.gms",
                    "Noto Color Emoji Compat",
                    R.array.com_google_android_gms_fonts_certs,
                  )
                ).setReplaceAll(true)
              )
            }
            EmojiManager.destroy()
            EmojiManager.install(GoogleCompatEmojiProvider(emojiCompat!!))
            recreate()
            return@setOnMenuItemClickListener true
          }
          else -> {
            return@setOnMenuItemClickListener false
          }
        }
      }
      menu.show()
    }
  }

  companion object {
    const val TAG = "MainActivity"
  }
}
