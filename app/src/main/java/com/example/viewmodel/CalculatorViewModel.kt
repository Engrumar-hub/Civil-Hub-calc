package com.example.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryEntity
import com.example.data.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.PI

class CalculatorViewModel(private val repository: HistoryRepository) : ViewModel() {

    // Global settings states
    var isMetric by mutableStateOf(true)
    var isDarkMode by mutableStateOf(true) // Start dark for "Immersive UI" flavor

    // Active sub-sections
    var activeTab by mutableStateOf("CALCULATOR") // "CALCULATOR", "CONVERTER", "HISTORY", "METRICS"
    var activeCalcType by mutableStateOf("CONCRETE") // "CONCRETE", "REBAR", "AGGREGATE", "ASPHALT", "STEEL_BEAM", "EXCAVATION"

    // Concrete volume input states
    var concreteShape by mutableStateOf("RECTANGULAR") // "RECTANGULAR", "CIRCULAR"
    
    // Rectangular/Slab inputs
    var slabLength by mutableStateOf("")
    var slabWidth by mutableStateOf("")
    var slabThickness by mutableStateOf("") // Entered in Centimeters (Metric) or Inches (Imperial)
    var slabQty by mutableStateOf("1")

    // Circular column inputs
    var colHeight by mutableStateOf("")
    var colDiameter by mutableStateOf("") // Entered in Centimeters (Metric) or Inches (Imperial)
    var colQty by mutableStateOf("1")

    // Rebar inputs
    var rebarSizeOption by mutableStateOf("SELECT") // "SELECT", "CUSTOM"
    var selectedMetricBarSize by mutableStateOf("12") // Bar diameter in mm
    var selectedImperialBarSize by mutableStateOf("#4") // Standard US sizes
    var customRebarDiameter by mutableStateOf("") // mm or inches
    var rebarLength by mutableStateOf("")
    var rebarQty by mutableStateOf("1")

    // Aggregate ratio inputs
    var aggregateWetVolumeInput by mutableStateOf("") // Prepopulated with last volume or custom
    var aggregateMixPreset by mutableStateOf("M20") // M5, M10, M15, M20, M25, CUSTOM
    var customCementRatio by mutableStateOf("1")
    var customSandRatio by mutableStateOf("2")
    var customGravelRatio by mutableStateOf("4")

    // Asphalt Input States
    var asphaltLength by mutableStateOf("")
    var asphaltWidth by mutableStateOf("")
    var asphaltThickness by mutableStateOf("") // Entered in Centimeters (Metric) or Inches (Imperial)
    var asphaltQty by mutableStateOf("1")

    // Steel Beam Input States
    var steelLength by mutableStateOf("")
    var steelWeightPerUnit by mutableStateOf("") // kg/m or lb/ft
    var steelQty by mutableStateOf("1")
    var steelPresetSelected by mutableStateOf("CUSTOM") // CUSTOM, IPE_100, IPE_200, HEB_200, W8_10, W10_22, W12_45

    // Excavation Input States
    var excavationLength by mutableStateOf("")
    var excavationWidth by mutableStateOf("")
    var excavationDepth by mutableStateOf("")
    var excavationSwellFactor by mutableStateOf("15") // % Swell

    // Cost Estimator Inputs (Custom pricing)
    var concreteUnitCost by mutableStateOf("120.00") // Per cubic meter or cubic yard
    var rebarUnitCost by mutableStateOf("1.25") // Per kg or lb
    var cementUnitCost by mutableStateOf("8.50") // Per bag
    var sandUnitCost by mutableStateOf("25.00") // Per ton
    var gravelUnitCost by mutableStateOf("30.00") // Per ton
    var asphaltUnitCost by mutableStateOf("95.00") // Per ton
    var steelUnitCost by mutableStateOf("1.80") // Per kg or lb
    var excavationUnitCost by mutableStateOf("18.00") // Per m³ or yard³

    // Output States
    var concreteVolumeResult by mutableStateOf<ConcreteResult?>(null)
    var rebarWeightResult by mutableStateOf<RebarResult?>(null)
    var aggregateResult by mutableStateOf<AggregateResult?>(null)
    var asphaltResult by mutableStateOf<AsphaltResult?>(null)
    var steelResult by mutableStateOf<SteelResult?>(null)
    var excavationResult by mutableStateOf<ExcavationResult?>(null)

    // Error messages
    var validationError by mutableStateOf<String?>(null)

    // History Flow from Database
    val historyLogList: StateFlow<List<HistoryEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Unit Conversion States
    var converterCategory by mutableStateOf("LENGTH") // LENGTH, AREA, VOLUME, WEIGHT
    var converterInputVal by mutableStateOf("")
    var converterFromUnit by mutableStateOf("m")
    var converterToUnit by mutableStateOf("ft")
    var converterResultVal by mutableStateOf("")

    // Calculation Result Models
    data class ConcreteResult(
        val wetVolume: Double, // Cubic Meters or Cubic Feet
        val outputLabel: String, // m³ or ft³
        val imperialYardsVolume: Double? = null, // Cubic Yards for Imperial
        val shape: String,
        val details: String,
        val estimatedCost: Double? = null
    )

    data class RebarResult(
        val totalWeight: Double, // kg or lbs
        val weightPerUnitLen: Double, // kg/m or lb/ft
        val totalLength: Double, // meters or feet
        val details: String,
        val estimatedCost: Double? = null
    )

    data class AggregateResult(
        val wetVolume: Double,
        val dryVolume: Double,
        val cementWeight: Double, // kg or lbs
        val cementBags: Double, // 50kg or 94lb bags
        val sandVolume: Double, // m³ or ft³
        val sandWeight: Double, // metric tons or US tons
        val gravelVolume: Double, // m³ or ft³
        val gravelWeight: Double, // metric tons or US tons
        val detailsStr: String,
        val cementCost: Double? = null,
        val sandCost: Double? = null,
        val gravelCost: Double? = null,
        val totalCost: Double? = null
    )

    data class AsphaltResult(
        val area: Double, // m² or ft²
        val volume: Double, // m³ or ft³
        val weightTons: Double, // Metric tons or US tons
        val estimatedCost: Double? = null,
        val details: String
    )

    data class SteelResult(
        val totalWeight: Double, // kg or lbs
        val weightPerUnit: Double, // kg/m or lb/ft
        val totalLength: Double, // meters or feet
        val estimatedCost: Double? = null,
        val details: String
    )

    data class ExcavationResult(
        val bankVolume: Double, // m³ or ft³
        val looseVolume: Double, // m³ or ft³
        val imperialYardsBankVolume: Double? = null, // Cubic Yards if imperial
        val imperialYardsLooseVolume: Double? = null, // Cubic Yards if imperial
        val estimatedCost: Double? = null,
        val details: String
    )

    // Sync isMetric changes with matching defaults
    fun toggleUnitSystem() {
        isMetric = !isMetric
        // Switch defaults to sensible metric/imperial numbers
        if (isMetric) {
            concreteUnitCost = "120.00" // Per m³
            rebarUnitCost = "1.25" // Per kg
            cementUnitCost = "8.50" // Per bag
            sandUnitCost = "25.00" // Per ton
            gravelUnitCost = "30.00" // Per ton
            asphaltUnitCost = "95.00" // Per metric ton
            steelUnitCost = "1.80" // Per kg
            excavationUnitCost = "18.00" // Per m³
            if (steelPresetSelected != "CUSTOM" && steelPresetSelected.startsWith("W")) {
                steelPresetSelected = "CUSTOM"
                steelWeightPerUnit = ""
            }
        } else {
            concreteUnitCost = "95.00" // Per Cubic Yard
            rebarUnitCost = "0.75" // Per lb
            cementUnitCost = "11.50" // Per 94lb bag
            sandUnitCost = "35.00" // Per short ton
            gravelUnitCost = "40.00" // Per short ton
            asphaltUnitCost = "85.00" // Per short ton
            steelUnitCost = "1.10" // Per lb
            excavationUnitCost = "22.00" // Per Cubic Yard
            if (steelPresetSelected != "CUSTOM" && !steelPresetSelected.startsWith("W")) {
                steelPresetSelected = "CUSTOM"
                steelWeightPerUnit = ""
            }
        }
    }

    // Calculations execution
    fun calculateConcrete() {
        validationError = null
        try {
            val costPerUnit = concreteUnitCost.toDoubleOrNull() ?: 0.0

            if (concreteShape == "RECTANGULAR") {
                val len = slabLength.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid length")
                val wid = slabWidth.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid width")
                val thick = slabThickness.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid thickness")
                val qty = slabQty.toIntOrNull() ?: throw IllegalArgumentException("Invalid quantity")

                if (len <= 0 || wid <= 0 || thick <= 0 || qty <= 0) {
                    throw IllegalArgumentException("Inputs must be positive numbers")
                }

                if (isMetric) {
                    val thicknessM = thick / 100.0
                    val totalVol = len * wid * thicknessM * qty
                    val estimatedCost = totalVol * costPerUnit
                    
                    val detailsText = "Slab: L=${len}m, W=${wid}m, T=${thick}cm, Qty=$qty"
                    val resultString = String.format("%.3f m³", totalVol)
                    val costString = String.format("$%.2f", estimatedCost)
                    
                    concreteVolumeResult = ConcreteResult(totalVol, "m³", null, "Rectangular", detailsText, estimatedCost)
                    aggregateWetVolumeInput = String.format("%.3f", totalVol)
                    
                    saveHistory(
                        calcType = "CONCRETE",
                        title = "Slab Vol: ${resultString} (${costString})",
                        inputs = "$detailsText (Cost: $$costPerUnit/m³)",
                        outputs = "Wet Volume: $resultString, Estimated Cost: $costString"
                    )
                } else {
                    val thicknessFt = thick / 12.0
                    val totalVolFt3 = len * wid * thicknessFt * qty
                    val totalVolYd3 = totalVolFt3 / 27.0
                    val estimatedCost = totalVolYd3 * costPerUnit
                    
                    val detailsText = "Slab: L=${len}ft, W=${wid}ft, T=${thick}in, Qty=$qty"
                    val resultString = String.format("%.2f ft³ (%.2f yd³)", totalVolFt3, totalVolYd3)
                    val costString = String.format("$%.2f", estimatedCost)
                    
                    concreteVolumeResult = ConcreteResult(totalVolFt3, "ft³", totalVolYd3, "Rectangular", detailsText, estimatedCost)
                    aggregateWetVolumeInput = String.format("%.2f", totalVolFt3)
                    
                    saveHistory(
                        calcType = "CONCRETE",
                        title = "Slab Vol: ${String.format("%.2f yd³", totalVolYd3)} (${costString})",
                        inputs = "$detailsText (Cost: $$costPerUnit/yd³)",
                        outputs = "Wet Volume: $resultString, Estimated Cost: $costString"
                    )
                }
            } else {
                val height = colHeight.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid height")
                val diameter = colDiameter.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid diameter")
                val qty = colQty.toIntOrNull() ?: throw IllegalArgumentException("Invalid quantity")

                if (height <= 0 || diameter <= 0 || qty <= 0) {
                    throw IllegalArgumentException("Inputs must be positive numbers")
                }

                if (isMetric) {
                    val r = (diameter / 100.0) / 2.0
                    val totalVol = PI * r * r * height * qty
                    val estimatedCost = totalVol * costPerUnit
                    
                    val detailsText = "Col: H=${height}m, Ø=${diameter}cm, Qty=$qty"
                    val resultString = String.format("%.3f m³", totalVol)
                    val costString = String.format("$%.2f", estimatedCost)
                    
                    concreteVolumeResult = ConcreteResult(totalVol, "m³", null, "Circular Column", detailsText, estimatedCost)
                    aggregateWetVolumeInput = String.format("%.3f", totalVol)

                    saveHistory(
                        calcType = "CONCRETE",
                        title = "Column Vol: ${resultString} (${costString})",
                        inputs = "$detailsText (Cost: $$costPerUnit/m³)",
                        outputs = "Wet Volume: $resultString, Estimated Cost: $costString"
                    )
                } else {
                    val r = (diameter / 12.0) / 2.0
                    val totalVolFt3 = PI * r * r * height * qty
                    val totalVolYd3 = totalVolFt3 / 27.0
                    val estimatedCost = totalVolYd3 * costPerUnit
                    
                    val detailsText = "Col: H=${height}ft, Ø=${diameter}in, Qty=$qty"
                    val resultString = String.format("%.2f ft³ (%.2f yd³)", totalVolFt3, totalVolYd3)
                    val costString = String.format("$%.2f", estimatedCost)
                    
                    concreteVolumeResult = ConcreteResult(totalVolFt3, "ft³", totalVolYd3, "Circular Column", detailsText, estimatedCost)
                    aggregateWetVolumeInput = String.format("%.2f", totalVolFt3)

                    saveHistory(
                        calcType = "CONCRETE",
                        title = "Column Vol: ${String.format("%.2f yd³", totalVolYd3)} (${costString})",
                        inputs = "$detailsText (Cost: $$costPerUnit/yd³)",
                        outputs = "Wet Volume: $resultString, Estimated Cost: $costString"
                    )
                }
            }
        } catch (e: Exception) {
            validationError = e.message ?: "Invalid inputs. Please verify."
        }
    }

    fun calculateRebar() {
        validationError = null
        try {
            val length = rebarLength.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid total bar length")
            val qty = rebarQty.toIntOrNull() ?: throw IllegalArgumentException("Invalid quantity")
            val costPerUnit = rebarUnitCost.toDoubleOrNull() ?: 0.0

            if (length <= 0 || qty <= 0) {
                throw IllegalArgumentException("Length and quantity must be greater than zero")
            }

            val totalLengthVal = length * qty

            if (isMetric) {
                val dia = if (rebarSizeOption == "CUSTOM") {
                    customRebarDiameter.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid custom diameter")
                } else {
                    selectedMetricBarSize.toDoubleOrNull() ?: throw IllegalArgumentException("Verify selected bar size")
                }

                if (dia <= 0) throw IllegalArgumentException("Diameter must be positive")

                val uWeight = (dia * dia) / 162.0
                val totalW = uWeight * totalLengthVal
                val estimatedCost = totalW * costPerUnit

                val detailsText = "Rebar: Ø=${dia}mm, L=${length}m, Qty=$qty"
                val outputText = String.format("Weight: %.2f kg (Unit: %.3f kg/m, Length: %.1f m), Cost: $%.2f", totalW, uWeight, totalLengthVal, estimatedCost)
                rebarWeightResult = RebarResult(totalW, uWeight, totalLengthVal, detailsText, estimatedCost)

                saveHistory(
                    calcType = "REBAR",
                    title = "Rebar: ${String.format("%.1f kg", totalW)} ($${String.format("%.2f", estimatedCost)})",
                    inputs = "$detailsText (Cost: $$costPerUnit/kg)",
                    outputs = outputText
                )
            } else {
                var uWeight = 0.0
                val desc: String
                
                if (rebarSizeOption == "CUSTOM") {
                    val dia = customRebarDiameter.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid custom diameter")
                    if (dia <= 0) throw IllegalArgumentException("Diameter must be positive")
                    uWeight = dia * dia * 2.669
                    desc = "Custom Ø=${dia} in"
                } else {
                    desc = selectedImperialBarSize
                    uWeight = when (selectedImperialBarSize) {
                        "#3" -> 0.376
                        "#4" -> 0.668
                        "#5" -> 1.043
                        "#6" -> 1.502
                        "#7" -> 2.044
                        "#8" -> 2.670
                        "#9" -> 3.400
                        "#10" -> 4.303
                        else -> throw IllegalArgumentException("Invalid size selection")
                    }
                }

                val totalW = uWeight * totalLengthVal
                val estimatedCost = totalW * costPerUnit
                val detailsText = "Rebar: Size=$desc, L=${length}ft, Qty=$qty"
                val outputText = String.format("Weight: %.1f lbs (Unit: %.3f lb/ft, Length: %.1f ft), Cost: $%.2f", totalW, uWeight, totalLengthVal, estimatedCost)
                rebarWeightResult = RebarResult(totalW, uWeight, totalLengthVal, detailsText, estimatedCost)

                saveHistory(
                    calcType = "REBAR",
                    title = "Rebar: ${String.format("%.1f lbs", totalW)} ($${String.format("%.2f", estimatedCost)})",
                    inputs = "$detailsText (Cost: $$costPerUnit/lb)",
                    outputs = outputText
                )
            }
        } catch (e: Exception) {
            validationError = e.message ?: "Invalid inputs. Please verify."
        }
    }

    fun calculateAggregate() {
        validationError = null
        try {
            val wetVol = aggregateWetVolumeInput.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid wet volume")
            if (wetVol <= 0) throw IllegalArgumentException("Volume must be greater than zero")

            val dryVol = wetVol * 1.54

            val cRatio: Double
            val sRatio: Double
            val gRatio: Double

            if (aggregateMixPreset == "CUSTOM") {
                cRatio = customCementRatio.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid cement ratio")
                sRatio = customSandRatio.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid sand ratio")
                gRatio = customGravelRatio.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid gravel ratio")
            } else {
                when (aggregateMixPreset) {
                    "M5" -> { cRatio = 1.0; sRatio = 5.0; gRatio = 10.0 }
                    "M7.5" -> { cRatio = 1.0; sRatio = 4.0; gRatio = 8.0 }
                    "M10" -> { cRatio = 1.0; sRatio = 3.0; gRatio = 6.0 }
                    "M15" -> { cRatio = 1.0; sRatio = 2.0; gRatio = 4.0 }
                    "M20" -> { cRatio = 1.0; sRatio = 1.5; gRatio = 3.0 }
                    "M25" -> { cRatio = 1.0; sRatio = 1.0; gRatio = 2.0 }
                    else -> throw IllegalArgumentException("Unknown Mix Preset")
                }
            }

            if (cRatio <= 0 || sRatio <= 0 || gRatio <= 0) {
                throw IllegalArgumentException("Proportions must be positive numbers")
            }

            val totalParts = cRatio + sRatio + gRatio
            val cementVol = dryVol * (cRatio / totalParts)
            val sandVol = dryVol * (sRatio / totalParts)
            val gravelVol = dryVol * (gRatio / totalParts)

            val uCementPrice = cementUnitCost.toDoubleOrNull() ?: 0.0
            val uSandPrice = sandUnitCost.toDoubleOrNull() ?: 0.0
            val uGravelPrice = gravelUnitCost.toDoubleOrNull() ?: 0.0

            if (isMetric) {
                val cementWt = cementVol * 1440.0
                val bagsCount = cementWt / 50.0 // 50kg bag
                val sandWtTons = (sandVol * 1600.0) / 1000.0
                val gravelWtTons = (gravelVol * 1650.0) / 1000.0

                val cementCost = bagsCount * uCementPrice
                val sandCost = sandWtTons * uSandPrice
                val gravelCost = gravelWtTons * uGravelPrice
                val totalCost = cementCost + sandCost + gravelCost

                val detailsText = "Aggregates (Mix $aggregateMixPreset, Wet Vol=${String.format("%.3f", wetVol)} m³)"
                val outputText = String.format(
                    "Dry Vol: %.3f m³, Cement: %.1f bags ($%.2f), Sand: %.2f t ($%.2f), Gravel: %.2f t ($%.2f), Total Cost: $%.2f",
                    dryVol, bagsCount, cementCost, sandWtTons, sandCost, gravelWtTons, gravelCost, totalCost
                )

                aggregateResult = AggregateResult(
                    wetVolume = wetVol,
                    dryVolume = dryVol,
                    cementWeight = cementWt,
                    cementBags = bagsCount,
                    sandVolume = sandVol,
                    sandWeight = sandWtTons,
                    gravelVolume = gravelVol,
                    gravelWeight = gravelWtTons,
                    detailsStr = detailsText,
                    cementCost = cementCost,
                    sandCost = sandCost,
                    gravelCost = gravelCost,
                    totalCost = totalCost
                )

                saveHistory(
                    calcType = "AGGREGATE",
                    title = "Mix $aggregateMixPreset: ${String.format("%.1f bags", bagsCount)} ($${String.format("%.2f", totalCost)})",
                    inputs = "$detailsText (Cement: $$uCementPrice/bag, Sand: $$uSandPrice/t, Gravel: $$uGravelPrice/t)",
                    outputs = outputText
                )
            } else {
                val cementWt = cementVol * 94.0
                val bagsCount = cementWt / 94.0 // 94lb bags
                val sandWtTons = (sandVol * 100.0) / 2000.0 // short tons
                val gravelWtTons = (gravelVol * 103.0) / 2000.0

                val cementCost = bagsCount * uCementPrice
                val sandCost = sandWtTons * uSandPrice
                val gravelCost = gravelWtTons * uGravelPrice
                val totalCost = cementCost + sandCost + gravelCost

                val detailsText = "Aggregates (Mix $aggregateMixPreset, Wet Vol=${String.format("%.2f", wetVol)} ft³)"
                val outputText = String.format(
                    "Dry Vol: %.2f ft³, Cement: %.1f bags ($%.2f), Sand: %.2f tons ($%.2f), Gravel: %.2f tons ($%.2f), Total Cost: $%.2f",
                    dryVol, bagsCount, cementCost, sandWtTons, sandCost, gravelWtTons, gravelCost, totalCost
                )

                aggregateResult = AggregateResult(
                    wetVolume = wetVol,
                    dryVolume = dryVol,
                    cementWeight = cementWt,
                    cementBags = bagsCount,
                    sandVolume = sandVol,
                    sandWeight = sandWtTons,
                    gravelVolume = gravelVol,
                    gravelWeight = gravelWtTons,
                    detailsStr = detailsText,
                    cementCost = cementCost,
                    sandCost = sandCost,
                    gravelCost = gravelCost,
                    totalCost = totalCost
                )

                saveHistory(
                    calcType = "AGGREGATE",
                    title = "Mix $aggregateMixPreset: ${String.format("%.1f bags", bagsCount)} ($${String.format("%.2f", totalCost)})",
                    inputs = "$detailsText (Cement: $$uCementPrice/bag, Sand: $$uSandPrice/ton, Gravel: $$uGravelPrice/ton)",
                    outputs = outputText
                )
            }
        } catch (e: Exception) {
            validationError = e.message ?: "Invalid inputs. Please verify."
        }
    }

    // New Calculator 1: Asphalt volume, weight & cost
    fun calculateAsphalt() {
        validationError = null
        try {
            val len = asphaltLength.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid length")
            val wid = asphaltWidth.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid width")
            val thick = asphaltThickness.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid thickness")
            val qty = asphaltQty.toIntOrNull() ?: throw IllegalArgumentException("Invalid quantity")
            val uPrice = asphaltUnitCost.toDoubleOrNull() ?: 0.0

            if (len <= 0 || wid <= 0 || thick <= 0 || qty <= 0) {
                throw IllegalArgumentException("Inputs must be positive numbers")
            }

            val area = len * wid * qty
            if (isMetric) {
                // Length: m, Width: m, Thickness: cm, density standard 2400 kg/m³
                val thicknessM = thick / 100.0
                val volume = area * thicknessM
                val weightKg = volume * 2400.0
                val weightTons = weightKg / 1000.0
                val estimatedCost = weightTons * uPrice

                val detailsText = "Asphalt: ${len}m x ${wid}m @ ${thick}cm, Qty=$qty"
                val outputText = String.format("Area: %.1f m², Volume: %.2f m³, Weight: %.2f t, Cost: $%.2f", area, volume, weightTons, estimatedCost)
                
                asphaltResult = AsphaltResult(area, volume, weightTons, estimatedCost, detailsText)

                saveHistory(
                    calcType = "ASPHALT",
                    title = "Asphalt Pavement: ${String.format("%.1f t", weightTons)} ($${String.format("%.2f", estimatedCost)})",
                    inputs = "$detailsText (Cost: $$uPrice/tonne)",
                    outputs = outputText
                )
            } else {
                // Length: ft, Width: ft, Thickness: inches, density standard 148 lbs/ft³
                val thicknessFt = thick / 12.0
                val volume = area * thicknessFt
                val weightLbs = volume * 148.0
                val weightTons = weightLbs / 2000.0 // short tons
                val estimatedCost = weightTons * uPrice

                val detailsText = "Asphalt: ${len}ft x ${wid}ft @ ${thick}in, Qty=$qty"
                val outputText = String.format("Area: %.1f ft², Volume: %.2f ft³, Weight: %.2f ton, Cost: $%.2f", area, volume, weightTons, estimatedCost)
                
                asphaltResult = AsphaltResult(area, volume, weightTons, estimatedCost, detailsText)

                saveHistory(
                    calcType = "ASPHALT",
                    title = "Asphalt Pavement: ${String.format("%.1f tons", weightTons)} ($${String.format("%.2f", estimatedCost)})",
                    inputs = "$detailsText (Cost: $$uPrice/ton)",
                    outputs = outputText
                )
            }
        } catch (e: Exception) {
            validationError = e.message ?: "Invalid inputs. Please verify."
        }
    }

    // New Calculator 2: Steel weight & cost
    fun updateSteelPreset() {
        if (steelPresetSelected == "CUSTOM") return
        steelWeightPerUnit = if (isMetric) {
            when (steelPresetSelected) {
                "IPE_100" -> "8.1"
                "IPE_200" -> "22.4"
                "HEB_200" -> "61.3"
                else -> ""
            }
        } else {
            when (steelPresetSelected) {
                "W8_10" -> "10.0"
                "W10_22" -> "22.0"
                "W12_45" -> "45.0"
                else -> ""
            }
        }
    }

    fun calculateSteelBeam() {
        validationError = null
        try {
            val len = steelLength.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid length")
            val uWeight = steelWeightPerUnit.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid unit weight")
            val qty = steelQty.toIntOrNull() ?: throw IllegalArgumentException("Invalid quantity")
            val uPrice = steelUnitCost.toDoubleOrNull() ?: 0.0

            if (len <= 0 || uWeight <= 0 || qty <= 0) {
                throw IllegalArgumentException("Inputs must be positive numbers")
            }

            val totalLength = len * qty
            val totalWeight = totalLength * uWeight
            val estimatedCost = totalWeight * uPrice

            if (isMetric) {
                val detailsText = "Steel Beam (${steelPresetSelected}): L=${len}m, W_unit=${uWeight}kg/m, Qty=$qty"
                val outputText = String.format("Total Length: %.2f m, Total Weight: %.2f kg, Cost: $%.2f", totalLength, totalWeight, estimatedCost)
                
                steelResult = SteelResult(totalWeight, uWeight, totalLength, estimatedCost, detailsText)

                saveHistory(
                    calcType = "STEEL_BEAM",
                    title = "Steel: ${String.format("%.1f kg", totalWeight)} ($${String.format("%.2f", estimatedCost)})",
                    inputs = "$detailsText (Cost: $$uPrice/kg)",
                    outputs = outputText
                )
            } else {
                val detailsText = "Steel Beam (${steelPresetSelected}): L=${len}ft, W_unit=${uWeight}lb/ft, Qty=$qty"
                val outputText = String.format("Total Length: %.2f ft, Total Weight: %.2f lbs, Cost: $%.2f", totalLength, totalWeight, estimatedCost)
                
                steelResult = SteelResult(totalWeight, uWeight, totalLength, estimatedCost, detailsText)

                saveHistory(
                    calcType = "STEEL_BEAM",
                    title = "Steel: ${String.format("%.1f lbs", totalWeight)} ($${String.format("%.2f", estimatedCost)})",
                    inputs = "$detailsText (Cost: $$uPrice/lb)",
                    outputs = outputText
                )
            }
        } catch (e: Exception) {
            validationError = e.message ?: "Invalid inputs. Please verify."
        }
    }

    // New Calculator 3: Excavation bank volume, loose volume & cost
    fun calculateExcavation() {
        validationError = null
        try {
            val len = excavationLength.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid excavation length")
            val wid = excavationWidth.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid excavation width")
            val dep = excavationDepth.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid excavation depth")
            val swellFactorPct = excavationSwellFactor.toDoubleOrNull() ?: 0.0
            val uPrice = excavationUnitCost.toDoubleOrNull() ?: 0.0

            if (len <= 0 || wid <= 0 || dep <= 0 || swellFactorPct < 0) {
                throw IllegalArgumentException("Inputs must be positive, swell must be non-negative")
            }

            val bankVol = len * wid * dep
            val looseVol = bankVol * (1.0 + (swellFactorPct / 100.0))

            if (isMetric) {
                val estimatedCost = bankVol * uPrice
                val detailsText = "Excavation: ${len}m x ${wid}m x ${dep}m with ${swellFactorPct}% Swell"
                val outputText = String.format("In-situ Volume: %.2f m³, Loose Volume: %.2f m³, Cost: $%.2f", bankVol, looseVol, estimatedCost)
                
                excavationResult = ExcavationResult(
                    bankVolume = bankVol,
                    looseVolume = looseVol,
                    imperialYardsBankVolume = null,
                    imperialYardsLooseVolume = null,
                    estimatedCost = estimatedCost,
                    details = detailsText
                )

                saveHistory(
                    calcType = "EXCAVATION",
                    title = "Excavation: ${String.format("%.1f m³", bankVol)} ($${String.format("%.2f", estimatedCost)})",
                    inputs = "$detailsText (Cost: $$uPrice/m³)",
                    outputs = outputText
                )
            } else {
                // Imperial values are in cubic feet. Convert to Cubic Yards (divide by 27!)
                val bankVolYds = bankVol / 27.0
                val looseVolYds = looseVol / 27.0
                val estimatedCost = bankVolYds * uPrice

                val detailsText = "Excavation: ${len}ft x ${wid}ft x ${dep}ft with ${swellFactorPct}% Swell"
                val outputText = String.format("In-situ Vol: %.2f ft³ (%.2f yd³), Loose Vol: %.2f ft³ (%.2f yd³), Cost: $%.2f", bankVol, bankVolYds, looseVol, looseVolYds, estimatedCost)
                
                excavationResult = ExcavationResult(
                    bankVolume = bankVol,
                    looseVolume = looseVol,
                    imperialYardsBankVolume = bankVolYds,
                    imperialYardsLooseVolume = looseVolYds,
                    estimatedCost = estimatedCost,
                    details = detailsText
                )

                saveHistory(
                    calcType = "EXCAVATION",
                    title = "Excavation: ${String.format("%.1f yd³", bankVolYds)} ($${String.format("%.2f", estimatedCost)})",
                    inputs = "$detailsText (Cost: $$uPrice/yd³)",
                    outputs = outputText
                )
            }
        } catch (e: Exception) {
            validationError = e.message ?: "Invalid inputs. Please verify."
        }
    }

    // Populate calculations from pre-existing history item clicked by user
    fun loadFromHistory(item: HistoryEntity) {
        isMetric = (item.unitSystem == "METRIC")
        activeCalcType = item.calcType
        activeTab = "CALCULATOR"
        
        validationError = "Loaded values from history log: \"${item.title}\""
    }

    private fun saveHistory(calcType: String, title: String, inputs: String, outputs: String) {
        viewModelScope.launch {
            val record = HistoryEntity(
                calcType = calcType,
                title = title,
                inputs = inputs,
                outputs = outputs,
                unitSystem = if (isMetric) "METRIC" else "IMPERIAL"
            )
            repository.insert(record)
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // Interactive Converter Logic
    fun updateConverterUnits() {
        val lists = when (converterCategory) {
            "LENGTH" -> listOf("m", "ft", "in", "yd", "mm")
            "AREA" -> listOf("m²", "ft²", "yd²")
            "VOLUME" -> listOf("m³", "ft³", "yd³", "L")
            "WEIGHT" -> listOf("kg", "lb", "t", "ton")
            else -> listOf("m", "ft")
        }
        converterFromUnit = lists[0]
        converterToUnit = lists[1]
        converterResultVal = ""
    }

    fun performQuickConversion() {
        try {
            val input = converterInputVal.toDoubleOrNull()
            if (input == null) {
                converterResultVal = "Please enter a valid number"
                return
            }

            val result = when (converterCategory) {
                "LENGTH" -> convertLength(input, converterFromUnit, converterToUnit)
                "AREA" -> convertArea(input, converterFromUnit, converterToUnit)
                "VOLUME" -> convertVolume(input, converterFromUnit, converterToUnit)
                "WEIGHT" -> convertWeight(input, converterFromUnit, converterToUnit)
                else -> 0.0
            }

            converterResultVal = String.format("%.4f %s", result, converterToUnit)
        } catch (e: Exception) {
            converterResultVal = "Error: Invalid operation"
        }
    }

    private fun convertLength(value: Double, from: String, to: String): Double {
        val inMeters = when (from) {
            "m" -> value
            "ft" -> value * 0.3048
            "in" -> value * 0.0254
            "yd" -> value * 0.9144
            "mm" -> value * 0.001
            else -> value
        }
        return when (to) {
            "m" -> inMeters
            "ft" -> inMeters / 0.3048
            "in" -> inMeters / 0.0254
            "yd" -> inMeters / 0.9144
            "mm" -> inMeters / 0.001
            else -> inMeters
        }
    }

    private fun convertArea(value: Double, from: String, to: String): Double {
        val inSqM = when (from) {
            "m²" -> value
            "ft²" -> value * 0.092903
            "yd²" -> value * 0.836127
            else -> value
        }
        return when (to) {
            "m²" -> inSqM
            "ft²" -> inSqM / 0.092903
            "yd²" -> inSqM / 0.836127
            else -> inSqM
        }
    }

    private fun convertVolume(value: Double, from: String, to: String): Double {
        val inLiters = when (from) {
            "L" -> value
            "m³" -> value * 1000.0
            "ft³" -> value * 28.3168
            "yd³" -> value * 764.555
            else -> value
        }
        return when (to) {
            "L" -> inLiters
            "m³" -> inLiters / 1000.0
            "ft³" -> inLiters / 28.3168
            "yd³" -> inLiters / 764.555
            else -> inLiters
        }
    }

    private fun convertWeight(value: Double, from: String, to: String): Double {
        val inKg = when (from) {
            "kg" -> value
            "lb" -> value * 0.45359237
            "t" -> value * 1000.0
            "ton" -> value * 907.18474
            else -> value
        }
        return when (to) {
            "kg" -> inKg
            "lb" -> inKg / 0.45359237
            "t" -> inKg / 1000.0
            "ton" -> inKg / 907.18474
            else -> inKg
        }
    }
}

class CalculatorViewModelFactory(private val repository: HistoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalculatorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
