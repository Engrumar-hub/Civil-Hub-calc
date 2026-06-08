package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculation_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val calcType: String, // "CONCRETE", "REBAR", "AGGREGATE"
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val inputs: String,
    val outputs: String,
    val unitSystem: String // "METRIC" or "IMPERIAL"
)
