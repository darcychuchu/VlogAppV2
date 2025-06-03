package com.vlog.app.screens.users

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: UserViewModel = hiltViewModel()
) {
    val registerState by viewModel.registerState.collectAsState()
    val usernameValidationState by viewModel.usernameValidationState.collectAsState()
    val nicknameValidationState by viewModel.nicknameValidationState.collectAsState()

    // 添加日志以跟踪验证状态
    LaunchedEffect(usernameValidationState) {
        android.util.Log.d("RegisterScreen", "Username validation state: $usernameValidationState")
    }

    LaunchedEffect(nicknameValidationState) {
        android.util.Log.d("RegisterScreen", "Nickname validation state: $nicknameValidationState")
    }
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }

    // 密码匹配状态
    val passwordsMatch = password == confirmPassword && password.isNotBlank()

    // 处理注册状态变化
    LaunchedEffect(registerState) {
        when (registerState) {
            is RegisterState.Success -> {
                // 注册成功，导航到主页
                onRegisterSuccess()
                // 重置注册状态
                viewModel.resetRegisterState()
            }
            is RegisterState.Error -> {
                // 显示错误消息
                scope.launch {
                    snackBarHostState.showSnackbar((registerState as RegisterState.Error).message)
                }
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("注册") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 用户名输入框
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (username.isNotBlank()) {
                                viewModel.checkUsername(username)
                            }
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    ),
                    singleLine = true,
                    trailingIcon = {
                        if (username.isNotBlank()) {
                            when (usernameValidationState) {
                                is ValidationState.Valid -> {
                                    IconButton(onClick = { username = "" }) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "有效，点击清除",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                is ValidationState.Error -> {
                                    IconButton(onClick = { username = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "无效，点击清除",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                is ValidationState.Loading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                else -> {
                                    IconButton(onClick = { username = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "清除",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    },
                    isError = usernameValidationState is ValidationState.Error,
                    supportingText = {
                        if (usernameValidationState is ValidationState.Error) {
                            Text(
                                text = (usernameValidationState as ValidationState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 密码输入框
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 确认密码输入框
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("确认密码") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    isError = password.isNotBlank() && confirmPassword.isNotBlank() && !passwordsMatch,
                    supportingText = {
                        if (password.isNotBlank() && confirmPassword.isNotBlank() && !passwordsMatch) {
                            Text(
                                text = "密码不匹配",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    trailingIcon = {
                        if (password.isNotBlank() && confirmPassword.isNotBlank()) {
                            if (passwordsMatch) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "密码匹配",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "密码不匹配",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 昵称输入框
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("昵称") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (nickname.isNotBlank()) {
                                viewModel.checkNickname(nickname)
                            }
                            focusManager.clearFocus()
                            if (isFormValid(username, password, confirmPassword, nickname, usernameValidationState, nicknameValidationState)) {
                                viewModel.register(username, password, nickname)
                            }
                        }
                    ),
                    singleLine = true,
                    trailingIcon = {
                        if (nickname.isNotBlank()) {
                            when (nicknameValidationState) {
                                is ValidationState.Valid -> {
                                    IconButton(onClick = { nickname = "" }) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "有效，点击清除",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                is ValidationState.Error -> {
                                    IconButton(onClick = { nickname = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "无效，点击清除",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                is ValidationState.Loading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                else -> {
                                    IconButton(onClick = { nickname = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "清除",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    },
                    isError = nicknameValidationState is ValidationState.Error,
                    supportingText = {
                        if (nicknameValidationState is ValidationState.Error) {
                            Text(
                                text = (nicknameValidationState as ValidationState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 验证按钮
                Button(
                    onClick = {
                        if (username.isNotBlank()) {
                            viewModel.checkUsername(username)
                        }
                        if (nickname.isNotBlank()) {
                            viewModel.checkNickname(nickname)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("验证用户名和昵称")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 注册按钮
                Button(
                    onClick = { viewModel.register(username, password, nickname) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormValid(username, password, confirmPassword, nickname, usernameValidationState, nicknameValidationState) && registerState !is RegisterState.Loading
                ) {
                    if (registerState is RegisterState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("注册")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 返回登录
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("已有账号？")
                    Text(
                        text = "返回登录",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                            .clickable { onNavigateBack() }
                    )
                }
            }

            // 加载指示器
            if (registerState is RegisterState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/**
 * 检查表单是否有效
 */
private fun isFormValid(
    username: String,
    password: String,
    confirmPassword: String,
    nickname: String,
    usernameValidationState: ValidationState,
    nicknameValidationState: ValidationState
): Boolean {
    return username.isNotBlank() &&
            password.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            password == confirmPassword &&
            nickname.isNotBlank() &&
            usernameValidationState is ValidationState.Valid &&
            nicknameValidationState is ValidationState.Valid
}
