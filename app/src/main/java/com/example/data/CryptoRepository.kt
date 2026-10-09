package com.example.data

import kotlinx.coroutines.flow.Flow

class CryptoRepository(private val dao: CryptoCardDao) {

    val allCards: Flow<List<CryptoCardEntity>> = dao.getAllCards()

    suspend fun getCardById(id: Long): CryptoCardEntity? = dao.getCardById(id)

    suspend fun insertCard(card: CryptoCardEntity): Long = dao.insertCard(card)

    suspend fun updateCard(card: CryptoCardEntity) = dao.updateCard(card)

    suspend fun deleteCard(card: CryptoCardEntity) = dao.deleteCard(card)

    suspend fun deleteCardById(id: Long) = dao.deleteCardById(id)
}
