package eu.darken.myperm.common.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val LucideRadar: ImageVector
    get() {
        if (_LucideRadar != null) return _LucideRadar!!

        _LucideRadar = ImageVector.Builder(
            name = "radar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19.07f, 4.93f)
                arcTo(10f, 10f, 0f, false, false, 6.99f, 3.34f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 6f)
                horizontalLineToRelative(0.01f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2.29f, 9.62f)
                arcTo(10f, 10f, 0f, true, false, 21.31f, 8.35f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.24f, 7.76f)
                arcTo(6f, 6f, 0f, true, false, 8.23f, 16.67f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 18f)
                horizontalLineToRelative(0.01f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17.99f, 11.66f)
                arcTo(6f, 6f, 0f, false, true, 15.77f, 16.67f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 12f)
                arcTo(2f, 2f, 0f, false, true, 12f, 14f)
                arcTo(2f, 2f, 0f, false, true, 10f, 12f)
                arcTo(2f, 2f, 0f, false, true, 14f, 12f)
                close()
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13.41f, 10.59f)
                lineToRelative(5.66f, -5.66f)
            }
        }.build()

        return _LucideRadar!!
    }

private var _LucideRadar: ImageVector? = null

val DiscordIcon: ImageVector
    get() {
        if (_DiscordIcon != null) return _DiscordIcon!!

        _DiscordIcon = ImageVector.Builder(
            name = "discord",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
            ) {
                moveTo(20.317f, 4.37f)
                arcToRelative(19.791f, 19.791f, 0f, false, false, -4.885f, -1.515f)
                arcToRelative(0.074f, 0.074f, 0f, false, false, -0.079f, 0.037f)
                curveToRelative(-0.211f, 0.375f, -0.445f, 0.865f, -0.608f, 1.25f)
                arcToRelative(18.27f, 18.27f, 0f, false, false, -5.487f, 0f)
                curveToRelative(-0.164f, -0.393f, -0.406f, -0.874f, -0.618f, -1.25f)
                arcToRelative(0.077f, 0.077f, 0f, false, false, -0.079f, -0.037f)
                arcToRelative(19.736f, 19.736f, 0f, false, false, -4.885f, 1.515f)
                arcToRelative(0.07f, 0.07f, 0f, false, false, -0.032f, 0.028f)
                curveTo(0.533f, 9.046f, -0.319f, 13.58f, 0.099f, 18.058f)
                arcToRelative(0.082f, 0.082f, 0f, false, false, 0.031f, 0.056f)
                curveToRelative(2.053f, 1.508f, 4.041f, 2.423f, 5.993f, 3.03f)
                arcToRelative(0.078f, 0.078f, 0f, false, false, 0.084f, -0.028f)
                curveToRelative(0.462f, -0.63f, 0.873f, -1.295f, 1.226f, -1.994f)
                arcToRelative(0.076f, 0.076f, 0f, false, false, -0.042f, -0.106f)
                arcToRelative(13.107f, 13.107f, 0f, false, true, -1.872f, -0.892f)
                arcToRelative(0.077f, 0.077f, 0f, false, true, -0.008f, -0.128f)
                curveToRelative(0.126f, -0.094f, 0.252f, -0.192f, 0.372f, -0.291f)
                arcToRelative(0.074f, 0.074f, 0f, false, true, 0.078f, -0.01f)
                curveToRelative(3.928f, 1.793f, 8.18f, 1.793f, 12.062f, 0f)
                arcToRelative(0.074f, 0.074f, 0f, false, true, 0.078f, 0.01f)
                curveToRelative(0.12f, 0.099f, 0.246f, 0.198f, 0.373f, 0.292f)
                arcToRelative(0.077f, 0.077f, 0f, false, true, -0.007f, 0.128f)
                arcToRelative(12.299f, 12.299f, 0f, false, true, -1.873f, 0.891f)
                arcToRelative(0.077f, 0.077f, 0f, false, false, -0.041f, 0.107f)
                curveToRelative(0.36f, 0.698f, 0.772f, 1.363f, 1.225f, 1.993f)
                arcToRelative(0.076f, 0.076f, 0f, false, false, 0.084f, 0.029f)
                curveToRelative(1.961f, -0.607f, 3.95f, -1.522f, 6.002f, -3.029f)
                arcToRelative(0.077f, 0.077f, 0f, false, false, 0.031f, -0.055f)
                curveToRelative(0.5f, -5.177f, -0.838f, -9.674f, -3.549f, -13.66f)
                arcToRelative(0.061f, 0.061f, 0f, false, false, -0.031f, -0.029f)
                close()
                moveTo(8.02f, 15.331f)
                curveToRelative(-1.183f, 0f, -2.157f, -1.086f, -2.157f, -2.419f)
                curveToRelative(0f, -1.333f, 0.956f, -2.419f, 2.157f, -2.419f)
                curveToRelative(1.21f, 0f, 2.176f, 1.095f, 2.157f, 2.419f)
                curveToRelative(0f, 1.333f, -0.956f, 2.419f, -2.157f, 2.419f)
                close()
                moveTo(15.995f, 15.331f)
                curveToRelative(-1.182f, 0f, -2.157f, -1.086f, -2.157f, -2.419f)
                curveToRelative(0f, -1.333f, 0.955f, -2.419f, 2.157f, -2.419f)
                curveToRelative(1.21f, 0f, 2.176f, 1.095f, 2.157f, 2.419f)
                curveToRelative(0f, 1.333f, -0.946f, 2.419f, -2.157f, 2.419f)
                close()
            }
        }.build()

        return _DiscordIcon!!
    }

private var _DiscordIcon: ImageVector? = null

@Composable
private fun IconPreviewRow(icon: ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview2
@Composable
private fun LucideRadarPreview() = PreviewWrapper {
    IconPreviewRow(LucideRadar, "LucideRadar")
}

@Preview2
@Composable
private fun DiscordIconPreview() = PreviewWrapper {
    IconPreviewRow(DiscordIcon, "DiscordIcon")
}
