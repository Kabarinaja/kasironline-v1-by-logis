package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.printer.EscPosBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("KASIR DUNIA", appName)
    }

    @Test
    fun `test esc pos builder 58mm width`() {
        val builder = EscPosBuilder("58mm")
        assertEquals(32, builder.maxColumns)
    }

    @Test
    fun `test esc pos builder 80mm width`() {
        val builder = EscPosBuilder("80mm")
        assertEquals(48, builder.maxColumns)
    }
}
