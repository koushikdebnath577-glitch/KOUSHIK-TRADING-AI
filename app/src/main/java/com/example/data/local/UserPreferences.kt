package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "koushik_trading_ai_user_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_INTRADAY_RISK = "key_intraday_risk_amount"
        private const val KEY_TARGET_RR = "key_target_rr"
        const val DEFAULT_RISK_AMOUNT = 2500.0
        const val DEFAULT_TARGET_RR = 2.0
    }

    /**
     * Gets the saved Intraday Risk per trade.
     * Defaults to ₹2500.0 for first-time users.
     */
    fun getIntradayRisk(): Double {
        val str = prefs.getString(KEY_INTRADAY_RISK, null)
        val value = str?.toDoubleOrNull()
        return if (value != null && value > 0.0) value else DEFAULT_RISK_AMOUNT
    }

    /**
     * Saves the edited Intraday Risk per trade permanently to local storage.
     */
    fun saveIntradayRisk(amount: Double) {
        if (amount > 0.0) {
            prefs.edit().putString(KEY_INTRADAY_RISK, amount.toString()).apply()
        }
    }

    /**
     * Gets the saved Minimum Target Risk:Reward ratio. Defaults to 2.0.
     */
    fun getTargetRR(): Double {
        val str = prefs.getString(KEY_TARGET_RR, null)
        val value = str?.toDoubleOrNull()
        return if (value != null && value > 0.0) value else DEFAULT_TARGET_RR
    }

    /**
     * Saves the Target RR ratio permanently to local storage.
     */
    fun saveTargetRR(rr: Double) {
        if (rr > 0.0) {
            prefs.edit().putString(KEY_TARGET_RR, rr.toString()).apply()
        }
    }
}
