package com.justnopoint.interfaces

interface FrameDataProvider {
    fun getCharacters(): List<Character>
    fun setSelectedCharacter(character: Character)
    fun getSequences(): List<Sequence>
    fun getFrameRenderer(): FrameRenderer
}