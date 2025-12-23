package com.bestapp.client.ui.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import com.bestapp.client.data.api.models.ServiceCategoryDto
import com.bestapp.client.data.api.models.ServiceTemplateDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    categories: List<ServiceCategoryDto>,
    templates: List<ServiceTemplateDto>,
    onCategorySelected: (ServiceCategoryDto?) -> Unit,
    onTemplateSelected: (ServiceTemplateDto?) -> Unit,
    selectedCategory: ServiceCategoryDto? = null,
    selectedTemplate: ServiceTemplateDto? = null,
    isLoading: Boolean = false
) {
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var currentSubcategories by remember { mutableStateOf<List<ServiceCategoryDto>>(emptyList()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🎯 Выберите услугу",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // Выбор категории
            OutlinedButton(
                onClick = { showCategoryDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selectedCategory?.name ?: "Выберите категорию",
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
            
            // Выбор шаблона (если категория выбрана)
            if (selectedCategory != null) {
                OutlinedButton(
                    onClick = { showTemplateDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedTemplate?.name ?: "Выберите шаблон (опционально)",
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
                
                // Показываем информацию о выбранном шаблоне
                selectedTemplate?.let { template ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = template.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            template.description?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                template.fixedPrice?.let {
                                    Text(
                                        text = "Цена: ${it.toInt()} ₽",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                template.estimatedTime?.let {
                                    Text(
                                        text = "Время: ~$it мин",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Диалог выбора категории
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Выберите категорию") },
            text = {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(categories) { category ->
                            CategoryItem(
                                category = category,
                                onClick = {
                                    onCategorySelected(category)
                                    if (category.subcategoriesCount != null && category.subcategoriesCount > 0) {
                                        currentSubcategories = category.subcategories ?: emptyList()
                                        showCategoryDialog = false
                                        showTemplateDialog = true
                                    } else {
                                        showCategoryDialog = false
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
    
    // Диалог выбора подкатегории/шаблона
    if (showTemplateDialog && selectedCategory != null) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { 
                        showTemplateDialog = false
                        showCategoryDialog = true
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                    Text("${selectedCategory.name} - Выберите услугу")
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Подкатегории
                    if (currentSubcategories.isNotEmpty()) {
                        item {
                            Text(
                                text = "Подкатегории:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(currentSubcategories) { subcategory ->
                            CategoryItem(
                                category = subcategory,
                                onClick = {
                                    onCategorySelected(subcategory)
                                    showTemplateDialog = false
                                }
                            )
                        }
                    }
                    
                    // Шаблоны
                    val categoryTemplates = templates.filter { 
                        it.categoryId == selectedCategory.id || 
                        (currentSubcategories.isNotEmpty() && 
                         currentSubcategories.any { sub -> sub.id == it.categoryId })
                    }
                    
                    if (categoryTemplates.isNotEmpty()) {
                        item {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "Популярные услуги:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(categoryTemplates) { template ->
                            TemplateItem(
                                template = template,
                                onClick = {
                                    onTemplateSelected(template)
                                    showTemplateDialog = false
                                }
                            )
                        }
                    }
                    
                    // Опция "Другое"
                    item {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onTemplateSelected(null)
                                    showTemplateDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "Другое (заполнить вручную)",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplateDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun CategoryItem(
    category: ServiceCategoryDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                category.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (category.subcategoriesCount != null && category.subcategoriesCount > 0) {
                Text(
                    text = "${category.subcategoriesCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TemplateItem(
    template: ServiceTemplateDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            template.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                template.fixedPrice?.let {
                    Text(
                        text = "${it.toInt()} ₽",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                template.estimatedTime?.let {
                    Text(
                        text = "~$it мин",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

