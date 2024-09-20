package com.harish.drivemaster.models

data class LeaderboardUser(
    val userId: String = "",
    val userName: String = "",
    val points: Int = 0,
    val streak: Int = 0
)