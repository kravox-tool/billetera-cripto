package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CryptoCardDao {

    @Query("SELECT * FROM crypto_cards ORDER BY createdAt DESC")
    fun getAllCards(): Flow<List<CryptoCardEntity>>

    @Query("SELECT * FROM crypto_cards WHERE id = :cardId LIMIT 1")
    suspend fun getCardById(cardId: Long): CryptoCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CryptoCardEntity): Long

    @Update
    suspend fun updateCard(card: CryptoCardEntity)

    @Delete
    suspend fun deleteCard(card: CryptoCardEntity)

    @Query("DELETE FROM crypto_cards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: Long)
}
