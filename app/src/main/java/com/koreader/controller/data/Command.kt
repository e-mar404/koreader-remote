package com.koreader.controller.data

sealed class Command(val displayName: String, val description: String) {
    object PreviousPage : Command(
        displayName = "Previous Page",
        description = "Go to previous page in KOReader"
    )
    
    object NextPage : Command(
        displayName = "Next Page",
        description = "Go to next page in KOReader"
    )
    
    companion object {
        val allCommands: List<Command>
            get() = listOf(PreviousPage, NextPage)
        
        fun fromDisplayName(name: String): Command? {
            return allCommands.find { it.displayName == name }
        }
    }
}
