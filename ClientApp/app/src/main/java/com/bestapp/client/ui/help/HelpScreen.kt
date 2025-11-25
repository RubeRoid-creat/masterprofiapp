package com.bestapp.client.ui.help

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bestapp.client.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    navController: NavController
) {
    var expandedItem by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Помощь и поддержка") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
        ) {
            // Контакты поддержки
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Связаться с поддержкой",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+7 (800) 123-45-67",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "support@bestapp.ru",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // FAQ
            Text(
                text = "Часто задаваемые вопросы",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val faqItems = listOf(
                FAQItem(
                    question = "Как создать заказ?",
                    answer = "Нажмите кнопку 'Создать заказ' на главном экране, заполните форму с описанием проблемы и выберите адрес. После этого система автоматически найдет подходящего мастера."
                ),
                FAQItem(
                    question = "Сколько стоит ремонт?",
                    answer = "Стоимость зависит от типа техники и сложности ремонта. Примерные цены указаны в каталоге услуг. Точную стоимость мастер определит после диагностики."
                ),
                FAQItem(
                    question = "Как оплатить заказ?",
                    answer = "Оплата возможна наличными, банковской картой или онлайн. Вы можете выбрать способ оплаты при создании заказа."
                ),
                FAQItem(
                    question = "Что делать, если мастер не приехал?",
                    answer = "Свяжитесь с поддержкой по телефону или через чат в приложении. Мы решим проблему в кратчайшие сроки."
                ),
                FAQItem(
                    question = "Как работает программа лояльности?",
                    answer = "За каждый заказ вы получаете баллы, которые можно использовать для получения скидок на следующие заказы. 1 балл = 1 рубль."
                )
            )

            faqItems.forEachIndexed { index, item ->
                FAQCard(
                    item = item,
                    isExpanded = expandedItem == index,
                    onExpandClick = {
                        expandedItem = if (expandedItem == index) null else index
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FAQCard(
    item: FAQItem,
    isExpanded: Boolean,
    onExpandClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onExpandClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.question,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class FAQItem(
    val question: String,
    val answer: String
)

