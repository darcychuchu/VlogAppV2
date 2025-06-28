package com.vlog.app.screens.filter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySettingsScreen(
    navController: NavController,
    onNavigateBack: () -> Unit = {},
    viewModel: CategorySettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 当设置发生变化时，标记需要通知FilterScreen
    var hasChanges by remember { mutableStateOf(false) }
    
    // 监听设置变化
    LaunchedEffect(uiState.enabledCategoryIds, uiState.loginRequiredCategoryIds, uiState.allCategories) {
        hasChanges = true
    }
    
    // 当返回时，如果有变化则通知FilterScreen
    DisposableEffect(Unit) {
        onDispose {
            if (hasChanges) {
                navController.previousBackStackEntry?.savedStateHandle?.set("category_settings_changed", true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分类设置") },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (hasChanges) {
                            navController.previousBackStackEntry?.savedStateHandle?.set("category_settings_changed", true)
                        }
                        navController.navigateUp() 
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.resetToDefault() }
                    ) {
                        Text("重置")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "分类显示设置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "选择要显示的分类，可以拖拽调整顺序",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            itemsIndexed(uiState.allCategories) { index, category ->
                CategorySettingItem(
                    category = category,
                    isEnabled = uiState.enabledCategoryIds.contains(category.id),
                    isLoginRequired = uiState.loginRequiredCategoryIds.contains(category.id),
                    onToggleEnabled = { viewModel.toggleCategoryEnabled(category.id) },
                    onToggleLoginRequired = { viewModel.toggleCategoryLoginRequired(category.id) },
                    onMoveUp = if (index > 0) { { viewModel.moveCategoryUp(index) } } else null,
                    onMoveDown = if (index < uiState.allCategories.size - 1) { { viewModel.moveCategoryDown(index) } } else null
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "登录验证设置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "标记为需要登录的分类，用户必须登录后才能浏览",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
fun CategorySettingItem(
    category: FilterItem,
    isEnabled: Boolean,
    isLoginRequired: Boolean,
    onToggleEnabled: () -> Unit,
    onToggleLoginRequired: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 上移按钮
                    onMoveUp?.let {
                        IconButton(onClick = it) {
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = "上移",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    // 下移按钮
                    onMoveDown?.let {
                        IconButton(onClick = it) {
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = "下移",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { onToggleEnabled() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "显示",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isLoginRequired,
                        onCheckedChange = { onToggleLoginRequired() },
                        enabled = isEnabled
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "需要登录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}