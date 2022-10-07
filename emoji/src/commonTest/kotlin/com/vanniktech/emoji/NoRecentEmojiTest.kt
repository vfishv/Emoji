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

import com.vanniktech.emoji.recent.NoRecentEmoji
import kotlin.test.Test
import kotlin.test.assertEquals

class NoRecentEmojiTest {
  @Test fun alwaysEmpty() {
    assertEquals(expected = emptyList(), actual = NoRecentEmoji.getRecentEmojis())
    NoRecentEmoji.addEmoji(TestEmoji(intArrayOf(0x1f55a), listOf("test"), false))
    NoRecentEmoji.persist()
    assertEquals(expected = emptyList(), actual = NoRecentEmoji.getRecentEmojis())
  }
}
