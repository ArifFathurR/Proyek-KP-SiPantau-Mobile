package com.example.sipantau.model

data class ApproveResponse(
    val status: Boolean,
    val message: String,
    val data: ApproveData?
)

data class ApproveData(
    val id_pcl: Int,
    val status_approval: String
)