package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentNotebookDao {

    @Query("SELECT * FROM recent_notebooks ORDER BY isPinned DESC, lastOpenedAt DESC")
    fun observeAllRecentNotebooks(): Flow<List<RecentNotebookEntity>>

    @Query("SELECT * FROM recent_notebooks WHERE id = :id LIMIT 1")
    suspend fun getNotebookById(id: String): RecentNotebookEntity?

    @Query("SELECT * FROM recent_notebooks WHERE sourceUri = :sourceUri LIMIT 1")
    suspend fun getNotebookByUri(sourceUri: String): RecentNotebookEntity?

    @Query("SELECT COUNT(*) FROM recent_notebooks")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotebook(notebook: RecentNotebookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notebooks: List<RecentNotebookEntity>)

    @Query("UPDATE recent_notebooks SET displayName = :newName, lastModifiedAt = :timestamp WHERE id = :id")
    suspend fun renameNotebook(id: String, newName: String, timestamp: Long)

    @Query("UPDATE recent_notebooks SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("DELETE FROM recent_notebooks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recent_notebooks")
    suspend fun clearAll()
}
