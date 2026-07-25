package com.example.widgetfatsecret

import com.example.widgetfatsecret.fatsecret.data.NutritionSnapshot
import com.example.widgetfatsecret.fatsecret.data.SyncStatus
import com.example.widgetfatsecret.fatsecret.domain.DailyNutrition
import com.example.widgetfatsecret.ui.ContentState
import com.example.widgetfatsecret.ui.toChipStatus
import com.example.widgetfatsecret.ui.toContentState
import org.junit.Assert.assertEquals
import org.junit.Test
import com.example.widgetfatsecret.ui.design.SyncStatus as UiSyncStatus

class SyncPresentationTest {

    @Test
    fun `loading without cache uses skeleton state`() {
        val snapshot = NutritionSnapshot(
            connected = true,
            syncStatus = SyncStatus.LOADING,
        )

        assertEquals(ContentState.LOADING, snapshot.toContentState())
        assertEquals(UiSyncStatus.SYNCING, snapshot.toChipStatus())
    }

    @Test
    fun `sync failure keeps cached content and uses offline chip`() {
        val snapshot = NutritionSnapshot(
            daily = DailyNutrition(calories = 1200.0, entryCount = 3),
            connected = true,
            syncStatus = SyncStatus.ERROR,
            hasValidData = true,
        )

        assertEquals(ContentState.CONTENT, snapshot.toContentState())
        assertEquals(UiSyncStatus.OFFLINE, snapshot.toChipStatus())
    }

    @Test
    fun `successful synchronized day without entries is not zero consumption`() {
        val snapshot = NutritionSnapshot(
            connected = true,
            syncStatus = SyncStatus.SUCCESS,
            hasValidData = true,
        )

        assertEquals(ContentState.EMPTY, snapshot.toContentState())
        assertEquals(UiSyncStatus.SYNCED, snapshot.toChipStatus())
    }

    @Test
    fun `connected cache that was never populated remains not synchronized`() {
        val snapshot = NutritionSnapshot(
            connected = true,
            syncStatus = SyncStatus.IDLE,
        )

        assertEquals(ContentState.NOT_SYNCED, snapshot.toContentState())
    }

    @Test
    fun `failure without cache uses explicit error state`() {
        val snapshot = NutritionSnapshot(
            connected = true,
            syncStatus = SyncStatus.ERROR,
        )

        assertEquals(ContentState.SYNC_ERROR, snapshot.toContentState())
        assertEquals(UiSyncStatus.ERROR, snapshot.toChipStatus())
    }
}
