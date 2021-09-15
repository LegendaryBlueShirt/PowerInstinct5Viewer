package com.justnopoint.`interface`

import javafx.beans.property.Property
import javafx.collections.ObservableList

interface FrameDataProvider {
    fun getCharacters(): List<Character>
    fun getCharacterSelection(): Property<Character>
    fun getSequences(): Property<ObservableList<Sequence>>
    fun getFrameRenderer(): FrameRenderer
}