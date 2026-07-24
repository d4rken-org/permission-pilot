package eu.darken.myperm.common.upgrade.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Code
import androidx.compose.material.icons.twotone.Favorite
import androidx.compose.material.icons.twotone.FileDownload
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material.icons.twotone.Palette
import androidx.compose.material.icons.twotone.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.PermPilotMascot

private data class Benefit(val icon: ImageVector, val textRes: Int)

private val upgradeBenefits = listOf(
    Benefit(Icons.TwoTone.Palette, R.string.upgrade_benefit_themes),
    Benefit(Icons.TwoTone.Tune, R.string.upgrade_benefit_filtering),
    Benefit(Icons.TwoTone.FileDownload, R.string.upgrade_benefit_export),
    Benefit(Icons.TwoTone.Notifications, R.string.upgrade_benefit_monitoring),
    Benefit(Icons.TwoTone.Code, R.string.upgrade_benefit_manifest_viewer),
    Benefit(Icons.TwoTone.Favorite, R.string.upgrade_benefit_support),
)

// The mascot-in-a-circle header plus the "prefix Suffix" headline, shared by both flavors. Colors are
// passed in so each flavor keeps its own palette (gplay tints the circle/suffix differently to foss).
@Composable
internal fun UpgradeMascotHeader(
    circleColor: Color,
    suffixColor: Color,
    titleColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = circleColor,
            ) {}
            PermPilotMascot(size = 80.dp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.upgrade_title_prefix))
                append(" ")
                withStyle(SpanStyle(color = suffixColor, fontWeight = FontWeight.Bold)) {
                    append(stringResource(R.string.upgrade_title_suffix))
                }
            },
            style = MaterialTheme.typography.headlineLarge,
            color = titleColor,
        )
    }
}

// The list of Pro benefits with icon chips. The chip colors are flavor-provided so the card matches
// the surrounding palette.
@Composable
internal fun UpgradeBenefitsCard(
    chipColor: Color,
    chipContentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            upgradeBenefits.forEach { benefit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = chipColor,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = benefit.icon,
                                contentDescription = null,
                                tint = chipContentColor,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(benefit.textRes),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

// A simple tinted preamble card. The body text is passed as a resolved String so the composable stays
// flavor-agnostic (the two flavors use different preamble strings).
@Composable
internal fun UpgradePreambleCard(
    text: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}
