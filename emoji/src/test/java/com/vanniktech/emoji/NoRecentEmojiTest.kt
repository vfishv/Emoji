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

import com.vanniktech.emoji.emoji.Emoji
import org.junit.Assert.assertEquals
import org.junit.Test

class NoRecentEmojiTest {
  @Test fun alwaysEmpty() {
    assertEquals(emptyList<Emoji>(), NoRecentEmoji.getRecentEmojis())
    NoRecentEmoji.addEmoji(Emoji(0x1f55a, arrayOf("test"), R.drawable.emoji_recent, false))
    NoRecentEmoji.persist()
    assertEquals(emptyList<Emoji>(), NoRecentEmoji.getRecentEmojis())
  }
}
