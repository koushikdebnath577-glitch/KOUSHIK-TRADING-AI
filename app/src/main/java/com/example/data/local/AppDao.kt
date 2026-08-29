package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM watchlist ORDER BY addedTimestamp DESC")
    fun getAllWatchlist(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(item: WatchlistEntity)

    @Delete
    suspend fun deleteWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun deleteWatchlistBySymbol(symbol: String)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE symbol = :symbol)")
    fun isInWatchlist(symbol: String): Flow<Boolean>

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Query("UPDATE alerts SET isRead = 1 WHERE id = :alertId")
    suspend fun markAlertAsRead(alertId: String)

    @Query("UPDATE alerts SET isEnabled = :enabled WHERE id = :alertId")
    suspend fun setAlertEnabled(alertId: String, enabled: Boolean)

    @Query("DELETE FROM alerts WHERE id = :alertId")
    suspend fun deleteAlert(alertId: String)

    @Query("DELETE FROM alerts")
    suspend fun clearAllAlerts()

    @Query("SELECT * FROM saved_plans ORDER BY timestamp DESC")
    fun getAllSavedPlans(): Flow<List<SavedPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPlan(plan: SavedPlanEntity)

    @Query("DELETE FROM saved_plans WHERE id = :planId")
    suspend fun deleteSavedPlan(planId: String)
}
