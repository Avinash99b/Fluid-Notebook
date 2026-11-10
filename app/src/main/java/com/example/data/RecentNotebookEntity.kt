package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_notebooks")
data class RecentNotebookEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val sourceUri: String,
    val notebookJson: String,
    val kernelName: String,
    val language: String,
    val cellCount: Int,
    val codeCellCount: Int,
    val markdownCellCount: Int,
    val fileSizeBytes: Long,
    val lastOpenedAt: Long,
    val lastModifiedAt: Long,
    val isPinned: Boolean = false
)
