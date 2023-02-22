package com.ic.mymemory.models

data class MemoryCard(
    val identifier: Int,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false // var-> can be changed val-> can never be changed once its set to a value
)