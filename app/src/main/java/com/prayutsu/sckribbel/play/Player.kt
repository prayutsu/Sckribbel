package com.prayutsu.sckribbel.play

import kotlin.Comparable

data class Player(var username: String, var profileImageUrl: String) : Comparable<Player> {
    constructor() : this("", "")

    var points = 0
    var hasAlreadyGuessed = false
    var indexOfTurn: Int? = null
    var currentDrawer: Boolean = false

    fun reset() {
        hasAlreadyGuessed = false
    }

    fun addPoints(reward: Int) {
        points += reward
    }

    override fun toString(): String {
        return "$username:$points"
    }


    override fun compareTo(other: Player): Int {
        return other.points - points
    }
}