package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val token: String,
    val exchange: String = "NSE",
    val addedTimestamp: Long = System.currentTimeMillis()
) {
    val companyName: String get() = name
}

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val symbol: String,
    val title: String,
    val message: String,
    val triggerPrice: Double,
    val isRead: Boolean = false,
    val isEnabled: Boolean = true
)

@Entity(tableName = "saved_plans")
data class SavedPlanEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val symbol: String,
    val strategyName: String,
    val direction: String,
    val score: Int,
    val entryPrice: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val riskReward: Double,
    val notes: String
)
