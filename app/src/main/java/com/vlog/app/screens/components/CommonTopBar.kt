package com.vlog.app.screens.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vlog.app.R
import com.vlog.app.navigation.NavigationRoutes

/**
 * 通用顶部栏
 * 
 * @param title 标题
 * @param navController 导航控制器
 * @param showSearchIcon 是否显示搜索图标，默认为true
 * @param currentRoute 当前路由，用于判断是否显示搜索图标
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopBar(
    title: String,
    navController: NavController,
    showSearchIcon: Boolean = true,
    currentRoute: String? = null
) {
    // 判断当前页面是否是用户页面，如果是则不显示搜索图标
    val isUserPage = currentRoute == NavigationRoutes.MainRoute.Profile.route
    val shouldShowSearchIcon = showSearchIcon && !isUserPage
    
    CenterAlignedTopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            // 左侧Logo
            IconButton(onClick = { /* 点击Logo的操作，可以导航到首页 */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        actions = {
            // 右侧搜索按钮，根据条件显示
            if (shouldShowSearchIcon) {
                IconButton(onClick = { navController.navigate("search") }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search)
                    )
                }
            } else {
                // 为了保持布局平衡，当不显示搜索图标时，添加一个空的Spacer
                Spacer(modifier = Modifier.width(48.dp))
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
