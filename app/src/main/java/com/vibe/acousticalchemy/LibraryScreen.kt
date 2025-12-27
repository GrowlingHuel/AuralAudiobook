package com.vibe.acousticalchemy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LibraryScreen(
    onBookSelected: (String, String, Boolean) -> Unit, // Title, Content, isClassic
    onImport: () -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Classics, 1: Vault
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF080808)).padding(16.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                Text("< Lab", color = Color.Gray)
            }
            Text("The Vault", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Tabs
        TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = Color.Cyan) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Classics", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Private Vault", modifier = Modifier.padding(16.dp))
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // List
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                // Classics
                items(LibraryManager.ClassicManifest.size) { i ->
                    val book = LibraryManager.ClassicManifest[i]
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                             onBookSelected(book.title, "Placeholder for Classic: ${book.title}...", true)
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(book.title, style = MaterialTheme.typography.titleMedium, color = Color.Cyan)
                            Text(book.author, color = Color.Gray)
                        }
                    }
                }
            } else {
                // Vault
                val files = LibraryManager.listVaultFiles(context)
                item {
                    Button(
                        onClick = onImport,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("+ Import New Text")
                    }
                }
                items(files.size) { i ->
                    val name = files[i]
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                             val content = LibraryManager.loadFromVault(context, name)
                             onBookSelected(name, content, false)
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
