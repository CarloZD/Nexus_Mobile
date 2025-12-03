package com.tecsup.nexusmobile.domain.repository

import com.tecsup.nexusmobile.domain.model.Review

interface ReviewRepository {
    suspend fun getReviewsByGameId(gameId: String): Result<List<Review>>
    suspend fun addReview(review: Review): Result<Review>
}

