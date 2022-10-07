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

package com.vanniktech.emoji

/**
 * EmojiManager where an EmojiProvider can be installed for further usage.
 */
object EmojiManager {
  private const val GUESSED_UNICODE_AMOUNT = 3000
  private const val GUESSED_TOTAL_PATTERN_LENGTH = GUESSED_UNICODE_AMOUNT * 4

  private val emojiMap: MutableMap<String, Emoji> = LinkedHashMap(GUESSED_UNICODE_AMOUNT)
  private var emojiProvider: EmojiProvider? = null
  private var categories: Array<EmojiCategory>? = null
  private var emojiPattern: Regex? = null
  internal var emojiRepetitivePattern: Regex? = null

  internal fun categories(): Array<EmojiCategory> {
    verifyInstalled()
    return categories!!
  }

  internal fun emojiProvider(): EmojiProvider {
    verifyInstalled()
    return emojiProvider!!
  }

  internal fun findAllEmojis(text: CharSequence?): List<EmojiRange> {
    verifyInstalled()

    if (text != null && text.isNotEmpty()) {
      return emojiPattern?.findAll(text)
        ?.mapNotNull {
          val emoji = findEmoji(it.value)

          if (emoji != null) {
            EmojiRange(emoji, IntRange(it.range.first, it.range.last + 1))
          } else {
            null
          }
        }
        .orEmpty()
        .toList()
    }

    return emptyList()
  }

  internal fun findEmoji(candidate: CharSequence): Emoji? {
    verifyInstalled()

    // We need to call toString on the candidate, since the emojiMap may not find the requested entry otherwise, because the type is different.
    return emojiMap[candidate.toString()]
  }

  internal fun verifyInstalled() {
    checkNotNull(categories) {
      "Please install an EmojiProvider through the EmojiManager.install() method first."
    }
  }

  /**
   * Installs the given EmojiProvider.
   *
   * NOTE: That only one can be present at any time.
   *
   * [provider] the provider that should be installed.
   */
  @JvmStatic fun install(provider: EmojiProvider) {
    synchronized(EmojiManager::class.java) {
      categories = provider.categories
      emojiProvider = provider
      emojiMap.clear()
      val unicodesForPattern: MutableList<String> = ArrayList(GUESSED_UNICODE_AMOUNT)
      val categoriesSize = provider.categories.size

      for (i in 0 until categoriesSize) {
        val emojis = provider.categories[i].emojis
        val emojisSize = emojis.size
        for (j in 0 until emojisSize) {
          val emoji = emojis[j]
          val unicode = emoji.unicode
          val variants = emoji.variants
          emojiMap[unicode] = emoji
          unicodesForPattern.add(unicode)
          for (k in variants.indices) {
            val variant = variants[k]
            val variantUnicode = variant.unicode
            emojiMap[variantUnicode] = variant
            unicodesForPattern.add(variantUnicode)
          }
        }
      }
      require(unicodesForPattern.isNotEmpty()) { "Your EmojiProvider must at least have one category with at least one emoji." }

      // We need to sort the unicodes by length so the longest one gets matched first.
      unicodesForPattern.sortWith { first, second -> second.length.compareTo(first.length) }
      val patternBuilder = StringBuilder(GUESSED_TOTAL_PATTERN_LENGTH)
      val unicodesForPatternSize = unicodesForPattern.size
      for (i in 0 until unicodesForPatternSize) {
        patternBuilder.append(Regex.escape(unicodesForPattern[i])).append('|')
      }
      val regex = patternBuilder.deleteCharAt(patternBuilder.length - 1).toString()
      emojiPattern = Regex(regex, RegexOption.IGNORE_CASE)
      emojiRepetitivePattern = Regex("($regex)+", RegexOption.IGNORE_CASE)
    }
  }

  /**
   * Destroys the EmojiManager. This means that all internal data structures are released as well as
   * all data associated with installed [Emoji]s. For the existing [EmojiProvider]s this
   * means the memory-heavy emoji sheet.
   *
   * @see .destroy
   */
  @JvmStatic fun destroy() {
    synchronized(EmojiManager::class.java) {
      release()
      emojiMap.clear()
      emojiProvider = null
      categories = null
      emojiPattern = null
      emojiRepetitivePattern = null
    }
  }

  /**
   * Releases all data associated with installed [Emoji]s. For the existing [EmojiProvider]s this
   * means the memory-heavy emoji sheet.
   *
   * In contrast to [destroy], this does **not** destroy the internal
   * data structures and thus, you do not need to [install] again before using the EmojiManager.
   *
   * @see .destroy
   */
  @JvmStatic fun release() {
    synchronized(EmojiManager::class.java) {
      emojiProvider?.release()
    }
  }
}
