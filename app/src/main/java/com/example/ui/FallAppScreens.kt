package com.example.ui

import kotlin.math.sqrt
import com.example.viewmodel.FallAlertState

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Divider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import com.example.viewmodel.ImpactEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FallLog
import com.example.viewmodel.FallViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
fun FallAppMainContainer(viewModel: FallViewModel) {
    val isSetupComplete by viewModel.isSetupComplete.collectAsState()
    val activeAlertState by viewModel.activeAlertState.collectAsState()
    val isListeningVoice by viewModel.isListeningVoice.collectAsState()
    val smsSimulationLog by viewModel.smsSimulationLog.collectAsState()
    val dialSimulationActive by viewModel.dialSimulationActive.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.fetchLocationData()
            if (isSetupComplete) {
                viewModel.startLocationUpdates()
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.RECORD_AUDIO
            )
        )
    }

    // Request sensors and location updates when active
    LaunchedEffect(isSetupComplete) {
        if (isSetupComplete) {
            viewModel.startSensorMonitoring()
            val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
                viewModel.startLocationUpdates()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (!isSetupComplete) {
            SetupScreen(
                onSave = { name, gName, gPhone, eContact, eTarget ->
                    viewModel.saveSetup(name, gName, gPhone, eContact, eTarget)
                }
            )
        } else {
            DashboardLayout(viewModel = viewModel)
        }

        // Emergency Overlay Modal
        activeAlertState?.let { alert ->
            EmergencyOverlay(
                alertState = alert,
                isListeningVoice = isListeningVoice,
                onDismiss = { viewModel.userDismissedAlert() },
                onEmergency = { viewModel.userRequestedEmergency() },
                onCloseNoResponse = { viewModel.closeNoResponseAlert() }
            )
        }

        // SMS Simulation Log Modal
        smsSimulationLog?.let { logText ->
            SmsSimulationDialog(
                logText = logText,
                onDismiss = { viewModel.userDismissedAlert() } // also clears log on VM
            )
        }

        // Phone call Simulation Overlay
        if (dialSimulationActive) {
            val targetLabel = viewModel.getEmergencyTargetLabel()
            val targetNumber = viewModel.getEmergencyNumber()
            DialerSimulationOverlay(
                targetLabel = targetLabel,
                targetNumber = targetNumber,
                onEndCall = { viewModel.dismissDialSimulation() }
            )
        }
    }
}

@Composable
fun SetupScreen(onSave: (String, String, String, String, String) -> Unit) {
    val focusManager = LocalFocusManager.current
    var userName by remember { mutableStateOf("") }
    var guardianName by remember { mutableStateOf("") }
    var guardianPhone by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }
    var emergencyTarget by remember { mutableStateOf("119") }

    var nameError by remember { mutableStateOf(false) }
    var gNameError by remember { mutableStateOf(false) }
    var gPhoneError by remember { mutableStateOf(false) }
    var customContactError by remember { mutableStateOf(false) }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .testTag("setup_screen"),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "App Shield Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 8.dp)
                )
                Text(
                    text = "AI 충격 감지 시스템",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "사고 실시간 감지 및 긴급 구조 요청 설정",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "사용자 정보 등록",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val focusManager = LocalFocusManager.current

                        OutlinedTextField(
                            value = userName,
                            onValueChange = {
                                userName = it
                                nameError = false
                            },
                            label = { Text("사용자 이름", fontWeight = FontWeight.Bold) },
                            placeholder = { Text("예: 홍길동") },
                            modifier = Modifier.fillMaxWidth().testTag("username_input"),
                            isError = nameError,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        OutlinedTextField(
                            value = guardianName,
                            onValueChange = {
                                guardianName = it
                                gNameError = false
                            },
                            label = { Text("보호자 이름", fontWeight = FontWeight.Bold) },
                            placeholder = { Text("예: 김철수") },
                            modifier = Modifier.fillMaxWidth().testTag("guardian_name_input"),
                            isError = gNameError,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        OutlinedTextField(
                            value = guardianPhone,
                            onValueChange = {
                                guardianPhone = it
                                gPhoneError = false
                            },
                            label = { Text("보호자 전화번호", fontWeight = FontWeight.Bold) },
                            placeholder = { Text("예: 01012345678") },
                            modifier = Modifier.fillMaxWidth().testTag("guardian_phone_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            isError = gPhoneError,
                            singleLine = true
                        )
                    }
                }
            }

            // Target selector option card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "긴급 신고 대상 설정 (직접 선택)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "충격 사고 자동 감지 시, 비상 카운트다운 완료 후 자동으로 신고를 전달할 주 대상을 직접 결정할 수 있습니다.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        val targets = listOf(
                            Triple("119", "🚨 119 소방본부", "119 소방긴급구조대로 즉시 다이얼"),
                            Triple("police", "👮 경찰청 (112)", "경찰청 긴급구조신고센터로 즉시 다이얼"),
                            Triple("guardian", "👤 등록 보호자", "위 등록된 보호자 번호로 비상 연락 발신"),
                            Triple("custom", "📞 비상연락처 직접 입력하기", "비상 연락처 직접 입력하여 긴급 신고하기")
                        )

                        targets.forEach { (value, label, desc) ->
                            val isSelected = emergencyTarget == value
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            emergencyTarget = value
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            emergencyTarget = value
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = label,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (value == "custom" && isSelected) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = emergencyContact,
                                        onValueChange = { 
                                            emergencyContact = it 
                                            customContactError = false
                                        },
                                        label = { Text("비상 연락처 직접 입력", fontWeight = FontWeight.Bold) },
                                        placeholder = { Text("예: 01012345678 또는 119") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp)
                                            .testTag("emergency_contact_input"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        isError = customContactError,
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        var hasError = false
                        if (userName.trim().isEmpty()) {
                            nameError = true
                            hasError = true
                        }
                        if (guardianName.trim().isEmpty()) {
                            gNameError = true
                            hasError = true
                        }
                        if (guardianPhone.trim().isEmpty()) {
                            gPhoneError = true
                            hasError = true
                        }
                        if (emergencyTarget == "custom" && emergencyContact.trim().isEmpty()) {
                            customContactError = true
                            hasError = true
                        }

                        if (!hasError) {
                            onSave(userName, guardianName, guardianPhone, emergencyContact, emergencyTarget)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("save_settings_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Check")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("저장 및 감지 시작", fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun DashboardLayout(viewModel: FallViewModel) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            AppHeaderBar(viewModel = viewModel)
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_navigation")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = "감지") },
                    label = { Text("실시간 감지") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.History, contentDescription = "기록") },
                    label = { Text("사고 기록") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "설정") },
                    label = { Text("시스템 설정") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> RealtimeDashboard(viewModel = viewModel)
                1 -> HistoryScreen(viewModel = viewModel)
                2 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RealtimeDashboard(viewModel: FallViewModel) {
    val userName by viewModel.userName.collectAsState()
    val rX by viewModel.realtimeX.collectAsState()
    val rY by viewModel.realtimeY.collectAsState()
    val rZ by viewModel.realtimeZ.collectAsState()
    val rTilt by viewModel.realtimeTilt.collectAsState()
    val rRotSpeed by viewModel.realtimeRotSpeed.collectAsState()
    val sensorStatus by viewModel.sensorStatusMessage.collectAsState()
    val aiStatus by viewModel.aiAnalysisStatus.collectAsState()
    val liveLat by viewModel.currentLatitude.collectAsState()
    val liveLng by viewModel.currentLongitude.collectAsState()
    val liveAddress by viewModel.currentAddress.collectAsState()
    val guidanceTitle by viewModel.guidanceTitle.collectAsState()
    val guidanceMessage by viewModel.guidanceMessage.collectAsState()

    val totalG = sqrt(rX * rX + rY * rY + rZ * rZ) / 9.8f
    val isAnalyzing = aiStatus == "ANALYZING"
    
    var isSensorExpanded by remember { mutableStateOf(true) }
    var locationCopied by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current

    // Automatically clear copy status after 2 seconds
    LaunchedEffect(locationCopied) {
        if (locationCopied) {
            kotlinx.coroutines.delay(2000)
            locationCopied = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("dashboard_tab"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Premium Minimalism Header (Welcome Section)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI FALL DETECTOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "안녕하세요, ${userName}님",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // 1.5. Safety Guidance Alert Banner (Shows when a mild shock is detected)
        guidanceMessage?.let { msg ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .testTag("safety_guidance_banner"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Guidance Icon",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = guidanceTitle ?: "안전 가이드",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissGuidance() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Text(
                            text = msg,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { viewModel.dismissGuidance() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("확인했습니다", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }





        // 3. PREMIUM HIGH-READABILITY LOCATION CARD (Segmented & Visualized GPS Console)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp, 
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), 
                        shape = RoundedCornerShape(24.dp)
                    )
                    .testTag("gps_center_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title Bar & Active Pulse Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "GPS",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(22.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = "GPS 기준 지도",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "설명이 잘려 보일 수 있으며, 현재 지도가 렌더링되지 않아 나오지 않을 수 있습니다.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Satellite Status Pill with simulated flash
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(100.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF4CAF50), CircleShape)
                                )
                                Text(
                                    text = "D-GPS 초정밀",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    // 1. Coordinates Grid (Latitude & Longitude Cards)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Latitude Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "위도 (LATITUDE)",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format("%.6f", liveLat),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "N (북위) - 정밀 고정",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Longitude Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "경도 (LONGITUDE)",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format("%.6f", liveLng),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "E (동경) - 실시간 갱신",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 실시간 위치 추적 지도 표시 (Stylized Vector Map with Interactive Controls)
                    StylizedVectorMap(
                        latitude = liveLat,
                        longitude = liveLng,
                        address = liveAddress,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Copy Coordinates Button
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString("${String.format("%.6f", liveLat)}, ${String.format("%.6f", liveLng)}"))
                                locationCopied = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (locationCopied) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (locationCopied) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = if (locationCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (locationCopied) "복사 완료" else "좌표 복사",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Map Open Button
                        Button(
                            onClick = {
                                val mapUrl = "https://maps.google.com/?q=${liveLat},${liveLng}"
                                uriHandler.openUri(mapUrl)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Map",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "구글 지도",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. STRESS-FREE STABLE SENSOR TRAY (Collapsible)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp, 
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), 
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header Clickable Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSensorExpanded = !isSensorExpanded }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Sensor Status",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "실시간 기기 움직임 및 센서 진단",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isSensorExpanded) "실시간 원형 데이터 수치" else "기울기: ${String.format("%.1f", rTilt)}° | 중력강도: ${String.format("%.2f", totalG)}G",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Gentle stability status pill
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (totalG > 2.0f) MaterialTheme.colorScheme.errorContainer 
                                                else MaterialTheme.colorScheme.secondaryContainer, 
                                        shape = RoundedCornerShape(100.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (totalG > 2.0f) "충격 감지" else "안정 상태",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalG > 2.0f) MaterialTheme.colorScheme.onErrorContainer 
                                            else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            
                            Icon(
                                imageVector = if (isSensorExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Sensors",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Collapsible sensor contents with animation
                    AnimatedVisibility(visible = isSensorExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )
                            
                            // X, Y, Z progress indicators with distinct beautiful color themes
                            SensorLinearProgress(label = "X축 가속도 (좌우 움직임)", value = rX, maxRange = 20f, color = Color(0xFFE57373))
                            SensorLinearProgress(label = "Y축 가속도 (앞뒤 움직임)", value = rY, maxRange = 20f, color = Color(0xFF81C784))
                            SensorLinearProgress(label = "Z축 가속도 (수직 움직임)", value = rZ, maxRange = 20f, color = Color(0xFF64B5F6))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                             ) {
                                 // Rotation Card
                                 Card(
                                     modifier = Modifier.weight(1f),
                                     shape = RoundedCornerShape(16.dp),
                                     colors = CardDefaults.cardColors(
                                         containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                     )
                                 ) {
                                     Column(modifier = Modifier.padding(12.dp)) {
                                         Icon(
                                             imageVector = Icons.Default.Speed,
                                             contentDescription = "Gyro",
                                             tint = MaterialTheme.colorScheme.primary,
                                             modifier = Modifier.size(18.dp)
                                         )
                                         Spacer(modifier = Modifier.height(4.dp))
                                         Text("회전 속도", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                         Text("${String.format("%.1f", rRotSpeed)} °/s", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                     }
                                 }

                                 // Tilt Card
                                 Card(
                                     modifier = Modifier.weight(1f),
                                     shape = RoundedCornerShape(16.dp),
                                     colors = CardDefaults.cardColors(
                                         containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                     )
                                 ) {
                                     Column(modifier = Modifier.padding(12.dp)) {
                                         Icon(
                                             imageVector = Icons.Default.DeviceUnknown,
                                             contentDescription = "Tilt",
                                             tint = MaterialTheme.colorScheme.primary,
                                             modifier = Modifier.size(18.dp)
                                         )
                                         Spacer(modifier = Modifier.height(4.dp))
                                         Text("기기 기울기", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                         Text("${String.format("%.1f", rTilt)} °", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                     }
                                 }
                             }
                             
                             // Sensor status source text
                             Text(
                                 text = "센서 작동 상태: $sensorStatus",
                                 fontSize = 11.sp,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant,
                                 textAlign = TextAlign.Center,
                                 modifier = Modifier.fillMaxWidth(),
                                 fontWeight = FontWeight.Bold
                             )
                         }
                     }
                 }
             }
         }

        // 5. SYSTEM HEALTH & SENSOR CONNECTIVITY DASHBOARD (Battery & Sensor status with Progress Bars)
        if (false) { // Disabled from original position, moved to the bottom of the scroll view
        item {
            val batteryLevel by viewModel.batteryLevel.collectAsState()
            val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
            val sensorStatusDetail by viewModel.sensorStatusMessage.collectAsState()
            
            // Calculate a progress value for connectivity:
            // 1.0f (100%) for active monitoring
            // 0.7f (70%) for simulation mode (e.g. sensor unavailable, running in emulator/virtual mode)
            // 0.0f (0%) for stopped or idle
            val connectivityProgress = when {
                sensorStatusDetail.contains("감지 중") -> 1.0f
                sensorStatusDetail.contains("정상") -> 1.0f
                sensorStatusDetail.contains("시뮬레이션") -> 0.7f
                sensorStatusDetail.contains("중단") || sensorStatusDetail.contains("대기") -> 0.0f
                else -> 0.5f
            }
            
            val connectivityColor = when {
                connectivityProgress >= 0.9f -> Color(0xFF4CAF50) // Healthy Green
                connectivityProgress >= 0.5f -> Color(0xFF2196F3) // Simulated Blue
                else -> Color(0xFFF44336) // Unconnected/Inactive Red
            }

            val batteryColor = when {
                isBatteryCharging -> Color(0xFF4CAF50)
                batteryLevel < 20 -> Color(0xFFF44336)
                batteryLevel < 50 -> Color(0xFFFF9800)
                else -> Color(0xFF4CAF50)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .testTag("system_health_dashboard_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "System Health",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "실시간 시스템 및 센서 진단",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "배터리 수준 및 물리 센서 연결 안전성 통합 진단",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Status Pill
                        val systemStatusLabel = when {
                            batteryLevel < 20 && !isBatteryCharging -> "주의 (배터리 부족)"
                            connectivityProgress == 0.0f -> "센서 대기"
                            connectivityProgress == 0.7f -> "점검 (모의 작동)"
                            else -> "정상 작동"
                        }
                        val systemStatusBgColor = when {
                            batteryLevel < 20 && !isBatteryCharging -> Color(0xFFFFEBEE)
                            connectivityProgress == 0.0f -> Color(0xFFFFF3E0)
                            connectivityProgress == 0.7f -> Color(0xFFE3F2FD)
                            else -> Color(0xFFE8F5E9)
                        }
                        val systemStatusTextColor = when {
                            batteryLevel < 20 && !isBatteryCharging -> Color(0xFFC62828)
                            connectivityProgress == 0.0f -> Color(0xFFE65100)
                            connectivityProgress == 0.7f -> Color(0xFF1565C0)
                            else -> Color(0xFF2E7D32)
                        }

                        Box(
                            modifier = Modifier
                                .background(systemStatusBgColor, RoundedCornerShape(100.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = systemStatusLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = systemStatusTextColor
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // 1. DEVICE BATTERY LEVEL PROGRESS BAR
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed, // Using Speed icon as battery load/usage gauge
                                    contentDescription = "Battery Status",
                                    tint = batteryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "기기 배터리 잔량",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isBatteryCharging) {
                                    Text(
                                        text = "[충전 중]",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                                Text(
                                    text = "$batteryLevel%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = batteryColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Premium Progressive Gauge Indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            LinearProgressIndicator(
                                progress = batteryLevel.toFloat() / 100f,
                                modifier = Modifier.fillMaxSize(),
                                color = batteryColor,
                                trackColor = Color.Transparent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. REAL-TIME SENSOR CONNECTIVITY STATUS PROGRESS BAR
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh, // Using Refresh for continuous loop/connection sync
                                    contentDescription = "Sensor Stream",
                                    tint = connectivityColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "실시간 센서 스트리밍 연결도",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Text(
                                text = "${(connectivityProgress * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = connectivityColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Connectivity Progress Indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            LinearProgressIndicator(
                                progress = connectivityProgress,
                                modifier = Modifier.fillMaxSize(),
                                color = connectivityColor,
                                trackColor = Color.Transparent
                            )
                        }
                        
                        // Individual real-time sensor connection & status panel
                        val hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED || androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                        val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.RECORD_AUDIO
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                        val isListeningVoiceActive by viewModel.isListeningVoice.collectAsState()

                        val accelState = when {
                            sensorStatusDetail.contains("정상 작동") -> "STABLE"
                            sensorStatusDetail.contains("시뮬레이션") -> "WEAK"
                            else -> "DISCONNECTED"
                        }

                        val gyroState = when {
                            sensorStatusDetail.contains("정상 작동") -> "STABLE"
                            sensorStatusDetail.contains("시뮬레이션") -> "WEAK"
                            else -> "DISCONNECTED"
                        }

                        val gpsState = when {
                            hasLocationPermission && (liveLat != 37.5665 || liveLng != 126.9780) -> "STABLE"
                            hasLocationPermission -> "WEAK"
                            else -> "DISCONNECTED"
                        }

                        val micState = when {
                            hasMicPermission && isListeningVoiceActive -> "STABLE"
                            hasMicPermission -> "WEAK"
                            else -> "DISCONNECTED"
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "실시간 개별 센서 연결 분석 (Color-Coded Diagnostics)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SensorStatusCard(
                                    sensorName = "가속도 센서",
                                    sensorIcon = Icons.Default.Speed,
                                    statusState = accelState,
                                    modifier = Modifier.weight(1f)
                                )
                                SensorStatusCard(
                                    sensorName = "자이로 센서",
                                    sensorIcon = Icons.Default.Refresh,
                                    statusState = gyroState,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SensorStatusCard(
                                    sensorName = "GPS 위성 센서",
                                    sensorIcon = Icons.Default.LocationOn,
                                    statusState = gpsState,
                                    modifier = Modifier.weight(1f)
                                )
                                SensorStatusCard(
                                    sensorName = "음성인식 마이크",
                                    sensorIcon = Icons.Default.NotificationsActive,
                                    statusState = micState,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
 }
        } // Closing the if (false) statement
 
 @Composable
 fun SensorLinearProgress(label: String, value: Float, maxRange: Float, color: Color = MaterialTheme.colorScheme.primary) {
     val progress = (value.absoluteValue / maxRange).coerceIn(0f, 1f)
     Column {
         Row(
             modifier = Modifier.fillMaxWidth(),
             horizontalArrangement = Arrangement.SpaceBetween
         ) {
             Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
             Text(text = "${String.format("%.2f", value)} m/s²", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
         }
         Spacer(modifier = Modifier.height(4.dp))
         LinearProgressIndicator(
             progress = { progress },
             modifier = Modifier
                 .fillMaxWidth()
                 .height(6.dp)
                 .clip(RoundedCornerShape(3.dp)),
             color = if (value.absoluteValue > 15f) MaterialTheme.colorScheme.error else color,
             trackColor = MaterialTheme.colorScheme.surfaceVariant,
         )
     }
 }

@Composable
fun SensorStatusCard(
    sensorName: String,
    sensorIcon: androidx.compose.ui.graphics.vector.ImageVector,
    statusState: String,
    modifier: Modifier = Modifier
) {
    val (statusIcon, statusColor, statusLabel) = when (statusState) {
        "STABLE" -> Triple(Icons.Default.Check, Color(0xFF4CAF50), "안정 (정상)")
        "WEAK" -> Triple(Icons.Default.Warning, Color(0xFFFFA500), "대기 (모의)")
        "DISCONNECTED" -> Triple(Icons.Default.Block, Color(0xFFF44336), "미인가 (차단)")
        else -> Triple(Icons.Default.Info, Color.Gray, "미지원")
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(statusColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = sensorIcon,
                        contentDescription = sensorName,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = sensorName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statusLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }
            Icon(
                imageVector = statusIcon,
                contentDescription = statusLabel,
                tint = statusColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun HistoryScreen(viewModel: FallViewModel) {
    val logs by viewModel.logsList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("history_tab")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "사고 분석 및 처리 이력",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (logs.isNotEmpty()) {
                OutlinedButton(
                    onClick = { viewModel.clearAllLogs() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("전체 삭제", fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Empty",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "기록된 충격 및 사고 기록이 없습니다.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(logs) { log ->
                    LogItemCard(log = log, onDelete = { viewModel.deleteLog(log.id) })
                }
            }
        }
    }
}

@Composable
fun LogItemCard(log: FallLog, onDelete: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val formattedDate = sdf.format(Date(log.timestamp))

    val statusColor = when (log.aiResult) {
        "응급상황 가능성 높음" -> Color.Red
        "낙상 의심" -> Color(0xFFFFA500)
        else -> Color.Green
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (log.aiResult == "낙상 의심") "충격 의심" else log.aiResult,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = statusColor
                    )
                    if (log.isSimulated) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("모의 테스트", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Item", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "감지 일시: $formattedDate",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "사용자 응답: ${log.userResponse}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "충격 강도",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "감지 충격 강도: ${String.format("%.1f", log.maxAccel)} m/s² (${String.format("%.2f", log.maxAccel / 9.8f)} G)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location coordinates and link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        val mapUrl = "https://maps.google.com/?q=${log.latitude},${log.longitude}"
                        uriHandler.openUri(mapUrl)
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "위치: 위도 ${String.format("%.4f", log.latitude)}, 경도 ${String.format("%.4f", log.longitude)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "지도 바로가기 (보호자 문자 공유 전송값)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Map link icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: FallViewModel) {
    val userName by viewModel.userName.collectAsState()
    val guardianName by viewModel.guardianName.collectAsState()
    val guardianPhone by viewModel.guardianPhone.collectAsState()
    val emergencyContact by viewModel.emergencyContact.collectAsState()
    val emergencyTarget by viewModel.emergencyTarget.collectAsState()
    val sensitivity by viewModel.sensitivity.collectAsState()
    val sensitivityThreshold by viewModel.sensitivityThreshold.collectAsState()
    val warningThreshold by viewModel.warningThreshold.collectAsState()
    val emergencyThreshold by viewModel.emergencyThreshold.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val isRealtimeSimActive by viewModel.isRealtimeSimulationActive.collectAsState()
    val realtimeSimPhase by viewModel.realtimeSimulationPhase.collectAsState()
    val gForceHistory by viewModel.gForceHistory.collectAsState()
    val peakGForce by viewModel.peakGForce.collectAsState()
    val impactEvents by viewModel.impactEvents.collectAsState()
    
    val isCalibrating by viewModel.isCalibrating.collectAsState()
    val calibrationStep by viewModel.calibrationStep.collectAsState()
    val calibrationBaselinePeak by viewModel.calibrationBaselinePeak.collectAsState()
    val calibrationImpactPeak by viewModel.calibrationImpactPeak.collectAsState()
    val calibrationCountdown by viewModel.calibrationCountdown.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("settings_tab"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "긴급 모듈 및 사용자 설정",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Contact info configuration card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "비상 대처 연락망 정보",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.testTag("edit_setup_button")
                        ) {
                            Text("수정")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsDetailRow(label = "사용자 이름", value = userName)
                    SettingsDetailRow(label = "등록 보호자", value = "$guardianName ($guardianPhone)")
                    SettingsDetailRow(
                        label = "긴급 비상 연락처",
                        value = if (emergencyContact.isEmpty()) "미지정 (기본 119 다이얼 시뮬레이션)" else emergencyContact
                    )
                    SettingsDetailRow(
                        label = "긴급 신고 대상",
                        value = when (emergencyTarget) {
                            "119" -> "🚨 119 소방본부"
                            "police" -> "👮 경찰청 (112)"
                            "guardian" -> "👤 등록 보호자: $guardianName"
                            "custom" -> "📞 지정 연락처: $emergencyContact"
                            else -> "🚨 119 소방본부"
                        }
                    )
                }
            }
        }

        // Guided Calibration Card
        item {
            GuidedCalibrationWizard(
                isCalibrating = isCalibrating,
                calibrationStep = calibrationStep,
                baselinePeak = calibrationBaselinePeak,
                impactPeak = calibrationImpactPeak,
                countdown = calibrationCountdown,
                currentThreshold = sensitivityThreshold,
                onStart = { viewModel.startCalibration() },
                onCancel = { viewModel.cancelCalibration() },
                onNextStep = { viewModel.setCalibrationStep(it) },
                onApply = { viewModel.applyCalibratedThreshold(it) },
                modifier = Modifier.fillMaxWidth().testTag("guided_calibration_card")
            )
        }

        // Sensitivity configuration card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "충격 감지 물리 감도 설정",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "임계값이 낮을수록 미세한 움직임에도 쉽게 반응하며, 임계값이 높을수록 강한 외부 충격에만 반응합니다. 슬라이더를 조절하여 감도를 상세히 맞출 수 있습니다.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "📱 내 스마트폰 기종 맞춤형 감도 설정",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val selectedPreset by viewModel.phonePreset.collectAsState()
                    val presets = listOf(
                        "galaxy_s" to "갤럭시 S/Note",
                        "galaxy_a" to "갤럭시 A/M",
                        "pixel" to "구글 픽셀",
                        "other" to "기타 기종",
                        "custom" to "직접 설정"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        presets.forEach { (presetValue, presetLabel) ->
                            val isPresetSelected = selectedPreset == presetValue
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(
                                        if (isPresetSelected) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable { viewModel.updatePhonePreset(presetValue) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = presetLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPresetSelected) MaterialTheme.colorScheme.onPrimary 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Two separate sliders for Warning & Emergency Thresholds
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Warning threshold
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️ 안내 메시지 및 경고 임계값",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${String.format("%.1f", warningThreshold)} m/s²",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Slider(
                                value = warningThreshold,
                                onValueChange = { viewModel.updateWarningThreshold(it) },
                                valueRange = 0.0f..60.0f,
                                modifier = Modifier.fillMaxWidth().testTag("warning_threshold_slider")
                            )
                            Text(
                                text = "기기에 경미한 충격이 감지될 때 친절한 안전 수칙 안내 음성(TTS)과 시각 배너가 표출되는 물리 강도 기준입니다.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // 2. Emergency threshold
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🚨 긴급 경보 화면(UI) 표시 임계값",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${String.format("%.1f", emergencyThreshold)} m/s²",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = emergencyThreshold,
                                onValueChange = { viewModel.updateEmergencyThreshold(it) },
                                valueRange = 0.0f..60.0f,
                                modifier = Modifier.fillMaxWidth().testTag("emergency_threshold_slider")
                            )
                            Text(
                                text = "실제 무의식 또는 위급 낙상사고로 판단하여 강력한 비상 사이렌 소리, 30초 카운트다운 오버레이 UI, 비상 문자 전송을 작동시키는 임계 기준입니다.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Real-time G-force History Calibration Chart
        item {
            RechartsStyleGForceChart(
                history = gForceHistory,
                threshold = sensitivityThreshold,
                peakGForce = peakGForce,
                onResetPeak = {
                    viewModel.clearGForceHistory()
                    viewModel.resetPeakGForce()
                },
                modifier = Modifier.fillMaxWidth().testTag("g_force_calibration_chart")
            )
        }

        // Detection History Section
        item {
            DetectionHistorySection(
                impactEvents = impactEvents,
                onClearHistory = { viewModel.clearImpactEvents() },
                modifier = Modifier.fillMaxWidth().testTag("detection_history_section")
            )
        }

        // Sound / Vibration Toggles
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "알람 경고음 및 촉각 진동 설정",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("비상 사이렌 경고음 재생", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("충격 진단 시 최대 음량 사운드로 긴급 경고", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { viewModel.updateSoundEnabled(it) },
                            modifier = Modifier.testTag("sound_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("기기 충격 알람 진동 패턴", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("충격 감지 경보음과 연동하여 강한 펄스 진동 인입", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = { viewModel.updateVibrationEnabled(it) },
                            modifier = Modifier.testTag("vibration_switch")
                        )
                    }
                }
            }
        }

        // Factory reset setup details
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "시스템 데이터 완전 초기화",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "이 버튼을 누르면 보호자 연락처, 사용자 정보 및 이전 사고 기밀 이력 등의 모든 정보가 로컬 저장소에서 제거되며 초기 셋업 화면으로 강제 이동합니다.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(
                        onClick = { viewModel.resetSetup() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                        modifier = Modifier.fillMaxWidth().testTag("reset_setup_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("사용자 설정 전체 초기화", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Test Mode Trigger button styled with heavy borders and custom shape (Moved here under settings!)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "충격 모의 테스트 기능 (시뮬레이션)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "실제 충격을 가하지 않고 가상의 충격 시나리오(강한 충격 후 부동 상태)를 임의로 인입하여 AI 판독 및 30초 긴급 구조 경보 전 과정을 즉시 시뮬레이션해봅니다.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    if (isRealtimeSimActive) {
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Active simulation status display
                        val (phaseTitle, phaseDesc, progressValue) = when (realtimeSimPhase) {
                            "WALKING" -> Triple(
                                "1단계: 일반 보행 활동 수집 중...",
                                "사용자의 일상적인 걷기 가속도 패턴을 분석하고 있습니다.",
                                0.25f
                            )
                            "FALL_DESCENT" -> Triple(
                                "2단계: 급격한 자유 낙하 발생!",
                                "공중 회전 및 무중력 하강 현상을 감지하고 있습니다.",
                                0.50f
                            )
                            "IMPACT" -> Triple(
                                "3단계: 지면 충격 충격량 수집",
                                "바닥 충격 수치가 계측되었습니다.",
                                0.75f
                            )
                            "IMMOBILITY" -> Triple(
                                "4단계: 충격 직후 정밀 부동 분석",
                                "의식 불명으로 인한 미동 상태를 분석하고 있습니다 (2.5초간 대기).",
                                0.95f
                            )
                            else -> Triple(
                                "가상 상태 초기화 중...",
                                "센서 시뮬레이션을 생성하고 있습니다.",
                                0.05f
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = phaseTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                // Pulse dot
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFEF4444), CircleShape)
                                )
                            }
                            Text(
                                text = phaseDesc,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                            LinearProgressIndicator(
                                progress = { progressValue },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.triggerSimulatedFall() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("simulate_fall_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Simulate")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("가상 충격 이벤트 발생시키기", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // 5. SYSTEM HEALTH & SENSOR CONNECTIVITY DASHBOARD (Moved to the very bottom as requested!)
        item {
            SystemHealthSection(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Modal Edit Contact dialog
    if (showEditDialog) {
        var tempName by remember { mutableStateOf(userName) }
        var tempGName by remember { mutableStateOf(guardianName) }
        var tempGPhone by remember { mutableStateOf(guardianPhone) }
        var tempEContact by remember { mutableStateOf(emergencyContact) }
        var tempETarget by remember { mutableStateOf(emergencyTarget) }

        var tempCustomContactError by remember { mutableStateOf(false) }

        val scrollState = rememberScrollState()

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("보호자 및 비상대처 정보 수정") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("사용자 이름") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tempGName,
                        onValueChange = { tempGName = it },
                        label = { Text("보호자 이름") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tempGPhone,
                        onValueChange = { tempGPhone = it },
                        label = { Text("보호자 전화번호") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tempEContact,
                        onValueChange = { 
                            tempEContact = it 
                            tempCustomContactError = false
                        },
                        label = { Text("비상 연락처 (선택)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = tempCustomContactError,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "긴급 신고 대상 선택",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val targets = listOf(
                        "119" to "🚨 119 소방본부",
                        "police" to "👮 경찰청 (112)",
                        "guardian" to "👤 등록 보호자",
                        "custom" to "📞 직접 입력하기"
                    )

                    targets.forEach { (value, label) ->
                        val isSelected = tempETarget == value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f)
                                    else Color.Transparent
                                )
                                .clickable { tempETarget = value }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { tempETarget = value }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        var hasError = false
                        if (tempName.trim().isEmpty() || tempGName.trim().isEmpty() || tempGPhone.trim().isEmpty()) {
                            hasError = true
                        }
                        if (tempETarget == "custom" && tempEContact.trim().isEmpty()) {
                            tempCustomContactError = true
                            hasError = true
                        }

                        if (!hasError) {
                            viewModel.saveSetup(tempName, tempGName, tempGPhone, tempEContact, tempETarget)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("저장")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun SettingsDetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

// Fullscreen Warning / Emergency Alert Popup overlay
@Composable
fun EmergencyOverlay(
    alertState: FallAlertState,
    isListeningVoice: Boolean,
    onDismiss: () -> Unit,
    onEmergency: () -> Unit,
    onCloseNoResponse: () -> Unit
) {
    val isNoResponseState = alertState.countdownSeconds <= -1

    Dialog(
        onDismissRequest = { /* Force response, do not allow dismiss by touching outside */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A) // Calming, deep premium dark background (Slate 900)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                if (isNoResponseState) {
                    // 1. NO RESPONSE / TIMEOUT STATE (Minimalist, reassuring charcoal design)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Scrollable Content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header Area
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AI 충격 감지 구조 시스템",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(100.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "시간 초과 자동 신고",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFCA5A5)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Large beautiful Glowing Warning Icon
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape)
                                    .border(2.dp, Color(0xFFEF4444), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "!",
                                    fontSize = 54.sp,
                                    fontWeight = FontWeight.Light,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "사용자 무응답 감지",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "30초 동안 움직임이 감지되지 않고 버튼 입력이 없어\n자동 비상 대응 조치를 기동했습니다.",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Modern list of actions triggered
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFF4CAF50), CircleShape)
                                        )
                                        Text(
                                            text = "보호자에게 실시간 긴급 구조 문자 발송 완료",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFF4CAF50), CircleShape)
                                        )
                                        Text(
                                            text = "GPS 위치 좌표 포함 공유 완료",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFFFFB74D), CircleShape)
                                        )
                                        Text(
                                            text = "119 소방 구급 전용 통화 대기 중",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Bottom Return Button
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onCloseNoResponse,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White, 
                                contentColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .testTag("no_response_close_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("상황 해제 및 메인으로", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(64.dp))
                    }
                } else {
                    // 2. ACTIVE COUNTDOWN STATE (Minimalist alarm with glowing timer, stress-free UX)
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_overlay")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.97f,
                        targetValue = 1.03f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )
                    
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.1f,
                        targetValue = 0.35f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glow_alpha"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Scrollable Content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AI 충격 감지 경보",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF57C00).copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                                        .border(1.dp, Color(0xFFF57C00), RoundedCornerShape(100.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "긴급 대응 카운트다운",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB74D)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Title
                            Text(
                                text = "충격이 감지되었습니다",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                              )
                            Text(
                                text = "30초간 응답이 없으면 자동으로 긴급 신고가 개시됩니다.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Large Premium Circular Countdown Timer
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .scale(pulseScale),
                                contentAlignment = Alignment.Center
                            ) {
                                // Soft glowing aura behind the timer
                                Box(
                                    modifier = Modifier
                                        .size(190.dp)
                                        .background(
                                            color = Color(0xFFEF4444).copy(alpha = glowAlpha),
                                            shape = CircleShape
                                        )
                                )
                                
                                // Concentric rings
                                Box(
                                    modifier = Modifier
                                        .size(170.dp)
                                        .background(Color.White.copy(alpha = 0.03f), CircleShape)
                                        .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                )
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${alertState.countdownSeconds}",
                                        fontSize = 72.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        lineHeight = 72.sp
                                    )
                                    Text(
                                        text = "자동 신고 전 남은 시간",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Telemetry summary (Clean & Subtle)
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "AI 정밀 상태 분석",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB74D)
                                    )
                                    Text(
                                        text = alertState.aiReason,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Voice Listener Status
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .background(
                                        color = if (isListeningVoice) Color(0xFF10B981).copy(alpha = 0.15f) 
                                                else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(100.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isListeningVoice) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(100.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("voice_cancel_status")
                            ) {
                                val pulseVoiceAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.4f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(600),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulse_voice"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .scale(pulseVoiceAlpha)
                                        .background(
                                            color = if (isListeningVoice) Color(0xFF10B981) else Color.White.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                )
                                Text(
                                    text = if (isListeningVoice) "음성 인식 활성화 - \"취소\" 라고 말하면 경보가 종료됩니다" 
                                           else "음성 대기 상태",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isListeningVoice) Color(0xFF34D399) else Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Stacked high-contrast buttons with large touch targets (64dp height)
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Action 1: CALL 119 IMMEDIATELY
                            Button(
                                onClick = onEmergency,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444), // Crimson Red
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .testTag("trigger_emergency_button"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("지금 즉시 119 구조 요청", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            }

                            // Action 2: I AM OK (CANCEL)
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.1f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .testTag("dismiss_alert_button"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                 Text("괜찮습니다, 경보 취소", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(64.dp))
                    }
                }
            }
        }
    }
}

// Simulation overlays for SMS sending and Dialing
@Composable
fun SmsSimulationDialog(logText: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("보호자 긴급 문자 모의 전송 완료")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "프로토타입 환경이므로 실제 SMS 패킷 요금 청구 대신 비상 긴급 무선 문자가 아래 내용으로 모의 전송되었습니다.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = logText,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.testTag("sms_sim_ok_button")) {
                Text("확인")
            }
        }
    )
}

@Composable
fun DialerSimulationOverlay(
    targetLabel: String,
    targetNumber: String,
    onEndCall: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* Block exit, must end call explicitly */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A) // Beautiful Slate Dark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Scrollable content area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
     
                    // Caller Animation Center
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse_dialer")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.92f,
                            targetValue = 1.08f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale_dialer"
                        )
                        
                        val glowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.05f,
                            targetValue = 0.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "glow_dialer"
                        )
     
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .scale(scale),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .background(Color(0xFFEF4444).copy(alpha = glowAlpha), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFFEF4444), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Calling icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                 )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            text = targetLabel,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$targetNumber 연결하는 중입니다...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
     
                    // Premium informative notes
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "긴급 신고 상태 안내",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB74D)
                            )
                            Text(
                                text = "본 화면은 프로토타입 환경으로, 지정하신 대상($targetLabel - $targetNumber)으로의 통화 연결 전 과정을 시뮬레이션하고 있습니다.\n\n확인이 완료되면 아래 종료 버튼을 눌러주십시오.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
     
                // End call circular button
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFFEF4444), CircleShape)
                            .testTag("end_sim_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "통화 종료",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// Simple Circular pulse animation element
@Composable
fun CircularPulsingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_circle")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(alpha)
            .clip(CircleShape)
            .background(Color.Red.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.Red)
        )
    }
}

@Composable
fun RechartsStyleGForceChart(
    history: List<Float>,
    threshold: Float,
    peakGForce: Float,
    onResetPeak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with legend and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "실시간 충격량 그래프",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "신체 가속 충격을 감도 임계값과 실시간 비교 분석합니다.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Clear / Reset Peak action - Circular M3 shape refresh icon button to avoid text truncation "피"
                IconButton(
                    onClick = onResetPeak,
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .testTag("reset_chart_peak_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "피크 수치 초기화",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Statistics indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current G
                val currentG = if (history.isNotEmpty()) history.last() / 9.8f else 1.0f
                val currentMS = if (history.isNotEmpty()) history.last() else 9.8f
                
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("실시간 충격량", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${String.format("%.2f", currentG)} G", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text("${String.format("%.1f", currentMS)} m/s²", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                // Peak G
                val peakG = peakGForce / 9.8f
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("세션 최대 충격", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${String.format("%.2f", peakG)} G", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                        Text("${String.format("%.1f", peakGForce)} m/s²", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                // Threshold G
                val threshG = threshold / 9.8f
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("설정된 감도선", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${String.format("%.2f", threshG)} G", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                        Text("${String.format("%.1f", threshold)} m/s²", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Recharts-Style Chart Drawing Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(surfaceColor, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "기기를 가볍게 흔들거나 아래 시뮬레이션을 작동하여\nG-Force 실시간 데이터 변화를 확인해 보세요.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        
                        // Reserve paddings for labels
                        val paddingLeft = 35.dp.toPx()
                        val paddingBottom = 20.dp.toPx()
                        val paddingTop = 10.dp.toPx()
                        val paddingRight = 10.dp.toPx()
                        
                        val chartWidth = width - paddingLeft - paddingRight
                        val chartHeight = height - paddingTop - paddingBottom
                        
                        // Maximum value on Y-axis is 50.0 m/s² (approx 5.1 G)
                        val maxVal = 50.0f 
                        
                        // 1. Draw thin background grids (horizontal & vertical)
                        val gridLines = 5
                        
                        // Horizontal grid lines & Y labels (G-force units)
                        for (i in 0..gridLines) {
                            val yVal = maxVal * i / gridLines
                            val yPos = height - paddingBottom - (yVal / maxVal) * chartHeight
                            
                            // Horizontal grid line
                            drawLine(
                                color = outlineColor.copy(alpha = 0.1f),
                                start = Offset(paddingLeft, yPos),
                                end = Offset(width - paddingRight, yPos),
                                strokeWidth = 1.dp.toPx()
                            )
                            
                            // Draw Y labels (G values)
                            val gVal = yVal / 9.8f
                            val labelText = if (gVal == 0f) "0G" else "${String.format("%.1f", gVal)}G"
                            
                            val paint = android.graphics.Paint().apply {
                                color = onSurfaceColor.copy(alpha = 0.6f).toArgb()
                                textSize = 9.sp.toPx()
                                textAlign = android.graphics.Paint.Align.LEFT
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                labelText,
                                5.dp.toPx(),
                                yPos + 4.dp.toPx(),
                                paint
                            )
                        }
                        
                        // Draw vertical grid lines (Time indices)
                        val verticalGridLines = 6
                        for (i in 0..verticalGridLines) {
                            val xPos = paddingLeft + i * (chartWidth / verticalGridLines)
                            drawLine(
                                color = outlineColor.copy(alpha = 0.08f),
                                start = Offset(xPos, paddingTop),
                                end = Offset(xPos, height - paddingBottom),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        
                        // Draw X Axis label
                        val xAxisPaint = android.graphics.Paint().apply {
                            color = onSurfaceColor.copy(alpha = 0.5f).toArgb()
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            "가속도 변화 트렌드 (최근 60회 샘플)",
                            paddingLeft + chartWidth / 2f,
                            height - 4.dp.toPx(),
                            xAxisPaint
                        )
                        
                        // 2. Draw Area Gradient under the trend line (Recharts AreaChart signature look)
                        val trendPoints = history.takeLast(60)
                        if (trendPoints.isNotEmpty()) {
                            val areaPath = Path()
                            val linePath = Path()
                            
                            trendPoints.forEachIndexed { index, value ->
                                val x = paddingLeft + index * (chartWidth / 59f)
                                val clampedVal = value.coerceAtMost(maxVal)
                                val y = height - paddingBottom - (clampedVal / maxVal) * chartHeight
                                
                                if (index == 0) {
                                    linePath.moveTo(x, y)
                                    areaPath.moveTo(x, height - paddingBottom)
                                    areaPath.lineTo(x, y)
                                } else {
                                    linePath.lineTo(x, y)
                                    areaPath.lineTo(x, y)
                                }
                                
                                if (index == trendPoints.size - 1) {
                                    areaPath.lineTo(x, height - paddingBottom)
                                    areaPath.close()
                                }
                            }
                            
                            // Draw filled area with soft gradient
                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.35f),
                                        primaryColor.copy(alpha = 0.02f)
                                    ),
                                    startY = paddingTop,
                                    endY = height - paddingBottom
                                )
                            )
                            
                            // Draw smooth trend line
                            drawPath(
                                path = linePath,
                                color = primaryColor,
                                style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round)
                            )
                        }
                        
                        // 3. Draw horizontal Calibration Threshold line (Current sensitivity threshold)
                        val threshY = (height - paddingBottom - (threshold.coerceIn(0f, maxVal) / maxVal) * chartHeight)
                        drawLine(
                            color = errorColor,
                            start = Offset(paddingLeft, threshY),
                            end = Offset(width - paddingRight, threshY),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                        
                        // Threshold horizontal tag
                        val threshPaint = android.graphics.Paint().apply {
                            color = errorColor.toArgb()
                            textSize = 9.sp.toPx()
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            "감도 임계선 (${String.format("%.1f", threshold / 9.8f)}G)",
                            width - paddingRight - 4.dp.toPx(),
                            threshY - 6.dp.toPx(),
                            threshPaint
                        )
                        
                        // 4. Highlight points that exceed the threshold
                        trendPoints.forEachIndexed { index, value ->
                            if (value > threshold) {
                                val x = paddingLeft + index * (chartWidth / 59f)
                                val clampedVal = value.coerceAtMost(maxVal)
                                val y = height - paddingBottom - (clampedVal / maxVal) * chartHeight
                                
                                // Draw double-ring glow circle
                                drawCircle(
                                    color = errorColor.copy(alpha = 0.3f),
                                    radius = 7.dp.toPx(),
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = errorColor,
                                    radius = 3.5.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                }
            }
            
            // Calibration helper tip
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "도움말",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "일상 보행/움직임은 보통 1.5G 미만으로 감도 임계선을 넘지 않습니다. 휴대폰을 가볍게 치거나 던지는 시뮬레이션을 통해 임계선을 돌파하는 적색 경보 마커가 바르게 계측되는지 점검하세요.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun BatteryIndicator(
    level: Int,
    isCharging: Boolean,
    onSurfaceColor: Color,
    modifier: Modifier = Modifier
) {
    val batteryColor = when {
        isCharging -> Color(0xFF4CAF50)
        level < 20 -> Color(0xFFF44336)
        level < 50 -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        if (isCharging) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "충전 중",
                tint = batteryColor,
                modifier = Modifier.size(12.dp)
            )
        }
        
        Text(
            text = "$level%",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Canvas(modifier = Modifier.size(width = 22.dp, height = 12.dp)) {
            val width = size.width
            val height = size.height
            val strokeWidth = 1.5.dp.toPx()
            val capWidth = 2.dp.toPx()
            
            val bodyWidth = width - capWidth - strokeWidth
            drawRoundRect(
                color = onSurfaceColor.copy(alpha = 0.4f),
                topLeft = Offset(0f, 0f),
                size = Size(bodyWidth, height),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )
            
            drawRoundRect(
                color = onSurfaceColor.copy(alpha = 0.4f),
                topLeft = Offset(bodyWidth + strokeWidth / 2, height * 0.25f),
                size = Size(capWidth, height * 0.5f),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
            
            val innerPadding = strokeWidth + 1.dp.toPx()
            val maxInnerWidth = bodyWidth - (innerPadding * 2)
            val fillWidth = maxInnerWidth * (level / 100f)
            val innerHeight = height - (innerPadding * 2)
            
            if (fillWidth > 0) {
                drawRoundRect(
                    color = batteryColor,
                    topLeft = Offset(innerPadding, innerPadding),
                    size = Size(fillWidth, innerHeight),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun AppHeaderBar(viewModel: FallViewModel) {
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    
    val isLowBattery = batteryLevel < 20 && !isBatteryCharging
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (isLowBattery) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                            shape = CircleShape
                        )
                )
                Text(
                    text = if (isLowBattery) "경고: 배터리 부족 (중단 우려)" else "실시간 안전 보호 활성화됨",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLowBattery) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }

            BatteryIndicator(
                level = batteryLevel,
                isCharging = isBatteryCharging,
                onSurfaceColor = MaterialTheme.colorScheme.onSurface
            )
        }
        
        if (isLowBattery) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "경고",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "배터리가 부족합니다! 안정적인 낙상 감지를 위해 기기를 충전기에 연결해 주세요.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

@Composable
fun DetectionHistorySection(
    impactEvents: List<ImpactEvent>,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "감지 기록",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "충격 감지 분석 이력",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (impactEvents.isNotEmpty()) {
                    TextButton(
                        onClick = onClearHistory,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp).testTag("clear_impact_events_button")
                    ) {
                        Text("이력 비우기", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (impactEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "아직 감지된 충격 이벤트가 없습니다.\n임계값 이상의 충격이 발생하면 이곳에 실시간 기록됩니다.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    impactEvents.take(10).forEach { event ->
                        val gForceVal = event.peakGForce / 9.8f
                        val isExceeded = event.status.contains("기준돌파")
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isExceeded) {
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = sdf.format(Date(event.timestamp)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                                shape = CircleShape
                                            )
                                    )
                                    Text(
                                        text = event.status,
                                        fontSize = 10.sp,
                                        color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "${String.format("%.2f", gForceVal)} G",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${String.format("%.1f", event.peakGForce)} m/s²",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    if (impactEvents.size > 10) {
                        Text(
                            text = "최근 10개의 충격 감지 기록만 표시 중입니다. (총 ${impactEvents.size}개)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GuidedCalibrationWizard(
    isCalibrating: Boolean,
    calibrationStep: Int,
    baselinePeak: Float,
    impactPeak: Float,
    countdown: Int,
    currentThreshold: Float,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onNextStep: (Int) -> Unit,
    onApply: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isCalibrating) 2.dp else 1.5.dp,
                color = if (isCalibrating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCalibrating) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (!isCalibrating) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "감도 진단",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "스마트폰 충격 감도 정밀 보정 가이드",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "내 스마트폰의 가속도 센서와 실생활 진동 데이터를 측정하여 이상적인 충격 감도를 맞춤 설정합니다. 낙상 오탐을 현격히 낮추는 최고의 방법입니다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("start_calibration_wizard_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("맞춤 감도 진단 가이드 시작", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Calibration",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "센서 맞춤형 자동 보정 중",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Text(
                        text = "단계 $calibrationStep / 4",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                when (calibrationStep) {
                    1 -> {
                        Text(
                            text = "🧭 맞춤 보정의 원리 안내",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "1. 평소 스마트폰을 들고 생활할 때 발생하는 움직임(Baseline Noise)을 먼저 계측합니다.\n\n" +
                                   "2. 이후 안전한 환경에서 가벼운 충격을 주어 낙상 시 발생하는 최저 임계 신호 크기를 가늠합니다.\n\n" +
                                   "3. 두 측정 데이터를 분석하여 오동작 없는 안전한 최적의 감도를 제공합니다.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier.weight(1f).testTag("calibration_cancel_step_1")
                            ) {
                                Text("취소", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { onNextStep(2) },
                                modifier = Modifier.weight(1.5f).testTag("calibration_next_step_1")
                            ) {
                                Text("가이드 시작하기", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    2 -> {
                        Text(
                            text = "🚶 1단계: 일상 움직임 노이즈 계측",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "휴대폰을 주머니에 넣거나 손에 쥐고 가볍게 걷거나 평소 일상 동작을 수행해 주세요.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "측정 남은 시간",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$countdown 초",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "최고 노이즈 세기",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${String.format("%.2f", baselinePeak / 9.8f)} G",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${String.format("%.1f", baselinePeak)} m/s²",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth().testTag("calibration_cancel_step_2")
                        ) {
                            Text("진단 취소", fontSize = 12.sp)
                        }
                    }
                    3 -> {
                        Text(
                            text = "💥 2단계: 모의 충격 계측",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "스마트폰을 손에 쥐고 손바닥을 툭 치거나, 소파/침대 등의 푹신한 표면에 가볍게 떨어뜨려 가상의 낙상 임팩트를 가해보세요.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "대기 및 측정",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$countdown 초",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "기록된 최고 충격",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${String.format("%.2f", impactPeak / 9.8f)} G",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "${String.format("%.1f", impactPeak)} m/s²",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth().testTag("calibration_cancel_step_3")
                        ) {
                            Text("진단 취소", fontSize = 12.sp)
                        }
                    }
                    4 -> {
                        Text(
                            text = "🎉 맞춤 감도 진단 분석 완료",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "두 데이터를 비교 분석하여 스마트폰 기기에 최적화된 충격 안전 임계값을 추출하였습니다.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("일상 최대 자극", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${String.format("%.2f", baselinePeak / 9.8f)} G", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${String.format("%.1f", baselinePeak)} m/s²", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("모의 가상 충격", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${String.format("%.2f", impactPeak / 9.8f)} G", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Text("${String.format("%.1f", impactPeak)} m/s²", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val calculatedRecommendation = if (baselinePeak > 0f && impactPeak > baselinePeak) {
                            val mid = baselinePeak + (impactPeak - baselinePeak) * 0.45f
                            mid.coerceIn(15.0f, 35.0f)
                        } else {
                            25.0f
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "💡 내 기기 맞춤 추천 임계값",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${String.format("%.1f", calculatedRecommendation)} m/s²  (${String.format("%.2f", calculatedRecommendation / 9.8f)} G)",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "이 값은 일상 움직임(${String.format("%.2f", baselinePeak / 9.8f)}G)에는 울리지 않으며, 낙상 충격(${String.format("%.2f", impactPeak / 9.8f)}G)에만 안전하게 트리거되도록 마진을 고려한 최상의 설정입니다.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onStart,
                                modifier = Modifier.weight(1f).testTag("recalibrate_button")
                            ) {
                                Text("재측정", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { onApply(calculatedRecommendation) },
                                modifier = Modifier.weight(1.5f).testTag("apply_calibrated_threshold_button")
                            ) {
                                Text("설정값 적용 및 저장", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemHealthSection(
    viewModel: FallViewModel,
    modifier: Modifier = Modifier
) {
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    val sensorStatusDetail by viewModel.sensorStatusMessage.collectAsState()
    val liveLat by viewModel.currentLatitude.collectAsState()
    val liveLng by viewModel.currentLongitude.collectAsState()
    val isListeningVoiceActive by viewModel.isListeningVoice.collectAsState()
    val context = LocalContext.current

    // Calculate a progress value for connectivity:
    // 1.0f (100%) for active monitoring
    // 0.7f (70%) for simulation mode (e.g. sensor unavailable, running in emulator/virtual mode)
    // 0.0f (0%) for stopped or idle
    val connectivityProgress = when {
        sensorStatusDetail.contains("감지 중") -> 1.0f
        sensorStatusDetail.contains("정상") -> 1.0f
        sensorStatusDetail.contains("시뮬레이션") -> 0.7f
        sensorStatusDetail.contains("중단") || sensorStatusDetail.contains("대기") -> 0.0f
        else -> 0.5f
    }

    val connectivityColor = when {
        connectivityProgress >= 0.9f -> Color(0xFF4CAF50) // Healthy Green
        connectivityProgress >= 0.5f -> Color(0xFF2196F3) // Simulated Blue
        else -> Color(0xFFF44336) // Unconnected/Inactive Red
    }

    val batteryColor = when {
        isBatteryCharging -> Color(0xFF4CAF50)
        batteryLevel < 20 -> Color(0xFFF44336)
        batteryLevel < 50 -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("system_health_dashboard_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "System Health",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "실시간 시스템 및 센서 진단",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "배터리 수준 및 물리 센서 연결 안전성 통합 진단",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Status Pill
                val systemStatusLabel = when {
                    batteryLevel < 20 && !isBatteryCharging -> "주의 (배터리 부족)"
                    connectivityProgress == 0.0f -> "센서 대기"
                    connectivityProgress == 0.7f -> "점검 (모의 작동)"
                    else -> "정상 작동"
                }
                val systemStatusBgColor = when {
                    batteryLevel < 20 && !isBatteryCharging -> Color(0xFFFFEBEE)
                    connectivityProgress == 0.0f -> Color(0xFFFFF3E0)
                    connectivityProgress == 0.7f -> Color(0xFFE3F2FD)
                    else -> Color(0xFFE8F5E9)
                }
                val systemStatusTextColor = when {
                    batteryLevel < 20 && !isBatteryCharging -> Color(0xFFC62828)
                    connectivityProgress == 0.0f -> Color(0xFFE65100)
                    connectivityProgress == 0.7f -> Color(0xFF1565C0)
                    else -> Color(0xFF2E7D32)
                }

                Box(
                    modifier = Modifier
                        .background(systemStatusBgColor, RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = systemStatusLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = systemStatusTextColor
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 1. DEVICE BATTERY LEVEL PROGRESS BAR
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed, // Using Speed icon as battery load/usage gauge
                            contentDescription = "Battery Status",
                            tint = batteryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "기기 배터리 잔량",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isBatteryCharging) {
                            Text(
                                text = "[충전 중]",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Text(
                            text = "$batteryLevel%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = batteryColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Premium Progressive Gauge Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    LinearProgressIndicator(
                        progress = { batteryLevel.toFloat() / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = batteryColor,
                        trackColor = Color.Transparent
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. REAL-TIME SENSOR CONNECTIVITY STATUS PROGRESS BAR
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh, // Using Refresh for continuous loop/connection sync
                            contentDescription = "Sensor Stream",
                            tint = connectivityColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "실시간 센서 스트리밍 연결도",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Text(
                        text = "${(connectivityProgress * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = connectivityColor,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Connectivity Progress Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    LinearProgressIndicator(
                        progress = { connectivityProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = connectivityColor,
                        trackColor = Color.Transparent
                    )
                }
                
                // Individual real-time sensor connection & status panel
                val hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED || androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                val accelState = when {
                    sensorStatusDetail.contains("정상 작동") -> "STABLE"
                    sensorStatusDetail.contains("시뮬레이션") -> "WEAK"
                    else -> "DISCONNECTED"
                }

                val gyroState = when {
                    sensorStatusDetail.contains("정상 작동") -> "STABLE"
                    sensorStatusDetail.contains("시뮬레이션") -> "WEAK"
                    else -> "DISCONNECTED"
                }

                val gpsState = when {
                    hasLocationPermission && (liveLat != 37.5665 || liveLng != 126.9780) -> "STABLE"
                    hasLocationPermission -> "WEAK"
                    else -> "DISCONNECTED"
                }

                val micState = when {
                    hasMicPermission && isListeningVoiceActive -> "STABLE"
                    hasMicPermission -> "WEAK"
                    else -> "DISCONNECTED"
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "실시간 개별 센서 연결 분석 (Color-Coded Diagnostics)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SensorStatusCard(
                            sensorName = "가속도 센서",
                            sensorIcon = Icons.Default.Speed,
                            statusState = accelState,
                            modifier = Modifier.weight(1f)
                        )
                        SensorStatusCard(
                            sensorName = "자이로 센서",
                            sensorIcon = Icons.Default.Refresh,
                            statusState = gyroState,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SensorStatusCard(
                            sensorName = "GPS 위성 센서",
                            sensorIcon = Icons.Default.LocationOn,
                            statusState = gpsState,
                            modifier = Modifier.weight(1f)
                        )
                        SensorStatusCard(
                            sensorName = "음성인식 마이크",
                            sensorIcon = Icons.Default.NotificationsActive,
                            statusState = micState,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StylizedVectorMap(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
    address: String = ""
) {
    var isMapLoading by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Real interactive OpenStreetMap via Leaflet for secure, 100% error-free rendering without any API keys
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                return false
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isMapLoading = false
                            }
                        }
                        webChromeClient = android.webkit.WebChromeClient()
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                        
                        val mapHtml = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                                <style>
                                    html, body, #map {
                                        margin: 0; padding: 0; height: 100%; width: 100%; background: #eaeaea; overflow: hidden;
                                    }
                                    /* Modern, flat controls */
                                    .leaflet-bar {
                                        border: none !important;
                                        box-shadow: 0 2px 8px rgba(0,0,0,0.15) !important;
                                    }
                                    .leaflet-bar a {
                                        background-color: #ffffff !important;
                                        color: #333333 !important;
                                        border-bottom: 1px solid #f0f0f0 !important;
                                    }
                                </style>
                            </head>
                            <body>
                                <div id="map"></div>
                                <script>
                                    // Disable default zoom controls to prevent overlap. Pinch-to-zoom is natively supported on mobile.
                                    var map = L.map('map', {
                                        zoomControl: false,
                                        attributionControl: false
                                    }).setView([$latitude, $longitude], 16);
                                    
                                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                        maxZoom: 19
                                    }).addTo(map);

                                    // Self-contained custom vector marker - ensures perfect loading without requiring external image assets
                                    var customIcon = L.divIcon({
                                        html: '<div style="background-color: #f44336; width: 18px; height: 18px; border-radius: 50%; border: 3px solid white; box-shadow: 0 2px 6px rgba(0,0,0,0.4);"></div>',
                                        className: 'custom-pin',
                                        iconSize: [18, 18],
                                        iconAnchor: [9, 9]
                                    });

                                    L.marker([$latitude, $longitude], { icon: customIcon }).addTo(map);
                                    
                                    // Accuracy circle
                                    L.circle([$latitude, $longitude], {
                                        color: '#2196F3',
                                        fillColor: '#2196F3',
                                        fillOpacity: 0.12,
                                        radius: 60
                                    }).addTo(map);
                                </script>
                            </body>
                            </html>
                        """.trimIndent()
                        loadDataWithBaseURL("https://openstreetmap.org", mapHtml, "text/html", "utf-8", null)
                    }
                },
                update = { webView ->
                    val mapHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                            <style>
                                html, body, #map {
                                    margin: 0; padding: 0; height: 100%; width: 100%; background: #eaeaea; overflow: hidden;
                                }
                                .leaflet-bar {
                                    border: none !important;
                                    box-shadow: 0 2px 8px rgba(0,0,0,0.15) !important;
                                }
                                .leaflet-bar a {
                                    background-color: #ffffff !important;
                                    color: #333333 !important;
                                    border-bottom: 1px solid #f0f0f0 !important;
                                }
                            </style>
                        </head>
                        <body>
                            <div id="map"></div>
                            <script>
                                var map = L.map('map', {
                                    zoomControl: false,
                                    attributionControl: false
                                }).setView([$latitude, $longitude], 16);
                                
                                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                    maxZoom: 19
                                }).addTo(map);

                                var customIcon = L.divIcon({
                                    html: '<div style="background-color: #f44336; width: 18px; height: 18px; border-radius: 50%; border: 3px solid white; box-shadow: 0 2px 6px rgba(0,0,0,0.4);"></div>',
                                    className: 'custom-pin',
                                    iconSize: [18, 18],
                                    iconAnchor: [9, 9]
                                });

                                L.marker([$latitude, $longitude], { icon: customIcon }).addTo(map);
                                
                                L.circle([$latitude, $longitude], {
                                    color: '#2196F3',
                                    fillColor: '#2196F3',
                                    fillOpacity: 0.12,
                                    radius: 60
                                }).addTo(map);
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL("https://openstreetmap.org", mapHtml, "text/html", "utf-8", null)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Sleek loading indicator over the WebView
            if (isMapLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                }
            }

            // Map Overlays / Information
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "실시간 위치 정보 (지도)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                val addressText = address.ifEmpty {
                    if (Math.abs(latitude - 37.5559) < 0.2) {
                        "서울특별시 중구 서울역 근처"
                    } else {
                        "서울특별시 종로구 세종대로"
                    }
                }
                Text(
                    text = addressText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

