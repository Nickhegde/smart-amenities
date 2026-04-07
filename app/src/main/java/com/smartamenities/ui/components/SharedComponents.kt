package com.smartamenities.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartamenities.data.model.*
import com.smartamenities.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// AmenityStatusChip — shows OPEN / CLOSED / OUT OF SERVICE / UNKNOWN
// Used on AmenityListScreen cards and AmenityDetailScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AmenityStatusChip(status: AmenityStatus) {
    val (color, label) = when (status) {
        AmenityStatus.OPEN           -> StatusOpen to "Open"
        AmenityStatus.CLOSED         -> StatusClosed to "Closed"
        AmenityStatus.OUT_OF_SERVICE -> StatusOutOfService to "Out of Service"
        AmenityStatus.UNKNOWN        -> StatusUnknown to "Status Unknown"
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.wrapContentSize()
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CrowdLevelChip — shows SHORT / MEDIUM / LONG wait
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CrowdLevelChip(crowdLevel: CrowdLevel) {
    val (color, label) = when (crowdLevel) {
        CrowdLevel.EMPTY   -> CrowdShort to "Empty"
        CrowdLevel.SHORT   -> CrowdShort to "Short Wait"
        CrowdLevel.MEDIUM  -> CrowdMedium to "Medium Wait"
        CrowdLevel.LONG    -> CrowdLong to "Long Wait"
        CrowdLevel.UNKNOWN -> CrowdUnknown to "Wait Unknown"
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.wrapContentSize()
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DataFreshnessIndicator — shows how old the status data is (FR 3.3)
// Also shows the confidence score when < 0.7 (FR 3.2 transparency)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DataFreshnessIndicator(
    timestampMillis: Long,
    confidenceScore: Float,
    modifier: Modifier = Modifier
) {
    // Ticks every 60 seconds so the label stays accurate without a ViewModel update.
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            value = System.currentTimeMillis()
        }
    }
    val ageMinutes = ((now - timestampMillis) / 60_000).toInt()
    val ageText = when {
        ageMinutes < 1  -> "Updated just now"
        ageMinutes < 60 -> "Updated $ageMinutes min ago"
        else            -> "Updated ${ageMinutes / 60}h ago"
    }

    // Low confidence = show warning (SRS NFR 3.2: transparency over false precision)
    val isStale = confidenceScore < 0.7f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isStale) Icons.Default.Warning else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isStale) StatusOutOfService else StatusOpen,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isStale) "$ageText · Low confidence" else ageText,
            style = MaterialTheme.typography.labelSmall,
            color = if (isStale) StatusOutOfService else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TimeToReachBadge — prominent walk time display (the primary sort key)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TimeToReachBadge(walkMinutes: Int, crowdLevel: CrowdLevel) {
    val total = walkMinutes + crowdLevel.waitEstimateMinutes
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$total",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "min",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AccessibilityBadgeRow — shows applicable accessibility icons for an amenity
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AccessibilityBadgeRow(amenity: Amenity) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (amenity.isWheelchairAccessible) {
            AccessibilityBadge(
                icon = Icons.Default.Accessible,
                label = "Wheelchair accessible",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        if (amenity.isStepFreeRoute) {
            AccessibilityBadge(
                icon = Icons.Default.Elevator,
                label = "Step-free route",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        if (amenity.isFamilyRestroom) {
            AccessibilityBadge(
                icon = Icons.Default.FamilyRestroom,
                label = "Family restroom",
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        if (amenity.isGenderNeutral) {
            AccessibilityBadge(
                icon = Icons.Outlined.Wc,
                label = "Gender neutral",
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun AccessibilityBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = tint.copy(alpha = 0.10f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier
                .padding(4.dp)
                .size(18.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AmenityTypeIcon — maps each AmenityType to a Material icon
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AmenityTypeIcon(type: AmenityType, modifier: Modifier = Modifier) {
    val icon = when (type) {
        AmenityType.RESTROOM               -> Icons.Default.Wc
        AmenityType.FAMILY_RESTROOM        -> Icons.Default.FamilyRestroom
        AmenityType.LACTATION_ROOM         -> Icons.Default.ChildFriendly
        AmenityType.GENDER_NEUTRAL_RESTROOM -> Icons.Default.Wc
        AmenityType.WATER_FOUNTAIN         -> Icons.Default.LocalDrink
    }
    Icon(
        imageVector = icon,
        contentDescription = type.displayName,
        modifier = modifier,
        tint = MaterialTheme.colorScheme.primary
    )
}
