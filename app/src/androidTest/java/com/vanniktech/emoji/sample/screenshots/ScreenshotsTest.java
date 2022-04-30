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

package com.vanniktech.emoji.sample.screenshots;

import android.content.Context;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.vanniktech.emoji.sample.MainActivity;
import com.vanniktech.emoji.sample.R;
import java.io.File;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import tools.fastlane.screengrab.FileWritingScreenshotCallback;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.vanniktech.emoji.sample.screenshots.AppendTextAction.append;
import static java.util.Locale.US;

@RunWith(AndroidJUnit4.class) public final class ScreenshotsTest {
  @ClassRule public static final LocaleTestRule LOCALE_TEST_RULE = new LocaleTestRule();

  @Rule public final ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

  @Before public void setUp() {
    Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
  }

  @Test public void takeScreenShotsIos() throws InterruptedException {
    start(Variant.IOS);
  }

  @Test public void takeScreenShotsEmojiGoogle() throws InterruptedException {
    start(Variant.GOOGLE);
  }

  @Test public void takeScreenShotsEmojiTwitter() throws InterruptedException {
    start(Variant.TWITTER);
  }

  @Test public void takeScreenShotsEmojiFacebook() throws InterruptedException {
    start(Variant.FACEBOOK);
  }

  private static void start(final Variant variant) throws InterruptedException {
    final String name = variant.name().toLowerCase(US);

    // Select the right variant.
    onView(withId(R.id.menuMainSearchOption)).perform(click());
    onView(withText(variant.title)).perform(click());

    // First text.
    onView(withId(R.id.main_activity_emoji)).perform(click());

    final int[] firstEmojis = new int[] { 0x1f913, 0x1F60E, 0x1F921, 0x1F920, 0x1F60F, 0x1F3BE };
    onView(withId(R.id.main_activity_chat_bottom_message_edittext)).perform(append("Hello what's up? " + new String(firstEmojis, 0, firstEmojis.length)));

    Thread.sleep(500); // Espresso does not synchronize it right away.
    Screengrab.screenshot(name + "_1", Screengrab.getDefaultScreenshotStrategy(), new ConstantFileWritingScreenshotCallback(InstrumentationRegistry.getInstrumentation().getTargetContext()));

    onView(withId(R.id.main_activity_send)).perform(click());

    // Second text.
    final int[] beerEmojis = new int[] { 0x1F37A, 0x1F37A, 0x1F37A };
    onView(withId(R.id.main_activity_chat_bottom_message_edittext)).perform(append(new String(beerEmojis, 0, beerEmojis.length)));
    onView(withId(R.id.main_activity_send)).perform(click());

    final int[] clinkingBeerEmoji = new int[] { 0x1F37B };
    onView(withId(R.id.main_activity_chat_bottom_message_edittext)).perform(append(new String(clinkingBeerEmoji, 0, clinkingBeerEmoji.length)));
    onView(withId(R.id.main_activity_send)).perform(click());

    Thread.sleep(500); // Espresso does not synchronize it right away.
    Screengrab.screenshot(name + "_2", Screengrab.getDefaultScreenshotStrategy(), new ConstantFileWritingScreenshotCallback(InstrumentationRegistry.getInstrumentation().getTargetContext()));

    // Third text.
    onView(withId(R.id.main_activity_send)).perform(click());

    final int[] secondEmojis = new int[] { 0x1F98B, 0x1F41E, 0x1F41D, 0x1F422, 0x1F432, 0x1F683, 0x1F37B, 0x1F943 };
    onView(withId(R.id.main_activity_chat_bottom_message_edittext)).perform(append("I don't know " + new String(secondEmojis, 0, secondEmojis.length)));

    Thread.sleep(500); // Espresso does not synchronize it right away.
    Screengrab.screenshot(name + "_3", Screengrab.getDefaultScreenshotStrategy(), new ConstantFileWritingScreenshotCallback(InstrumentationRegistry.getInstrumentation().getTargetContext()));
  }

  static class ConstantFileWritingScreenshotCallback extends FileWritingScreenshotCallback {
    ConstantFileWritingScreenshotCallback(final Context appContext) {
      super(appContext);
    }

    @Override protected File getScreenshotFile(final File screenshotDirectory, final String screenshotName) {
      return new File(screenshotDirectory, screenshotName + ".png");
    }
  }

  enum Variant {
    GOOGLE("Google"),
    IOS("iOS"),
    TWITTER("Twitter"),
    FACEBOOK("Facebook");

    final String title;

    Variant(final String title) {
      this.title = title;
    }
  }
}
