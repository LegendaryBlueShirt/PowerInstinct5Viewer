package com.justnopoint.interfaces

abstract class Character {
    abstract fun getFullName(): String

    override fun toString(): String {
        return getFullName()
    }
}