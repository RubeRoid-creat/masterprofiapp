package com.bestapp.client.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bestapp.client.data.models.Device

/**
 * Экран выбора проблемы для выбранного устройства
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemSelectorScreen(
    navController: NavController,
    device: Device,
    onProblemSelected: (String) -> Unit
) {
    var selectedProblem by remember { mutableStateOf<String?>(null) }
    var customProblemText by remember { mutableStateOf("") }
    var showCustomProblemDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Выберите проблему") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Информация об устройстве
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Заголовок
            Text(
                text = "Что случилось с техникой?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Список проблем
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(device.commonProblems) { problem ->
                    ProblemCard(
                        problem = problem,
                        isSelected = selectedProblem == problem,
                        onClick = {
                            if (problem == "Другая проблема...") {
                                showCustomProblemDialog = true
                            } else {
                                selectedProblem = problem
                                onProblemSelected(problem)
                                navController.navigateUp()
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Диалог для ввода пользовательской проблемы
    if (showCustomProblemDialog) {
        AlertDialog(
            onDismissRequest = { showCustomProblemDialog = false },
            title = { Text("Опишите проблему") },
            text = {
                OutlinedTextField(
                    value = customProblemText,
                    onValueChange = { customProblemText = it },
                    placeholder = { Text("Например: не включается, шумит, течет...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customProblemText.isNotBlank()) {
                            onProblemSelected(customProblemText)
                            showCustomProblemDialog = false
                            navController.navigateUp()
                        }
                    },
                    enabled = customProblemText.isNotBlank()
                ) {
                    Text("Продолжить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomProblemDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun ProblemCard(
    problem: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (problem == "Другая проблема...") 
                        Icons.Default.Edit 
                    else 
                        Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isSelected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = problem,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
