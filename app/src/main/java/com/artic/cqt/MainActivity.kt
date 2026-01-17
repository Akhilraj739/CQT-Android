package com.artic.cqt

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.artic.cqt.ui.components.InfoMessage
import com.artic.cqt.ui.components.StatusCard
import com.artic.cqt.ui.theme.CQTTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CQTTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var selectedTile by remember { mutableStateOf<Int?>(null) }
    var showBridgeWhitelist by remember { mutableStateOf(false) }
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var isPowerUserEnabled by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        while(true) {
            isAccessibilityEnabled = CQTAccessibilityService.isConnected.value
            isPowerUserEnabled = (try { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED } catch (_: Exception) { false }) || RootHelper.isRootAvailable()
            delay(2000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CQT", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = { showBridgeWhitelist = true }) {
                        Icon(Icons.Default.Security, contentDescription = "Bridge Whitelist")
                    }
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "About")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (selectedTile != null) {
                TileConfigurationScreen(
                    tileId = selectedTile!!,
                    isPowerUserEnabled = isPowerUserEnabled,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    onBack = { selectedTile = null }
                )
            } else if (showBridgeWhitelist) {
                BridgeWhitelistScreen(onBack = { showBridgeWhitelist = false })
            } else {
                HomeScreen(
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    isPowerUserEnabled = isPowerUserEnabled,
                    onTileClick = { selectedTile = it }
                )
            }
        }

        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }
    }
}

@Composable
fun HomeScreen(
    isAccessibilityEnabled: Boolean,
    isPowerUserEnabled: Boolean,
    onTileClick: (Int) -> Unit
) {
    val context = LocalContext.current
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { GridItemSpan(4) }) {
            Text("System Status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        
        item(span = { GridItemSpan(4) }) {
            StatusCard(
                title = "Accessibility Service",
                status = if (isAccessibilityEnabled) "Running" else "Disabled",
                isEnabled = isAccessibilityEnabled,
                icon = Icons.Default.Accessibility,
                actionLabel = "Enable",
                onAction = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )
        }

        item(span = { GridItemSpan(4) }) {
            StatusCard(
                title = "Power User (Root/Shizuku)",
                status = if (isPowerUserEnabled) "Granted" else "Missing",
                isEnabled = isPowerUserEnabled,
                icon = Icons.Default.Bolt,
                actionLabel = "Help",
                onAction = { /* Show Root Help */ }
            )
        }

        item(span = { GridItemSpan(4) }) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item(span = { GridItemSpan(4) }) {
            Text("Quick Setting Slots", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        items((1..20).toList()) { i ->
            TileSlotItem(i, onTileClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BridgeWhitelistScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val bridgeSettings = remember { BridgeSettings(context) }
    var whitelistedApps by remember { mutableStateOf(bridgeSettings.getWhitelistedApps()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bridge Whitelist") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text(
                "Apps in this list are allowed to modify your tiles automatically via the CQT Bridge.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (whitelistedApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No apps whitelisted yet.\nThey will appear here once you allow them.", textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn {
                    items(whitelistedApps) { pkg ->
                        WhitelistedAppItem(
                            packageName = pkg,
                            onRemove = {
                                bridgeSettings.setWhitelisted(pkg, false)
                                whitelistedApps = bridgeSettings.getWhitelistedApps()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WhitelistedAppItem(packageName: String, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(packageName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun TileSlotItem(id: Int, onClick: (Int) -> Unit) {
    val context = LocalContext.current
    val prefs = remember(id) { TilePreferences(context, id) }
    
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick(id) }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (prefs.iconType == "GALLERY" && prefs.iconValue.isNotEmpty()) {
                AsyncImage(
                    model = prefs.iconValue,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    contentScale = ContentScale.Fit
                )
            } else if (prefs.iconType == "BUILTIN" && prefs.iconValue.isNotEmpty()) {
                val resId = TilePreferences.getResIdByName(prefs.iconValue)
                if (resId != 0) {
                    Icon(
                        painter = painterResource(id = resId),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(Icons.Default.Error, contentDescription = null)
                }
            } else if (prefs.tileActionType != TilePreferences.ACTION_NONE) {
                val builtinIcon = TilePreferences.getBuiltinIconRes(prefs.tileActionType)
                Icon(
                    painter = painterResource(id = builtinIcon),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    id.toString(),
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Text(
                    id.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            prefs.tileLabel,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Text("About CQT", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "CQT: Custom Quick Tiles provide 20 fully customizable slots. " +
                    "Bridge the gap between hidden system menus and your notification shade with ease.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val activities: List<String> = emptyList()
)

@Composable
fun AppPickerDialog(
    onDismiss: () -> Unit,
    onAppSelected: (String) -> Unit,
    showActivities: Boolean = false
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showSystemApps by remember { mutableStateOf(false) }

    LaunchedEffect(showSystemApps) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val filteredApps = if (showSystemApps) {
                installedApps
            } else {
                installedApps.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 }
            }
            
            apps = filteredApps.map { appInfo ->
                AppInfo(
                    appInfo.loadLabel(pm).toString(),
                    appInfo.packageName,
                    appInfo.loadIcon(pm)
                )
            }.sortedBy { it.label }
        }
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (selectedApp == null) "Select App" else "Select Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    
                    if (selectedApp == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("System", style = MaterialTheme.typography.labelSmall)
                            Checkbox(checked = showSystemApps, onCheckedChange = { showSystemApps = it })
                        }
                    }
                }
                
                if (selectedApp == null) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        placeholder = { Text("Search apps...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = CircleShape
                    )
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val filteredApps = apps.filter { it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
                    
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        if (selectedApp == null) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        if (showActivities) {
                                            try {
                                                val packageInfo = pm.getPackageInfo(app.packageName, PackageManager.GET_ACTIVITIES)
                                                val activities = packageInfo.activities?.map { it.name } ?: emptyList()
                                                selectedApp = AppInfo(app.label, app.packageName, app.icon, activities)
                                            } catch (e: Exception) {
                                                selectedApp = AppInfo(app.label, app.packageName, app.icon, emptyList())
                                            }
                                        } else {
                                            onAppSelected(app.packageName)
                                        }
                                    }.padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(bitmap = app.icon.toBitmap().asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(app.label, fontWeight = FontWeight.Medium)
                                        Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        } else {
                            item {
                                TextButton(onClick = { selectedApp = null }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Back to apps")
                                }
                            }
                            if (selectedApp!!.activities.isEmpty()) {
                                item {
                                    Text("No activities found for this app.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                                }
                            }
                            items(selectedApp!!.activities) { activity ->
                                val shortName = activity.substringAfterLast(".")
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onAppSelected("${selectedApp!!.packageName}/$activity") }.padding(12.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(shortName, fontWeight = FontWeight.Medium)
                                        Text(activity, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun BuiltinIconPickerDialog(
    onDismiss: () -> Unit,
    onIconSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Icon", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(TilePreferences.builtinIcons, key = { it.first }) { (name, resId) ->
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onIconSelected(name) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = resId),
                                contentDescription = name,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileConfigurationScreen(
    tileId: Int, 
    isPowerUserEnabled: Boolean,
    isAccessibilityEnabled: Boolean,
    onBack: () -> Unit, 
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(tileId) { TilePreferences(context, tileId) }

    var label by remember { mutableStateOf(prefs.tileLabel) }
    var subtitle by remember { mutableStateOf(prefs.tileSubtitle) }
    var actionType by remember { mutableStateOf(prefs.tileActionType) }
    var actionValue by remember { mutableStateOf(prefs.tileActionValue) }
    var iconType by remember { mutableStateOf(prefs.iconType) }
    var iconValue by remember { mutableStateOf(prefs.iconValue) }
    var useAnimation by remember { mutableStateOf(prefs.useAnimation) }
    
    var showAppPicker by remember { mutableStateOf(false) }
    var appPickerShowActivities by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showCustomIconInfo by remember { mutableStateOf(false) }

    val filteredActionOptions = remember(isPowerUserEnabled, isAccessibilityEnabled) {
        TilePreferences.actionOptions.filter { (type, _) ->
            when (type) {
                TilePreferences.ACTION_NONE, TilePreferences.ACTION_TOGGLE, TilePreferences.ACTION_OPEN_APP,
                TilePreferences.ACTION_OPEN_URL, TilePreferences.ACTION_FLASHLIGHT -> true
                TilePreferences.ACTION_SCREENSHOT, TilePreferences.ACTION_LOCK_SCREEN, TilePreferences.ACTION_POWER_DIALOG -> isPowerUserEnabled || isAccessibilityEnabled
                TilePreferences.ACTION_REBOOT, TilePreferences.ACTION_REBOOT_RECOVERY, TilePreferences.ACTION_SHELL_COMMAND,
                TilePreferences.ACTION_ADB_WIFI, TilePreferences.ACTION_KILL_APP, TilePreferences.ACTION_CLEAR_DATA,
                TilePreferences.ACTION_MOBILE_DATA, TilePreferences.ACTION_NFC -> isPowerUserEnabled
                else -> false
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                iconType = "GALLERY"
                iconValue = it.toString()
            }
        }
    )

    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = {
                actionValue = it
                showAppPicker = false
            },
            showActivities = appPickerShowActivities
        )
    }

    if (showIconPicker) {
        BuiltinIconPickerDialog(
            onDismiss = { showIconPicker = false },
            onIconSelected = {
                iconType = "BUILTIN"
                iconValue = it
                showIconPicker = false
            }
        )
    }

    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Button(
                    onClick = {
                        prefs.tileLabel = label
                        prefs.tileSubtitle = subtitle
                        prefs.iconType = iconType
                        prefs.iconValue = iconValue
                        prefs.tileActionType = actionType
                        prefs.tileActionValue = actionValue
                        prefs.useAnimation = useAnimation
                        
                        val className = "com.artic.cqt.QuickTileService$tileId"
                        val componentName = ComponentName(context, className)
                        android.service.quicksettings.TileService.requestListeningState(context, componentName)
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()
                ) {
                    Text("Save & Apply")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary) }
                Text("Configuration for Tile #$tileId", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = label, 
                onValueChange = { label = it }, 
                label = { Text("Tile Label") }, 
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = subtitle, 
                onValueChange = { subtitle = it }, 
                label = { Text("Tile Subtitle (Android 10+)") }, 
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Text("Action Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = TilePreferences.actionOptions.find { it.first == actionType }?.second ?: "None",
                    onValueChange = {}, readOnly = true, label = { Text("Action Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    filteredActionOptions.forEach { (type, name) ->
                        DropdownMenuItem(text = { Text(name) }, onClick = { actionType = type; expanded = false })
                    }
                }
            }

            if (actionType == TilePreferences.ACTION_OPEN_APP) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { appPickerShowActivities = false; showAppPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Select App")
                    }
                    Button(
                        onClick = { appPickerShowActivities = true; showAppPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Select Activity")
                    }
                }
            } else if (actionType == TilePreferences.ACTION_CLEAR_DATA || actionType == TilePreferences.ACTION_KILL_APP) {
                Button(onClick = { appPickerShowActivities = false; showAppPicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Apps, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (actionValue.isEmpty()) "Select Target App" else "Target: ${actionValue.substringAfterLast("/")}")
                }
            }

            if (actionType == TilePreferences.ACTION_OPEN_URL || actionType == TilePreferences.ACTION_SHELL_COMMAND || 
                actionType == TilePreferences.ACTION_KILL_APP || actionType == TilePreferences.ACTION_CLEAR_DATA || 
                actionType == TilePreferences.ACTION_OPEN_APP) {
                OutlinedTextField(
                    value = actionValue, onValueChange = { actionValue = it },
                    label = { 
                        Text(when (actionType) {
                            TilePreferences.ACTION_SHELL_COMMAND -> "Command"
                            TilePreferences.ACTION_KILL_APP -> "Package Name"
                            TilePreferences.ACTION_CLEAR_DATA -> "Package Name"
                            TilePreferences.ACTION_OPEN_APP -> "Package/Activity"
                            else -> "URL"
                        })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Icon Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = { showCustomIconInfo = !showCustomIconInfo }) {
                    Icon(Icons.Default.Info, contentDescription = "Custom Icon Info", tint = MaterialTheme.colorScheme.primary)
                }
            }
            
            if (showCustomIconInfo) {
                InfoMessage(
                    "Custom Icon Guide:\n" +
                    "• Format: PNG (recommended) or JPEG.\n" +
                    "• Size: 96x96 to 128x128 pixels is ideal.\n" +
                    "• Style: Use a transparent background for the best native look.\n" +
                    "• Permissions: If an icon disappears, move the image file to a standard folder like 'Pictures' and re-select it. System security sometimes revokes access to files in private or cloud folders."
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                            if (iconType == "GALLERY" && iconValue.isNotEmpty()) {
                                AsyncImage(model = iconValue, contentDescription = null, modifier = Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
                            } else if (iconType == "BUILTIN" && iconValue.isNotEmpty()) {
                                val resId = TilePreferences.getResIdByName(iconValue)
                                if (resId != 0) {
                                    Icon(painter = painterResource(id = resId), contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                val builtinIcon = TilePreferences.getBuiltinIconRes(actionType)
                                Icon(painter = painterResource(id = builtinIcon), contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showIconPicker = true }, modifier = Modifier.fillMaxWidth()) { Text("Select Default Icon") }
                            Button(onClick = { imagePickerLauncher.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Custom (Gallery)") }
                        }
                    }
                    
                    if (iconType == "GALLERY" && !showCustomIconInfo) {
                        InfoMessage(
                            "Using a custom gallery icon. Tap the info icon above for format and size guidelines."
                        )
                    }

                    if (iconType != "DEFAULT") {
                        TextButton(onClick = { iconType = "DEFAULT"; iconValue = "" }, modifier = Modifier.fillMaxWidth()) { Text("Reset to Default", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }

            Text("Other Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Use Animation", fontWeight = FontWeight.Bold)
                        Text("Show fade-in animation when tile is updated", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = useAnimation, onCheckedChange = { useAnimation = it })
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
