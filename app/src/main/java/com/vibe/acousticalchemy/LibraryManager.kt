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

    fun saveToVault(context: Context, fileName: String, content: String) {
        try {
            val vaultDir = java.io.File(context.filesDir, "vault")
            if (!vaultDir.exists()) {
                vaultDir.mkdirs()
            }
            val file = java.io.File(vaultDir, fileName)
            file.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun listVaultFiles(context: Context): List<String> {
        val vaultDir = java.io.File(context.filesDir, "vault")
        if (!vaultDir.exists()) return emptyList()
        return vaultDir.listFiles()?.filter { it.isFile && it.name.endsWith(".txt") }?.map { it.name } ?: emptyList()
    }
    
    fun loadFromVault(context: Context, fileName: String): String {
        return try {
            val vaultDir = java.io.File(context.filesDir, "vault")
            val file = java.io.File(vaultDir, fileName)
            file.readText()
        } catch (e: Exception) {
            "Error loading file."
        }
    }

    fun importTextFile(context: Context, uri: Uri): Pair<String, String> {
        val stringBuilder = StringBuilder()
        var name = "Imported_Book.txt"
        
        try {
             // Try to get filename
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    name = cursor.getString(nameIndex)
                }
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    stringBuilder.append(reader.readText())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return name to "Error reading file: ${e.localizedMessage}"
        }
        return name to stringBuilder.toString()
    }
}
