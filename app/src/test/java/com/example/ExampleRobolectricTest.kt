package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Flappy Bird", appName)
  }

  @Test
  fun `verify medal calculations for score milestones`() {
    assertEquals(com.example.game.model.Medal.NONE, com.example.game.model.Medal.forScore(5))
    assertEquals(com.example.game.model.Medal.BRONZE, com.example.game.model.Medal.forScore(12))
    assertEquals(com.example.game.model.Medal.SILVER, com.example.game.model.Medal.forScore(25))
    assertEquals(com.example.game.model.Medal.GOLD, com.example.game.model.Medal.forScore(35))
    assertEquals(com.example.game.model.Medal.PLATINUM, com.example.game.model.Medal.forScore(50))
  }
}
