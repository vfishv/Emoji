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

package com.vanniktech.emoji.recent

import android.content.Context
import com.vanniktech.emoji.Emoji
import com.vanniktech.emoji.EmojiManager

class RecentEmojiManager @JvmOverloads constructor(
  context: Context,
  /** Maximum number of recent Emojis we'll keep. */
  private val maxRecents: Int = 40,
) : RecentEmoji {
  private var emojiList = EmojiList(mutableListOf(), maxRecents)
  private val sharedPreferences = context.applicationContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

  override fun getRecentEmojis(): Collection<Emoji> {
    if (emojiList.size() == 0) {
      val savedRecentEmojis = sharedPreferences.getString(RECENT_EMOJIS, "").orEmpty()

      if (savedRecentEmojis.isNotEmpty()) {
        val split = savedRecentEmojis.split(EMOJI_DELIMITER)
        emojiList = EmojiList(
          split.mapNotNull {
            val parts = it.split(TIME_DELIMITER).toTypedArray()
            if (parts.size == 2) {
              val candidate = parts[0]
              val emoji = EmojiManager.findEmoji(candidate)
              if (emoji != null) {
                val timestamp = parts[1].toLong()
                RecentEmojiData(emoji, timestamp)
              } else {
                null
              }
            } else {
              null
            }
          }.sortedByDescending { it.timestamp }.toMutableList(),
          maxRecents,
        )
      }
    }

    return emojiList.getEmojis()
  }

  override fun addEmoji(emoji: Emoji) {
    emojiList.add(emoji)
  }

  override fun persist() {
    if (emojiList.size() > 0) {
      val stringBuilder = StringBuilder(emojiList.size() * EMOJI_GUESS_SIZE)
      for (i in 0 until emojiList.size()) {
        val data = emojiList[i]
        stringBuilder.append(data.emoji.unicode)
          .append(TIME_DELIMITER)
          .append(data.timestamp)
          .append(EMOJI_DELIMITER)
      }
      stringBuilder.setLength(stringBuilder.length - EMOJI_DELIMITER.length)
      sharedPreferences.edit().putString(RECENT_EMOJIS, stringBuilder.toString()).apply()
    }
  }

  internal class EmojiList(
    private var emojis: MutableList<RecentEmojiData>,
    private val maxRecents: Int,
  ) {
    fun add(emoji: Emoji, timestamp: Long = System.currentTimeMillis()) {
      val iterator = emojis.iterator()
      val emojiBase = emoji.base

      while (iterator.hasNext()) {
        val data = iterator.next()
        if (data.emoji.base == emojiBase) { // Do the comparison by base so that skin tones are only saved once.
          iterator.remove()
        }
      }

      emojis.add(0, RecentEmojiData(emoji, timestamp))

      if (emojis.size > maxRecents) {
        emojis.removeAt(maxRecents)
      }
    }

    fun getEmojis() = emojis.sortedByDescending { it.timestamp }.map { it.emoji }

    fun size() = emojis.size

    operator fun get(index: Int) = emojis[index]
  }

  internal data class RecentEmojiData(val emoji: Emoji, val timestamp: Long)

  internal companion object {
    private const val PREFERENCE_NAME = "emoji-recent-manager"
    private const val TIME_DELIMITER = ";"
    private const val EMOJI_DELIMITER = "~"
    private const val RECENT_EMOJIS = "recent-emojis"
    internal const val EMOJI_GUESS_SIZE = 5
  }
}
