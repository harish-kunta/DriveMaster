package com.harish.drivemaster.models

data class Level(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val isLocked: Boolean = true
)