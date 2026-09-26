package com.pandeyganesha.kaamsutra.ui.components.todos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel


const val ALL_TAG_ID = "ALL_TAG_ID"

class TodoViewModel : ViewModel() {
    var selectedTagId by mutableStateOf(ALL_TAG_ID)
        private set

    fun selectTag(tagId: String) {
        selectedTagId = tagId
    }
}
