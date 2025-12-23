package com.bestapp.client.ui.reports

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bestapp.client.data.repository.ApiResult
import com.bestapp.client.di.AppContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSignScreen(
    navController: NavController,
    reportId: Long,
    orderId: Long,
    viewModel: ReportSignViewModel = viewModel(factory = ReportSignViewModel.provideFactory(reportId))
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var signatureBase64 by remember { mutableStateOf<String?>(null) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(reportId) {
        viewModel.loadReport()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Подписать отчет") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val state = uiState) {
                is ReportSignUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ReportSignUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "Ошибка: ${state.message}",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                is ReportSignUiState.Success -> {
                    val report = state.report
                    
                    // Информация об отчете
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Отчет о выполненной работе",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Divider()
                            
                            Text(
                                text = "Описание работы:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = report.workDescription ?: "Не указано",
                                fontSize = 14.sp
                            )
                            
                            val partsUsed = report.partsUsed
                            if (partsUsed != null && partsUsed.isNotEmpty()) {
                                Text(
                                    text = "Использованные запчасти:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                partsUsed.forEach { part ->
                                    val partName = part.name ?: "Неизвестно"
                                    val partQuantity = part.quantity ?: 0
                                    val partCost = part.cost ?: 0.0
                                    Text(
                                        text = "• $partName - $partQuantity шт. × $partCost ₽",
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Итого:",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${(report.totalCost ?: 0.0).toInt()} ₽",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    // Подпись
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Электронная подпись:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            
                            if (signatureBase64 == null) {
                                Button(
                                    onClick = { showSignatureDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Нарисовать подпись")
                                }
                            } else {
                                Text(
                                    text = "✓ Подпись добавлена",
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Button(
                                    onClick = { 
                                        signatureBase64 = null
                                        showSignatureDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Изменить подпись")
                                }
                            }
                        }
                    }
                    
                    // Кнопка подписания
                    Button(
                        onClick = {
                            if (signatureBase64 != null) {
                                scope.launch {
                                    viewModel.signReport(signatureBase64!!)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = signatureBase64 != null && !state.isSigning
                    ) {
                        if (state.isSigning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Подписать отчет")
                    }
                    
                    if (state.signSuccess) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = "Отчет успешно подписан!",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Диалог для рисования подписи
    if (showSignatureDialog) {
        AlertDialog(
            onDismissRequest = { showSignatureDialog = false },
            title = { Text("Нарисуйте подпись") },
            text = {
                Column {
                    SignatureCanvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.White),
                        onSignatureChanged = { signature ->
                            signatureBase64 = signature
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            signatureBase64 = null
                        }
                    ) {
                        Text("Очистить")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSignatureDialog = false }
                ) {
                    Text("Готово")
                }
            }
        )
    }
}

