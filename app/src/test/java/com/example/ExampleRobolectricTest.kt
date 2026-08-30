package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
        assertEquals("KOUSHIK TRADING AI", appName)
    }

    @Test
    fun `test risk calculation with editable risk amount`() {
        val entry = 2500.0
        val stopLoss = 2480.0
        val riskPerShare = kotlin.math.abs(entry - stopLoss)
        val customRiskAmount = 5000.0

        val quantity = (customRiskAmount / riskPerShare).toInt().coerceAtLeast(1)
        assertEquals(250, quantity)
        val totalRisk = quantity * riskPerShare
        assertTrue(totalRisk <= customRiskAmount)
    }
}
