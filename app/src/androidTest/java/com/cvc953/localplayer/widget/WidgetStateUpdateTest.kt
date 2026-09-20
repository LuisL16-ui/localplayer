package com.cvc953.localplayer.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetStateUpdateTest {

    @Test
    fun `WidgetStateUpdateTest verifies AppWidgetManager is accessible`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = AppWidgetManager.getInstance(context)
        assertNotNull(manager)
    }
}