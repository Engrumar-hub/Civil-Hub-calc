package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.HistoryEntity
import com.example.data.HistoryRepository
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CalculatorViewModel
import com.example.viewmodel.CalculatorViewModelFactory
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Database & Repository
        val database = AppDatabase.getDatabase(this)
        val repository = HistoryRepository(database.historyDao())
        
        // Instantiate ViewModel
        val factory = CalculatorViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[CalculatorViewModel::class.java]

        setContent {
            MyApplicationTheme(darkTheme = viewModel.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: CalculatorViewModel) {
    val context = LocalContext.current
    var showInterstitial by remember { mutableStateOf(false) }
    var showRewardVideoAd by remember { mutableStateOf(false) }
    var calcCount by remember { mutableStateOf(0) }
    var isAdFreeSession by remember { mutableStateOf(false) }

    // Interstitial Trigger Manager
    val onCalculateTriggered: () -> Unit = {
        calcCount++
        if (!isAdFreeSession && calcCount % 3 == 0) {
            showInterstitial = true
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Construction,
                                    contentDescription = "Builder Icon",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(20.dp)
                                )
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Civil",
                                        fontWeight = FontWeight.Light,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Calc",
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (isAdFreeSession) {
                                        Surface(
                                            color = Color(0xFF4CAF50),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "PRO",
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "CivilCalc Pro • Estimator",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                    },
                    actions = {
                        // Watch video reward to unlock premium ad-free
                        IconButton(
                            onClick = {
                                if (isAdFreeSession) {
                                    Toast.makeText(context, "Pro features are active for this shift!", Toast.LENGTH_SHORT).show()
                                } else {
                                    showRewardVideoAd = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardMembership,
                                contentDescription = "Active Pro Toolkit",
                                tint = if (isAdFreeSession) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            )
                        }

                        // Global Metric vs Imperial conversion toggler
                        IconButton(
                            onClick = {
                                viewModel.toggleUnitSystem()
                                Toast.makeText(
                                    context,
                                    if (viewModel.isMetric) "System: Metric (m/cm/kg)" else "System: Imperial (ft/in/lbs)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.testTag("unit_toggle_button")
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (viewModel.isMetric) "METRIC" else "IMP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Theme Mode Selector
                        IconButton(
                            onClick = { viewModel.isDarkMode = !viewModel.isDarkMode },
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (viewModel.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Dark Mode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                if (!isAdFreeSession) {
                    AdMobPremiumBannerPlacement()
                } else {
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "👑 Professional Ad-Free Shift Active",
                                color = Color(0xFF4CAF50),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = viewModel.activeTab == "CALCULATOR",
                        onClick = { viewModel.activeTab = "CALCULATOR" },
                        icon = { Icon(imageVector = Icons.Default.Calculate, contentDescription = "Estimators") },
                        label = { Text("Calculator", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_calculator")
                    )
                    NavigationBarItem(
                        selected = viewModel.activeTab == "CONVERTER",
                        onClick = {
                            viewModel.activeTab = "CONVERTER"
                            viewModel.updateConverterUnits()
                        },
                        icon = { Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Converter") },
                        label = { Text("Converter", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_converter")
                    )
                    NavigationBarItem(
                        selected = viewModel.activeTab == "HISTORY",
                        onClick = { viewModel.activeTab = "HISTORY" },
                        icon = { Icon(imageVector = Icons.Default.History, contentDescription = "History Ledger") },
                        label = { Text("Ledger Log", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_history")
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            when (viewModel.activeTab) {
                "CALCULATOR" -> CalculatorTab(viewModel, onCalculateTriggered)
                "CONVERTER" -> ConverterTab(viewModel)
                "HISTORY" -> HistoryTab(viewModel)
            }
        }
    }

    SimulatedInterstitialAd(
        show = showInterstitial,
        onDismiss = { showInterstitial = false }
    )

    SimulatedRewardedAd(
        show = showRewardVideoAd,
        onDismiss = { showRewardVideoAd = false },
        onRewardedCompleted = {
            isAdFreeSession = true
            showRewardVideoAd = false
            Toast.makeText(context, "Premium Ad-free Unlocked! Enjoy your shift! 👑", Toast.LENGTH_LONG).show()
        }
    )
}

@Suppress("DeprecatedCallableAddReplaceWith")
@Composable
fun AdMobPremiumBannerPlacement() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        "Ad",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Text(
                    "Banner • ca-app-pub-3884907756129789/banner",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
            Text(
                "Concrete Solutions Co. | High Grade Ready-Mix Delivered Fast",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SimulatedRewardedAd(
    show: Boolean,
    onDismiss: () -> Unit,
    onRewardedCompleted: () -> Unit
) {
    if (show) {
        var countdown by remember { mutableStateOf(5) }
        
        LaunchedEffect(Unit) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
        }

        AlertDialog(
            onDismissRequest = {
                if (countdown == 0) onDismiss()
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.OndemandVideo,
                        contentDescription = "Video Reward",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "Rewarded Video Ad Break")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Ad Unit: ca-app-pub-3884907756129789/reward",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .drawBehind {
                                drawCircle(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF81C784).copy(alpha = 0.4f), Color.Transparent)
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardMembership,
                            contentDescription = "Civil Calc Ad",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(54.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unlock Premium Civil-Calc Tools",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Watch this sponsored video to hide all banner ads, skip interstitial triggers, and unlock professional material cost features instantly.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    if (countdown > 0) {
                        Text(
                            text = "Rewarding in: $countdown seconds...",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "Video completed! Reward unlocked! 👑",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onRewardedCompleted,
                    enabled = countdown == 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (countdown == 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = if (countdown > 0) "Please watch the video..." else "Claim Ad-Free Shift Reward")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    enabled = countdown > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (countdown > 0) {
                        Text("Cancel Ad & Keep Ads On")
                    } else {
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
fun CalculatorTab(viewModel: CalculatorViewModel, onCalculate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Calculation Category Switcher Bar - supports all 6 estimators beautifully
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Calculation Toolkits",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val calcs = listOf(
                    "CONCRETE" to "Concrete",
                    "REBAR" to "Rebar Weight",
                    "AGGREGATE" to "Mix Ratio",
                    "ASPHALT" to "Asphalt",
                    "STEEL_BEAM" to "Steel Beam",
                    "EXCAVATION" to "Excavation"
                )
                items(calcs) { (key, label) ->
                    val isSelected = viewModel.activeCalcType == key
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .clickable { viewModel.activeCalcType = key }
                            .testTag("type_tab_$key")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = when (key) {
                                    "CONCRETE" -> Icons.Default.Widgets
                                    "REBAR" -> Icons.Default.GridOn
                                    "AGGREGATE" -> Icons.Default.Dashboard
                                    "ASPHALT" -> Icons.Default.Layers
                                    "STEEL_BEAM" -> Icons.Default.ViewStream
                                    "EXCAVATION" -> Icons.Default.Terrain
                                    else -> Icons.Default.Calculate
                                },
                                contentDescription = "",
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Show active calculations form
        when (viewModel.activeCalcType) {
            "CONCRETE" -> ConcreteForm(viewModel, onCalculate)
            "REBAR" -> RebarForm(viewModel, onCalculate)
            "AGGREGATE" -> AggregateForm(viewModel, onCalculate)
            "ASPHALT" -> AsphaltForm(viewModel, onCalculate)
            "STEEL_BEAM" -> SteelForm(viewModel, onCalculate)
            "EXCAVATION" -> ExcavationForm(viewModel, onCalculate)
        }
    }
}

@Composable
fun ConcreteForm(viewModel: CalculatorViewModel, onCalculate: () -> Unit) {
    val context = LocalContext.current
    
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(
            title = "Concrete Volume",
            icon = { Icon(Icons.Default.Widgets, "Concrete", tint = MaterialTheme.colorScheme.primary) }
        )

        // Cost Estimator parameters block
        CostEstimatorHeaderCard(
            costLabel = "Concrete Cost per ${if (viewModel.isMetric) "m³" else "Cubic Yard (yd³)"}",
            costVal = viewModel.concreteUnitCost,
            onCostChange = { viewModel.concreteUnitCost = it }
        )

        // Shape Toggle: Slab vs Column
        SegmentedSelectionBar(
            options = listOf("Slab / Rectangular", "Column / Circular"),
            selectedOption = if (viewModel.concreteShape == "RECTANGULAR") "Slab / Rectangular" else "Column / Circular",
            onOptionSelected = {
                viewModel.concreteShape = if (it.startsWith("Slab")) "RECTANGULAR" else "CIRCULAR"
            },
            testTagPrefix = "concrete_shape_toggle"
        )

        if (viewModel.concreteShape == "RECTANGULAR") {
            ConstructionTextField(
                value = viewModel.slabLength,
                onValueChange = { viewModel.slabLength = it },
                label = "Length",
                placeholder = "6.5",
                unitLabel = if (viewModel.isMetric) "m" else "ft",
                testTag = "input_slab_length",
                leadingIcon = { Icon(Icons.Default.ArrowForward, "Length") }
            )

            if (viewModel.isMetric) {
                ConstructionTextField(
                    value = viewModel.slabWidth,
                    onValueChange = { viewModel.slabWidth = it },
                    label = "Width",
                    placeholder = "4.0",
                    unitLabel = "m",
                    testTag = "input_slab_width",
                    leadingIcon = { Icon(Icons.Default.ArrowBack, "Width") }
                )
                ConstructionTextField(
                    value = viewModel.slabThickness,
                    onValueChange = { viewModel.slabThickness = it },
                    label = "Thickness",
                    placeholder = "15.0",
                    unitLabel = "cm",
                    testTag = "input_slab_thickness",
                    leadingIcon = { Icon(Icons.Default.Height, "Thickness") }
                )
            } else {
                ConstructionTextField(
                    value = viewModel.slabWidth,
                    onValueChange = { viewModel.slabWidth = it },
                    label = "Width",
                    placeholder = "12",
                    unitLabel = "ft",
                    testTag = "input_slab_width",
                    leadingIcon = { Icon(Icons.Default.ArrowBack, "Width") }
                )
                ConstructionTextField(
                    value = viewModel.slabThickness,
                    onValueChange = { viewModel.slabThickness = it },
                    label = "Thickness / Depth",
                    placeholder = "6.0",
                    unitLabel = "in",
                    testTag = "input_slab_thickness",
                    leadingIcon = { Icon(Icons.Default.Height, "Thickness") }
                )
            }
            
            ConstructionTextField(
                value = viewModel.slabQty,
                onValueChange = { viewModel.slabQty = it },
                label = "Quantity / Count",
                placeholder = "1",
                unitLabel = "qty",
                testTag = "input_slab_qty",
                leadingIcon = { Icon(Icons.Default.Layers, "Quantity") }
            )
        } else {
            ConstructionTextField(
                value = viewModel.colHeight,
                onValueChange = { viewModel.colHeight = it },
                label = "Height",
                placeholder = "3.2",
                unitLabel = if (viewModel.isMetric) "m" else "ft",
                testTag = "input_col_height",
                leadingIcon = { Icon(Icons.Default.Height, "Height") }
            )

            ConstructionTextField(
                value = viewModel.colDiameter,
                onValueChange = { viewModel.colDiameter = it },
                label = "Diameter",
                placeholder = if (viewModel.isMetric) "30.0" else "12.0",
                unitLabel = if (viewModel.isMetric) "cm" else "in",
                testTag = "input_col_diameter",
                leadingIcon = { Icon(Icons.Default.TripOrigin, "Diameter") }
            )

            ConstructionTextField(
                value = viewModel.colQty,
                onValueChange = { viewModel.colQty = it },
                label = "Quantity / Count",
                placeholder = "1",
                unitLabel = "qty",
                testTag = "input_col_qty",
                leadingIcon = { Icon(Icons.Default.Layers, "Quantity") }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                viewModel.calculateConcrete()
                onCalculate()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_calculate_concrete")
                .minimumInteractiveComponentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Done, "Calculate")
                Text("Calculate Volume & Cost", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        viewModel.validationError?.let {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, "Error", tint = MaterialTheme.colorScheme.error)
                    Text(text = it, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onErrorContainer))
                }
            }
        }

        viewModel.concreteVolumeResult?.let { result ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Concrete Volume & Cost",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val roundedCost = result.estimatedCost?.let { String.format(" ($%.2f)", it) } ?: ""
                                val formattedResult = "Concrete Volume Estimate:\nInputs: ${result.details}\nResult Volume: ${if (viewModel.isMetric) String.format("%.3f m³", result.wetVolume) else String.format("%.2f ft³ (%.2f yd³)", result.wetVolume, result.imperialYardsVolume)}$roundedCost"
                                clipboard.setPrimaryClip(ClipData.newPlainText("Concrete Calc", formattedResult))
                                Toast.makeText(context, "Copied Concrete Estimate details!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Report")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (viewModel.isMetric) String.format("%.3f", result.wetVolume) else String.format("%.2f", result.wetVolume),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = result.outputLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        if (!viewModel.isMetric && result.imperialYardsVolume != null) {
                            Spacer(modifier = Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format("%.2f", result.imperialYardsVolume),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                                )
                                Text(
                                    text = "cubic yards (yd³)",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }

                    result.estimatedCost?.let { cost ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Estimated Materials Cost", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format("$%,.2f", cost),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Text(
                        text = "Inputs Used: ${result.details}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { viewModel.activeCalcType = "AGGREGATE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Layers, "Layers", modifier = Modifier.size(16.dp))
                            Text("Breakdown into Aggregate Mixture", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RebarForm(viewModel: CalculatorViewModel, onCalculate: () -> Unit) {
    val context = LocalContext.current
    
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(
            title = "Rebar Weight & Cost",
            icon = { Icon(Icons.Default.GridOn, "Rebar", tint = MaterialTheme.colorScheme.primary) }
        )

        CostEstimatorHeaderCard(
            costLabel = "Rebar Cost per ${if (viewModel.isMetric) "kg" else "lb"}",
            costVal = viewModel.rebarUnitCost,
            onCostChange = { viewModel.rebarUnitCost = it }
        )

        SegmentedSelectionBar(
            options = listOf("Standard Bar List", "Custom Gauge"),
            selectedOption = if (viewModel.rebarSizeOption == "CUSTOM") "Custom Gauge" else "Standard Bar List",
            onOptionSelected = {
                viewModel.rebarSizeOption = if (it == "Custom Gauge") "CUSTOM" else "SELECT"
            },
            testTagPrefix = "rebar_size_toggle"
        )

        if (viewModel.rebarSizeOption == "SELECT") {
            if (viewModel.isMetric) {
                val sizes = listOf("6", "8", "10", "12", "16", "20", "25", "32")
                Column {
                    Text(
                        text = "Select Bar Diameter (mm)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sizes.forEach { size ->
                            val isSel = viewModel.selectedMetricBarSize == size
                            Surface(
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.selectedMetricBarSize = size }
                                    .testTag("bar_metric_$size")
                            ) {
                                Text(
                                    text = "${size}ø",
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                val sizes = listOf("#3", "#4", "#5", "#6", "#7", "#8", "#9", "#10")
                Column {
                    Text(
                        text = "Select US Rebar Size",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sizes.forEach { size ->
                            val isSel = viewModel.selectedImperialBarSize == size
                            Surface(
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.selectedImperialBarSize = size }
                                    .testTag("bar_imperial_${size.replace("#","")}")
                            ) {
                                Text(
                                    text = size,
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            ConstructionTextField(
                value = viewModel.customRebarDiameter,
                onValueChange = { viewModel.customRebarDiameter = it },
                label = if (viewModel.isMetric) "Custom Diameter (mm)" else "Custom Diameter (inches)",
                placeholder = if (viewModel.isMetric) "14.5" else "0.55",
                unitLabel = if (viewModel.isMetric) "mm" else "in",
                testTag = "input_custom_rebar_diameter",
                leadingIcon = { Icon(Icons.Default.ModeEdit, "Edit") }
            )
        }

        ConstructionTextField(
            value = viewModel.rebarLength,
            onValueChange = { viewModel.rebarLength = it },
            label = "Length of single bar",
            placeholder = if (viewModel.isMetric) "12.0" else "40.0",
            unitLabel = if (viewModel.isMetric) "m" else "ft",
            testTag = "input_rebar_length",
            leadingIcon = { Icon(Icons.Default.TrendingFlat, "Length") }
        )

        ConstructionTextField(
            value = viewModel.rebarQty,
            onValueChange = { viewModel.rebarQty = it },
            label = "Number of bars / Quantity",
            placeholder = "10",
            unitLabel = "bars",
            testTag = "input_rebar_qty",
            leadingIcon = { Icon(Icons.Default.Layers, "Qty") }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                viewModel.calculateRebar()
                onCalculate()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_calculate_rebar")
                .minimumInteractiveComponentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CheckCircle, "Calculate")
                Text("Calculate Weight & Steel Cost", fontWeight = FontWeight.Bold)
            }
        }

        viewModel.validationError?.let {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 8.dp))
                    Text(text = it, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onErrorContainer))
                }
            }
        }

        viewModel.rebarWeightResult?.let { res ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Rebar Weight & Cost",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clipStr = "Rebar weight & cost estimation:\nInputs: ${res.details}\nWeight: ${String.format("%.2f", res.totalWeight)} ${if (viewModel.isMetric) "kg" else "lbs"}\nCost: $${String.format("%.2f", res.estimatedCost ?: 0.0)}"
                                clipboard.setPrimaryClip(ClipData.newPlainText("Rebar Weight", clipStr))
                                Toast.makeText(context, "Copied Rebar estimation details!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, "Copy Report", modifier = Modifier.size(20.dp))
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = String.format("%.2f", res.totalWeight),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = if (viewModel.isMetric) "kg" else "lbs",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    res.estimatedCost?.let { cost ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Estimated Rebar Material Cost", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format("$%,.2f", cost),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Unit Weight", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (viewModel.isMetric) String.format("%.3f kg/m", res.weightPerUnitLen) else String.format("%.3f lbs/ft", res.weightPerUnitLen),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Combined Length", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (viewModel.isMetric) String.format("%.1f m", res.totalLength) else String.format("%.1f ft", res.totalLength),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AggregateForm(viewModel: CalculatorViewModel, onCalculate: () -> Unit) {
    val context = LocalContext.current
    var isCostPanelExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(
            title = "Mixture Materials Proportions",
            icon = { Icon(Icons.Default.Dashboard, "Mix", tint = MaterialTheme.colorScheme.primary) }
        )

        // Mixture Granular costing headers card
        Surface(
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isCostPanelExpanded = !isCostPanelExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, "Costing Details", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text("Granular Cost Estimator Parameters", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Icon(
                        imageVector = if (isCostPanelExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand Options"
                    )
                }
                
                AnimatedVisibility(visible = isCostPanelExpanded) {
                    Column(
                        modifier = Modifier.padding(top = 10.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = viewModel.cementUnitCost,
                                onValueChange = { viewModel.cementUnitCost = it },
                                label = { Text("Cement $/bag") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = viewModel.sandUnitCost,
                                onValueChange = { viewModel.sandUnitCost = it },
                                label = { Text("Sand $/ton") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = viewModel.gravelUnitCost,
                                onValueChange = { viewModel.gravelUnitCost = it },
                                label = { Text("Gravel $/ton") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        ConstructionTextField(
            value = viewModel.aggregateWetVolumeInput,
            onValueChange = { viewModel.aggregateWetVolumeInput = it },
            label = "Concrete Wet Volume required",
            placeholder = if (viewModel.isMetric) "1.5" else "120.0",
            unitLabel = if (viewModel.isMetric) "m³" else "ft³",
            testTag = "input_aggregate_volume_custom",
            leadingIcon = { Icon(Icons.Default.WaterDrop, "Fluid Vol") }
        )

        Column {
            Text(
                text = "Select Proportions / Nominal Mix",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val presets = listOf("M5", "M10", "M15", "M20", "M25", "CUSTOM")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                presets.forEach { preset ->
                    val isSel = viewModel.aggregateMixPreset == preset
                    Surface(
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.aggregateMixPreset = preset }
                            .testTag("mix_preset_$preset")
                    ) {
                        Text(
                            text = preset,
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = viewModel.aggregateMixPreset == "CUSTOM",
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Custom Mix Proportion Parts (Cement : Sand : Gravel)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = viewModel.customCementRatio,
                            onValueChange = { viewModel.customCementRatio = it },
                            label = { Text("Cement") },
                            placeholder = { Text("1") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("ratio_cement")
                        )
                        OutlinedTextField(
                            value = viewModel.customSandRatio,
                            onValueChange = { viewModel.customSandRatio = it },
                            label = { Text("Sand") },
                            placeholder = { Text("2") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("ratio_sand")
                        )
                        OutlinedTextField(
                            value = viewModel.customGravelRatio,
                            onValueChange = { viewModel.customGravelRatio = it },
                            label = { Text("Gravel") },
                            placeholder = { Text("4") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("ratio_gravel")
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                viewModel.calculateAggregate()
                onCalculate()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_calculate_aggregate")
                .minimumInteractiveComponentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Layers, "Layers")
                Text("Calculate Mixture Ingredients", fontWeight = FontWeight.Bold)
            }
        }

        viewModel.validationError?.let {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 8.dp))
                    Text(text = it, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onErrorContainer))
                }
            }
        }

        viewModel.aggregateResult?.let { res ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Ingredients Distribution & Cost",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val roundedCost = res.totalCost?.let { String.format(" | Combined Cost: $%.2f", it) } ?: ""
                                val reportStr = "Civil Mixer Ingredients List:\nInputs: ${res.detailsStr}\nDry Volume: ${String.format("%.3f", res.dryVolume)}\nCement needed: ${String.format("%.1f", res.cementBags)} bags\nSand: ${String.format("%.2f tons", res.sandWeight)}\nGravel: ${String.format("%.2f tons", res.gravelWeight)}$roundedCost"
                                clipboard.setPrimaryClip(ClipData.newPlainText("Aggregate Calc", reportStr))
                                Toast.makeText(context, "Copied distribution reports!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, "Copy Report", modifier = Modifier.size(20.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Est. Wet Vol", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format("%.2f %s", res.wetVolume, if (viewModel.isMetric) "m³" else "ft³"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Dry Vol Factor (1.54 multiplier)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format("%.2f %s", res.dryVolume, if (viewModel.isMetric) "m³" else "ft³"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    res.totalCost?.let { aggregateTotal ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Estimated Materials Cost", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format("$%,.2f", aggregateTotal),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    val lists = listOf(
                        Triple("Cement Ingredients", String.format("%.1f Bags", res.cementBags), "Estimated Cost: " + (res.cementCost?.let { String.format("$%.2f", it) } ?: "$0.00")),
                        Triple("Fine Sand", if (viewModel.isMetric) String.format("%.2f t", res.sandWeight) else String.format("%.2f US tons", res.sandWeight), "Estimated Cost: " + (res.sandCost?.let { String.format("$%.2f", it) } ?: "$0.00")),
                        Triple("Gravel / Coarse Stone", if (viewModel.isMetric) String.format("%.2f t", res.gravelWeight) else String.format("%.2f US tons", res.gravelWeight), "Estimated Cost: " + (res.gravelCost?.let { String.format("$%.2f", it) } ?: "$0.00"))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        lists.forEach { (title, qty, cashLine) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                    Text(text = cashLine, style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                                }
                                Text(text = qty, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                            }
                        }
                    }
                }
            }
        }
    }
}

// New Form 1: Asphalt
@Composable
fun AsphaltForm(viewModel: CalculatorViewModel, onCalculate: () -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(
            title = "Asphalt Volume & Weight Estimations",
            icon = { Icon(Icons.Default.Layers, "Asphalt", tint = MaterialTheme.colorScheme.primary) }
        )

        CostEstimatorHeaderCard(
            costLabel = "Asphalt Cost per ${if (viewModel.isMetric) "Metric Ton (t)" else "Short Ton"}",
            costVal = viewModel.asphaltUnitCost,
            onCostChange = { viewModel.asphaltUnitCost = it }
        )

        ConstructionTextField(
            value = viewModel.asphaltLength,
            onValueChange = { viewModel.asphaltLength = it },
            label = "Length of Section",
            placeholder = if (viewModel.isMetric) "50.0" else "150.0",
            unitLabel = if (viewModel.isMetric) "m" else "ft",
            testTag = "asphalt_length",
            leadingIcon = { Icon(Icons.Default.ArrowForward, "Length") }
        )

        ConstructionTextField(
            value = viewModel.asphaltWidth,
            onValueChange = { viewModel.asphaltWidth = it },
            label = "Width of Section",
            placeholder = if (viewModel.isMetric) "3.5" else "12.0",
            unitLabel = if (viewModel.isMetric) "m" else "ft",
            testTag = "asphalt_width",
            leadingIcon = { Icon(Icons.Default.ArrowBack, "Width") }
        )

        ConstructionTextField(
            value = viewModel.asphaltThickness,
            onValueChange = { viewModel.asphaltThickness = it },
            label = "Compacted Thickness",
            placeholder = if (viewModel.isMetric) "8.0" else "3.0",
            unitLabel = if (viewModel.isMetric) "cm" else "in",
            testTag = "asphalt_thickness",
            leadingIcon = { Icon(Icons.Default.Height, "Thickness") }
        )

        ConstructionTextField(
            value = viewModel.asphaltQty,
            onValueChange = { viewModel.asphaltQty = it },
            label = "Quantity / Count",
            placeholder = "1",
            unitLabel = "qty",
            testTag = "asphalt_qty",
            leadingIcon = { Icon(Icons.Default.Layers, "Count") }
        )

        Button(
            onClick = {
                viewModel.calculateAsphalt()
                onCalculate()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_calculate_asphalt")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Check, "Calc")
                Text("Calculate Asphalt Requirements", fontWeight = FontWeight.Bold)
            }
        }

        viewModel.asphaltResult?.let { result ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Asphalt Allocation Result", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val rep = "Asphalt estimate: ${result.details}\nWeight: ${String.format("%.2f tons", result.weightTons)}\nCost: $${String.format("%.2f", result.estimatedCost ?: 0.0)}"
                                clipboard.setPrimaryClip(ClipData.newPlainText("Asphalt", rep))
                                Toast.makeText(context, "Copied Asphalt report!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, "Copy")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = String.format("%.2f", result.weightTons),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = if (viewModel.isMetric) "Tons (t)" else "Short Tons",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    result.estimatedCost?.let { cost ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Estimated Asphalt Material Cost", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("$%,.2f", cost), fontWeight = FontWeight.Black, color = Color(0xFF4CAF50), fontSize = 18.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Surface Area", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("%.1f %s", result.area, if (viewModel.isMetric) "m²" else "ft²"), fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Volume", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("%.2f %s", result.volume, if (viewModel.isMetric) "m³" else "ft³"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// New Form 2: Steel Beam
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SteelForm(viewModel: CalculatorViewModel, onCalculate: () -> Unit) {
    val context = LocalContext.current
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(
            title = "Structural Steel Beam Calculator",
            icon = { Icon(Icons.Default.ViewStream, "Steel Beam", tint = MaterialTheme.colorScheme.primary) }
        )

        CostEstimatorHeaderCard(
            costLabel = "Steel Cost per ${if (viewModel.isMetric) "kg" else "lb"}",
            costVal = viewModel.steelUnitCost,
            onCostChange = { viewModel.steelUnitCost = it }
        )

        // Preset steel selector
        Column {
            Text("Select Standard Steel beam Profile", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))

            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = when (viewModel.steelPresetSelected) {
                        "CUSTOM" -> "Custom Steel Profile (Enter unit weight)"
                        "IPE_100" -> "I-Beam IPE 100 preset (8.1 kg/m)"
                        "IPE_200" -> "I-Beam IPE 200 preset (22.4 kg/m)"
                        "HEB_200" -> "H-Beam HEB 200 preset (61.3 kg/m)"
                        "W8_10" -> "Steel W8x10 preset (10.0 lb/ft)"
                        "W10_22" -> "Steel W10x22 preset (22.0 lb/ft)"
                        "W12_45" -> "Steel W12x45 preset (45.0 lb/ft)"
                        else -> "CUSTOM"
                    },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    val presets = if (viewModel.isMetric) {
                        listOf("CUSTOM", "IPE_100", "IPE_200", "HEB_200")
                    } else {
                        listOf("CUSTOM", "W8_10", "W10_22", "W12_45")
                    }
                    presets.forEach { label ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (label) {
                                        "CUSTOM" -> "Custom Steel Profile (Manual)"
                                        "IPE_100" -> "I-Beam IPE 100 (8.1 kg/m)"
                                        "IPE_200" -> "I-Beam IPE 200 (22.4 kg/m)"
                                        "HEB_200" -> "H-Beam HEB 200 (61.3 kg/m)"
                                        "W8_10" -> "US Standard W8x10 (10.0 lb/ft)"
                                        "W10_22" -> "US Standard W10x22 (22.0 lb/ft)"
                                        "W12_45" -> "US Standard W12x45 (45.0 lb/ft)"
                                        else -> label
                                    }
                                )
                            },
                            onClick = {
                                viewModel.steelPresetSelected = label
                                viewModel.updateSteelPreset()
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        ConstructionTextField(
            value = viewModel.steelLength,
            onValueChange = { viewModel.steelLength = it },
            label = "Single Beam Length",
            placeholder = "12.0",
            unitLabel = if (viewModel.isMetric) "m" else "ft",
            testTag = "steel_length",
            leadingIcon = { Icon(Icons.Default.Straighten, "Len") }
        )

        ConstructionTextField(
            value = viewModel.steelWeightPerUnit,
            onValueChange = { viewModel.steelWeightPerUnit = it },
            label = if (viewModel.isMetric) "Unit weight (kg per meter)" else "Unit weight (lbs per foot)",
            placeholder = if (viewModel.isMetric) "15.0" else "12.5",
            unitLabel = if (viewModel.isMetric) "kg/m" else "lb/ft",
            testTag = "steel_unit_weight",
            leadingIcon = { Icon(Icons.Default.Scale, "Weight") },
            modifier = Modifier.clickable(enabled = viewModel.steelPresetSelected != "CUSTOM") {}
        )

        ConstructionTextField(
            value = viewModel.steelQty,
            onValueChange = { viewModel.steelQty = it },
            label = "Beam Quantity",
            placeholder = "1",
            unitLabel = "qty",
            testTag = "steel_qty",
            leadingIcon = { Icon(Icons.Default.Layers, "Quantity") }
        )

        Button(
            onClick = {
                viewModel.calculateSteelBeam()
                onCalculate()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("btn_calculate_steel")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Check, "Done")
                Text("Calculate Steel Requirements", fontWeight = FontWeight.Bold)
            }
        }

        viewModel.steelResult?.let { result ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Steel Work Estimate", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val rep = "Structural Steel calculation report:\nDetails: ${result.details}\nWeight Sum: ${String.format("%.2f", result.totalWeight)} ${if (viewModel.isMetric) "kg" else "lbs"}\nCost: $${String.format("%.2f", result.estimatedCost ?: 0.0)}"
                                clipboard.setPrimaryClip(ClipData.newPlainText("Steel", rep))
                                Toast.makeText(context, "Copied report details!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, "copy")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = String.format("%.2f", result.totalWeight),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = if (viewModel.isMetric) "kg" else "lbs",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    result.estimatedCost?.let { cost ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Estimated Structural Steel Cost", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("$%,.2f", cost), fontWeight = FontWeight.Black, color = Color(0xFF4CAF50), fontSize = 18.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Continuous Length", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("%.1f %s", result.totalLength, if (viewModel.isMetric) "m" else "ft"), fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Profile Gauge Weight", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("%.2f %s", result.weightPerUnit, if (viewModel.isMetric) "kg/m" else "lb/ft"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// New Form 3: Excavation
@Composable
fun ExcavationForm(viewModel: CalculatorViewModel, onCalculate: () -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(
            title = "Civil Excavation & Site Groundwork",
            icon = { Icon(Icons.Default.Terrain, "Excavation", tint = MaterialTheme.colorScheme.primary) }
        )

        CostEstimatorHeaderCard(
            costLabel = "Excavation cost per ${if (viewModel.isMetric) "m³ (Bank Vol)" else "Cubic Yard (yd³)"}",
            costVal = viewModel.excavationUnitCost,
            onCostChange = { viewModel.excavationUnitCost = it }
        )

        ConstructionTextField(
            value = viewModel.excavationLength,
            onValueChange = { viewModel.excavationLength = it },
            label = "Excavation Length",
            placeholder = if (viewModel.isMetric) "15.0" else "40.0",
            unitLabel = if (viewModel.isMetric) "m" else "ft",
            testTag = "excavation_length",
            leadingIcon = { Icon(Icons.Default.ArrowForward, "Len") }
        )

        ConstructionTextField(
            value = viewModel.excavationWidth,
            onValueChange = { viewModel.excavationWidth = it },
            label = "Excavation Width",
            placeholder = if (viewModel.isMetric) "8.5" else "25.0",
            unitLabel = if (viewModel.isMetric) "m" else "ft",
            testTag = "excavation_width",
            leadingIcon = { Icon(Icons.Default.ArrowBack, "Width") }
        )

        ConstructionTextField(
            value = viewModel.excavationDepth,
            onValueChange = { viewModel.excavationDepth = it },
            label = "Digging Depth",
            placeholder = if (viewModel.isMetric) "2.5" else "6.0",
            unitLabel = if (viewModel.isMetric) "m" else "ft",
            testTag = "excavation_depth",
            leadingIcon = { Icon(Icons.Default.VerticalAlignBottom, "Depth") }
        )

        ConstructionTextField(
            value = viewModel.excavationSwellFactor,
            onValueChange = { viewModel.excavationSwellFactor = it },
            label = "Soil Swell percentage (Volume Relaxation)",
            placeholder = "15.0",
            unitLabel = "%",
            testTag = "excavation_swell",
            leadingIcon = { Icon(Icons.Default.TrendingUp, "Swell") }
        )

        Button(
            onClick = {
                viewModel.calculateExcavation()
                onCalculate()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("btn_calculate_excavation")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Terrain, "Groundwork")
                Text("Calculate Excavation Volumes", fontWeight = FontWeight.Bold)
            }
        }

        viewModel.excavationResult?.let { result ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Excavation Site Report", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val roundedCost = result.estimatedCost?.let { String.format(" | Price: $%.2f", it) } ?: ""
                                val reports = "Excavation Pit Summary:\nDetails: ${result.details}\nBank Volume: ${String.format("%.2f", result.bankVolume)}\nLoose Volume: ${String.format("%.2f", result.looseVolume)}$roundedCost"
                                clipboard.setPrimaryClip(ClipData.newPlainText("Excavation", reports))
                                Toast.makeText(context, "Copied excavation details!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, "copy")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (viewModel.isMetric) {
                                String.format("%.2f", result.bankVolume)
                            } else {
                                String.format("%.2f", result.imperialYardsBankVolume ?: 0.0)
                            },
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = if (viewModel.isMetric) "Bank m³" else "Bank yd³ (CY)",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    result.estimatedCost?.let { cost ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Estimated Site Excavation Cost", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("$%,.2f", cost), fontWeight = FontWeight.Black, color = Color(0xFF4CAF50), fontSize = 18.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Bank Volume (In-Situ)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (viewModel.isMetric) String.format("%.2f m³", result.bankVolume) else String.format("%.1f ft³ (%.2f yd³)", result.bankVolume, result.imperialYardsBankVolume),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Loose Volume (With Swell)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (viewModel.isMetric) String.format("%.2f m³", result.looseVolume) else String.format("%.1f ft³ (%.2f yd³)", result.looseVolume, result.imperialYardsLooseVolume),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// Reusable Helper Component for Cost Estimator Configuration Cards
@Composable
fun CostEstimatorHeaderCard(
    costLabel: String,
    costVal: String,
    onCostChange: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Payments,
                contentDescription = "Cost Estimating Tool",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = costLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("$", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)))
                    BasicTextFieldWithoutOutlines(
                        value = costVal,
                        onValueChange = onCostChange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun BasicTextFieldWithoutOutlines(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}

@Composable
fun ConverterTab(viewModel: CalculatorViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(
            title = "Unit Conversion Utility",
            icon = { Icon(Icons.Default.SwapHoriz, "Converters", tint = MaterialTheme.colorScheme.primary) }
        )

        val categories = listOf(
            Triple("LENGTH", Icons.Default.LinearScale, "Length"),
            Triple("AREA", Icons.Default.SelectAll, "Area"),
            Triple("VOLUME", Icons.Default.ViewInAr, "Volume"),
            Triple("WEIGHT", Icons.Default.Scale, "Weight")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEach { (catId, icon, word) ->
                val isSel = viewModel.converterCategory == catId
                Surface(
                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.converterCategory = catId
                            viewModel.updateConverterUnits()
                        }
                        .testTag("converter_cat_$catId"),
                    tonalElevation = if (isSel) 0.dp else 2.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = word,
                            tint = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = word,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        ConstructionTextField(
            value = viewModel.converterInputVal,
            onValueChange = {
                viewModel.converterInputVal = it
                viewModel.performQuickConversion()
            },
            label = "Input Amount to Convert",
            placeholder = "0.0",
            unitLabel = viewModel.converterFromUnit,
            testTag = "converter_input_field",
            leadingIcon = { Icon(Icons.Default.Calculate, "Number") }
        )

        val selectableUnitsList = when (viewModel.converterCategory) {
            "LENGTH" -> listOf("m", "ft", "in", "yd", "mm")
            "AREA" -> listOf("m²", "ft²", "yd²")
            "VOLUME" -> listOf("m³", "ft³", "yd³", "L")
            "WEIGHT" -> listOf("kg", "lb", "t", "ton")
            else -> listOf("m", "ft")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("From Unit", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 6.dp))
                LazyRowSelection(
                    items = selectableUnitsList,
                    selectedItem = viewModel.converterFromUnit,
                    onSelected = {
                        viewModel.converterFromUnit = it
                        if (viewModel.converterInputVal.isNotEmpty()) viewModel.performQuickConversion()
                    },
                    testTagSuffix = "from"
                )
            }

            Icon(
                imageVector = Icons.Default.CompareArrows,
                contentDescription = "Compare",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(24.dp)
                    .clickable {
                        val temp = viewModel.converterFromUnit
                        viewModel.converterFromUnit = viewModel.converterToUnit
                        viewModel.converterToUnit = temp
                        if (viewModel.converterInputVal.isNotEmpty()) viewModel.performQuickConversion()
                    }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text("To Unit", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 6.dp))
                LazyRowSelection(
                    items = selectableUnitsList,
                    selectedItem = viewModel.converterToUnit,
                    onSelected = {
                        viewModel.converterToUnit = it
                        if (viewModel.converterInputVal.isNotEmpty()) viewModel.performQuickConversion()
                    },
                    testTagSuffix = "to"
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Conversion Result Output",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = viewModel.converterResultVal.ifEmpty { "Enter custom amount above to convert" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = if (viewModel.converterResultVal.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun LazyRowSelection(
    items: List<String>,
    selectedItem: String,
    onSelected: (String) -> Unit,
    testTagSuffix: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)), RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { value ->
            val matches = value == selectedItem
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (matches) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelected(value) }
                    .testTag("unit_${testTagSuffix}_$value")
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (matches) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryTab(viewModel: CalculatorViewModel) {
    val context = LocalContext.current
    val historyList by viewModel.historyLogList.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(
                title = "Job Calculation Ledger",
                icon = { Icon(Icons.Default.Assignment, "Ledger", tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.weight(1f)
            )

            if (historyList.isNotEmpty()) {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        Toast.makeText(context, "Cleared whole history ledger!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_clear_all_ledger")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Clear All Ledger",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Ledger Empty",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = "History Ledger Empty",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Calculations performed on the jobs will appear here automatically for fast retrieval & copying.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().weight(1f)
            ) {
                items(
                    items = historyList,
                    key = { item -> item.id }
                ) { item ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    viewModel.loadFromHistory(item)
                                    Toast.makeText(context, "${item.title} populated into Calculator!", Toast.LENGTH_SHORT).show()
                                },
                                onLongClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Copied Result", "${item.title}\n${item.inputs}\n${item.outputs}"))
                                    Toast.makeText(context, "Copied ledger item to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            .testTag("history_item_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = when (item.calcType) {
                                        "CONCRETE" -> Icons.Default.Widgets
                                        "REBAR" -> Icons.Default.GridOn
                                        "AGGREGATE" -> Icons.Default.Dashboard
                                        "ASPHALT" -> Icons.Default.Layers
                                        "STEEL_BEAM" -> Icons.Default.ViewStream
                                        "EXCAVATION" -> Icons.Default.Terrain
                                        else -> Icons.Default.Calculate
                                    },
                                    contentDescription = "Log Type",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = item.unitSystem,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = item.inputs,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.outputs,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.deleteHistoryItem(item.id)
                                    Toast.makeText(context, "Calculation line deleted", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp).testTag("delete_history_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Item",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = "💡 Tip: Tap ledger list entries once to populate them into the active calculators, or press and hold to copy details.",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
