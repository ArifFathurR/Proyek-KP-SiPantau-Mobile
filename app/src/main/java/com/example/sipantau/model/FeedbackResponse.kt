package com.example.sipantau.model

data class FeedbackResponse(
    val status: String,
    val message: String,
    val data: List<Feedback>

)
data class FeedbackCreateResponse(
    val status: Boolean,
    val message: String,
    val data: Feedback
)

data class Feedback(
    val id_feedback: Int?,
    val sobat_id: Long?,
    val feedback: String,
    val rating: Int?,
    val created_at: String
)