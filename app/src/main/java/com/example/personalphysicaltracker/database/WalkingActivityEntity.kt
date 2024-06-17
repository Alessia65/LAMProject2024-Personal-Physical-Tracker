package com.example.personalphysicaltracker.database

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "steps_table",
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["walkingActivityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("walkingActivityId")]
)
data class WalkingActivityEntity(

    @PrimaryKey
    @ColumnInfo(name = "walkingActivityId")
    val walkingId: Int,

    @ColumnInfo(name = "steps")
    var steps: Long
)
