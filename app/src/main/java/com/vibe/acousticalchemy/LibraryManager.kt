package com.vibe.acousticalchemy

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

object LibraryManager {
    
    data class ClassicBook(val id: String, val title: String, val author: String)

    val ClassicManifest = listOf(
        ClassicBook("2701", "Moby Dick", "Herman Melville"),
        ClassicBook("1342", "Pride and Prejudice", "Jane Austen"),
        ClassicBook("11", "Alice's Adventures in Wonderland", "Lewis Carroll"),
        ClassicBook("84", "Frankenstein", "Mary Shelley"),
        ClassicBook("1661", "The Adventures of Sherlock Holmes", "Arthur Conan Doyle"),
        ClassicBook("1952", "The Yellow Wallpaper", "Charlotte Perkins Gilman"),
        ClassicBook("345", "Dracula", "Bram Stoker"),
        ClassicBook("25344", "The Scarlet Letter", "Nathaniel Hawthorne"),
        ClassicBook("76", "Adventures of Huckleberry Finn", "Mark Twain"),
        ClassicBook("98", "A Tale of Two Cities", "Charles Dickens")
    )

    fun importTextFile(context: Context, uri: Uri): String {
        val stringBuilder = StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error reading file: ${e.localizedMessage}"
        }
        return stringBuilder.toString()
    }
}
