package com.eazpire.creator.wear.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.eazpire.creator.wear.EazColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class WearJobLoaderStyle(val label: String) {
    PixelRing("Pixel ring"),
    OrbitDots("Orbit dots"),
    PulseGrid("Pulse grid"),
    SpinDiamond("Spin diamond"),
    DnaScroll("DNA scroll"),
    RadarSweep("Radar sweep"),
    BounceRow("Bounce row"),
    CornerMorph("Corner morph"),
    EazSpiral("Eaz spiral"),
    EazSpiralTwin("Spiral twin"),
    EazSpiralPulse("Spiral pulse"),
    EazSpiralReverse("Spiral reverse"),
    EazSpiralTight("Spiral tight"),
    EazSpiralBloom("Spiral bloom"),
    ScanGlitch("Scan glitch"),
    PixelForge("Pixel forge"),
    PixelBloom("Pixel bloom"),
    PixelCascade("Pixel cascade"),
    PixelMatrix("Pixel matrix"),
    PixelSpark("Pixel spark"),
}

@Composable
fun WearJobLoadingAnimation(
    style: WearJobLoaderStyle,
    modifier: Modifier = Modifier,
    primary: Color = EazColors.Orange,
    secondary: Color = EazColors.TextPrimary.copy(alpha = 0.35f),
) {
    // Always apply size here — callers passing only padding must not drop the default bounds.
    val sizedModifier = modifier.size(72.dp)
    when (style) {
        WearJobLoaderStyle.PixelRing -> PixelRingLoader(sizedModifier, primary, secondary)
        WearJobLoaderStyle.OrbitDots -> OrbitDotsLoader(sizedModifier, primary, secondary)
        WearJobLoaderStyle.PulseGrid -> PulseGridLoader(sizedModifier, primary, secondary)
        WearJobLoaderStyle.SpinDiamond -> SpinDiamondLoader(sizedModifier, primary, secondary)
        WearJobLoaderStyle.DnaScroll -> DnaScrollLoader(sizedModifier, primary, secondary)
        WearJobLoaderStyle.RadarSweep -> RadarSweepLoader(sizedModifier, primary, secondary)
        WearJobLoaderStyle.BounceRow -> BounceRowLoader(sizedModifier, primary, secondary)
        WearJobLoaderStyle.CornerMorph -> CornerMorphLoader(sizedModifier, primary, secondary)
        WearJobLoaderStyle.EazSpiral -> EazSpiralLoader(sizedModifier, primary, secondary, SpiralParams())
        WearJobLoaderStyle.EazSpiralTwin -> EazSpiralLoader(
            sizedModifier,
            primary,
            secondary,
            SpiralParams(dual = true, speedMs = 2400),
        )
        WearJobLoaderStyle.EazSpiralPulse -> EazSpiralLoader(
            sizedModifier,
            primary,
            secondary,
            SpiralParams(pulseRadius = true, speedMs = 2200),
        )
        WearJobLoaderStyle.EazSpiralReverse -> EazSpiralLoader(
            sizedModifier,
            primary,
            secondary,
            SpiralParams(reverse = true, speedMs = 2800),
        )
        WearJobLoaderStyle.EazSpiralTight -> EazSpiralLoader(
            sizedModifier,
            primary,
            secondary,
            SpiralParams(arms = 22, turns = 3f, dotScale = 0.028f, speedMs = 1800),
        )
        WearJobLoaderStyle.EazSpiralBloom -> EazSpiralLoader(
            sizedModifier,
            primary,
            secondary,
            SpiralParams(bloom = true, speedMs = 3000),
        )
        WearJobLoaderStyle.ScanGlitch -> ScanGlitchLoader(sizedModifier, primary, secondary)
        WearJobLoaderStyle.PixelForge -> PixelForgeLoader(sizedModifier, primary, secondary)
        WearJobLoaderStyle.PixelBloom -> PixelBloomLoader(sizedModifier, primary, secondary)
        WearJobLoaderStyle.PixelCascade -> PixelCascadeLoader(sizedModifier, primary, secondary)
        WearJobLoaderStyle.PixelMatrix -> PixelMatrixLoader(sizedModifier, primary, secondary)
        WearJobLoaderStyle.PixelSpark -> PixelSparkLoader(sizedModifier, primary, secondary)
    }
}

private data class SpiralParams(
    val arms: Int = 16,
    val turns: Float = 2f,
    val speedMs: Int = 2600,
    val reverse: Boolean = false,
    val pulseRadius: Boolean = false,
    val dual: Boolean = false,
    val bloom: Boolean = false,
    val dotScale: Float = 0.035f,
)

@Composable
private fun rememberLoaderPhase(durationMs: Int, label: String = "phase"): Float {
    val transition = rememberInfiniteTransition(label = label)
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = label,
    )
    return phase
}

@Composable
private fun PixelRingLoader(modifier: Modifier, primary: Color, secondary: Color) {
    val phase = rememberLoaderPhase(2400)
    Canvas(modifier) {
        val n = 12
        val r = size.minDimension * 0.36f
        val px = size.minDimension * 0.07f
        for (i in 0 until n) {
            val a = (i / n.toFloat()) * 2f * PI.toFloat() + phase * 2f * PI.toFloat()
            val lit = ((i + (phase * n).toInt()) % n) < 4
            drawRect(
                color = if (lit) primary else secondary,
                topLeft = Offset(
                    center.x + cos(a) * r - px / 2f,
                    center.y + sin(a) * r - px / 2f,
                ),
                size = Size(px, px),
            )
        }
    }
}

@Composable
private fun OrbitDotsLoader(modifier: Modifier, primary: Color, secondary: Color) {
    val phase = rememberLoaderPhase(2000)
    Canvas(modifier) {
        drawCircle(secondary, radius = size.minDimension * 0.08f, center = center)
        val n = 6
        for (i in 0 until n) {
            val a = phase * 2f * PI.toFloat() + (i / n.toFloat()) * 2f * PI.toFloat()
            val r = size.minDimension * 0.32f
            drawCircle(
                color = primary.copy(alpha = 0.45f + 0.55f * ((i + 1) / n.toFloat())),
                radius = size.minDimension * 0.05f,
                center = Offset(center.x + cos(a) * r, center.y + sin(a) * r),
            )
        }
    }
}

@Composable
private fun PulseGridLoader(modifier: Modifier, primary: Color, secondary: Color) {
    val phase = rememberLoaderPhase(1800)
    Canvas(modifier) {
        val cols = 5
        val cell = size.minDimension / (cols + 2f)
        val ox = center.x - (cols * cell) / 2f + cell / 2f
        val oy = center.y - (cols * cell) / 2f + cell / 2f
        for (y in 0 until cols) {
            for (x in 0 until cols) {
                val idx = x + y * cols
                val wave = sin((phase * 2f * PI.toFloat()) + idx * 0.55f) * 0.5f + 0.5f
                drawRect(
                    color = primary.copy(alpha = 0.25f + 0.75f * wave),
                    topLeft = Offset(ox + x * cell - cell * 0.35f, oy + y * cell - cell * 0.35f),
                    size = Size(cell * 0.7f, cell * 0.7f),
                )
            }
        }
    }
}

@Composable
private fun SpinDiamondLoader(modifier: Modifier, primary: Color, secondary: Color) {
    val spin = rememberLoaderPhase(1600)
    Canvas(modifier) {
        rotate(spin * 360f, center) {
            val s = size.minDimension * 0.22f
            val path = Path().apply {
                moveTo(center.x, center.y - s)
                lineTo(center.x + s, center.y)
                lineTo(center.x, center.y + s)
                lineTo(center.x - s, center.y)
                close()
            }
            drawPath(path, primary)
            drawPath(path, secondary, style = Stroke(width = 2f))
        }
    }
}

@Composable
private fun DnaScrollLoader(modifier: Modifier, primary: Color, secondary: Color) {
    val phase = rememberLoaderPhase(2200)
    Canvas(modifier) {
        val rows = 7
        val step = size.minDimension / (rows + 2f)
        for (i in 0 until rows) {
            val t = i / rows.toFloat()
            val y = center.y - (rows * step) / 2f + i * step
            val xOff = sin(phase * 2f * PI.toFloat() + t * PI.toFloat() * 2f) * step * 0.9f
            drawCircle(primary, step * 0.18f, Offset(center.x - xOff, y))
            drawCircle(secondary, step * 0.14f, Offset(center.x + xOff, y))
        }
    }
}

@Composable
private fun RadarSweepLoader(modifier: Modifier, primary: Color, secondary: Color) {
    val phase = rememberLoaderPhase(2400)
    Canvas(modifier) {
        val r = size.minDimension * 0.38f
        drawCircle(color = secondary, radius = r, center = center, style = Stroke(width = 2f))
        drawCircle(color = secondary.copy(alpha = 0.5f), radius = r * 0.55f, center = center, style = Stroke(width = 1f))
        val sweep = phase * 2f * PI.toFloat()
        drawArc(
            color = primary.copy(alpha = 0.25f),
            startAngle = Math.toDegrees(sweep.toDouble()).toFloat(),
            sweepAngle = 70f,
            useCenter = true,
            topLeft = Offset(center.x - r, center.y - r),
            size = Size(r * 2f, r * 2f),
        )
        val lx = center.x + cos(sweep) * r
        val ly = center.y + sin(sweep) * r
        drawLine(primary, center, Offset(lx, ly), strokeWidth = 3f, cap = StrokeCap.Round)
        drawCircle(primary, size.minDimension * 0.06f, Offset(lx, ly))
    }
}

@Composable
private fun BounceRowLoader(modifier: Modifier, primary: Color, secondary: Color) {
    val phase = rememberLoaderPhase(1200)
    Canvas(modifier) {
        val n = 5
        val gap = size.minDimension / (n + 2f)
        for (i in 0 until n) {
            val bounce = sin(phase * 2f * PI.toFloat() + i * 0.9f) * 0.5f + 0.5f
            val r = size.minDimension * (0.06f + 0.04f * bounce)
            drawCircle(
                color = if (i == (phase * n).toInt() % n) primary else secondary,
                radius = r,
                center = Offset(
                    center.x - (n - 1) * gap / 2f + i * gap,
                    center.y - bounce * size.minDimension * 0.12f,
                ),
            )
        }
    }
}

@Composable
private fun CornerMorphLoader(modifier: Modifier, primary: Color, secondary: Color) {
    val phase = rememberLoaderPhase(2000)
    Canvas(modifier) {
        val s = size.minDimension * 0.28f
        val morph = sin(phase * 2f * PI.toFloat()) * 0.5f + 0.5f
        val inset = s * morph * 0.35f
        drawRoundFrame(center, s, primary, inset)
        drawRoundFrame(center, s * 0.65f, secondary, inset * 0.5f)
    }
}

private fun DrawScope.drawRoundFrame(center: Offset, half: Float, color: Color, corner: Float) {
    val path = Path().apply {
        moveTo(center.x - half + corner, center.y - half)
        lineTo(center.x + half - corner, center.y - half)
        lineTo(center.x + half, center.y - half + corner)
        lineTo(center.x + half, center.y + half - corner)
        lineTo(center.x + half - corner, center.y + half)
        lineTo(center.x - half + corner, center.y + half)
        lineTo(center.x - half, center.y + half - corner)
        lineTo(center.x - half, center.y - half + corner)
        close()
    }
    drawPath(path, color, style = Stroke(width = 2.5f))
}

@Composable
private fun EazSpiralLoader(
    modifier: Modifier,
    primary: Color,
    secondary: Color,
    params: SpiralParams,
) {
    val phase = rememberLoaderPhase(params.speedMs, "spiral-${params.arms}")
    Canvas(modifier) {
        fun drawArmSet(phaseOffset: Float, alphaScale: Float) {
            val spin = (if (params.reverse) -phase else phase) * 2f * PI.toFloat() + phaseOffset
            val breathe =
                if (params.pulseRadius) sin(phase * 2f * PI.toFloat()) * 0.06f + 1f
                else 1f
            val bloomCore =
                if (params.bloom) sin(phase * 2f * PI.toFloat()) * 0.5f + 0.5f
                else 0f
            for (i in 0 until params.arms) {
                val t = i / params.arms.toFloat()
                val a = t * params.turns * 2f * PI.toFloat() + spin
                val r = size.minDimension * (0.08f + t * 0.28f) * breathe
                val alpha = (0.25f + 0.75f * (1f - t)) * alphaScale
                val dotR = size.minDimension * params.dotScale * (1f + bloomCore * t * 0.35f)
                drawCircle(
                    color = primary.copy(alpha = alpha.coerceIn(0.15f, 1f)),
                    radius = dotR,
                    center = Offset(center.x + cos(a) * r, center.y + sin(a) * r),
                )
            }
        }
        if (params.dual) {
            drawArmSet(0f, 1f)
            drawArmSet(PI.toFloat(), 0.55f)
        } else {
            drawArmSet(0f, 1f)
        }
        val coreR = size.minDimension * (if (params.bloom) 0.05f + 0.03f * sin(phase * 2f * PI.toFloat()) else 0.05f)
        drawCircle(
            if (params.bloom) primary.copy(alpha = 0.85f) else secondary,
            coreR,
            center,
        )
    }
}

@Composable
private fun PixelForgeLoader(modifier: Modifier, primary: Color, secondary: Color) {
    val phase = rememberLoaderPhase(2200, "pixel-forge")
    Canvas(modifier) {
        val grid = 7
        val cell = size.minDimension / (grid + 1.2f)
        val px = cell * 0.72f
        val cx = (grid - 1) / 2f
        val cy = (grid - 1) / 2f
        val head = phase * (grid * 2f + 4f)
        for (y in 0 until grid) {
            for (x in 0 until grid) {
                val dist = kotlin.math.abs(x - cx) + kotlin.math.abs(y - cy)
                val edge = head - dist
                val lit = edge in 0f..1.35f
                val ox = center.x - (grid * cell) / 2f + x * cell
                val oy = center.y - (grid * cell) / 2f + y * cell
                drawRect(
                    color = if (lit) primary else secondary.copy(alpha = 0.18f),
                    topLeft = Offset(ox - px / 2f, oy - px / 2f),
                    size = Size(px, px),
                )
            }
        }
    }
}

@Composable
private fun PixelBloomLoader(modifier: Modifier, primary: Color, secondary: Color) {
    val phase = rememberLoaderPhase(2000, "pixel-bloom")
    Canvas(modifier) {
        val rings = 5
        val maxR = size.minDimension * 0.38f
        val px = size.minDimension * 0.065f
        for (ring in 0 until rings) {
            val t = ring / rings.toFloat()
            val r = maxR * t
            val n = 6 + ring * 3
            val ringPhase = (phase * 2f * PI.toFloat() + t * PI.toFloat()).let { v ->
                sin(v) * 0.5f + 0.5f
            }
            for (i in 0 until n) {
                val a = (i / n.toFloat()) * 2f * PI.toFloat()
                val lit = ringPhase > 0.35f || (phase * n + i).toInt() % 3 == 0
                drawRect(
                    color = if (lit) primary.copy(alpha = 0.4f + 0.6f * ringPhase) else secondary.copy(alpha = 0.15f),
                    topLeft = Offset(
                        center.x + cos(a) * r - px / 2f,
                        center.y + sin(a) * r - px / 2f,
                    ),
                    size = Size(px, px),
                )
            }
        }
        drawRect(
            primary,
            topLeft = Offset(center.x - px, center.y - px),
            size = Size(px * 2f, px * 2f),
        )
    }
}

@Composable
private fun PixelCascadeLoader(modifier: Modifier, primary: Color, secondary: Color) {
    val phase = rememberLoaderPhase(1600, "pixel-cascade")
    Canvas(modifier) {
        val cols = 9
        val rows = 9
        val cell = size.minDimension / (cols + 1f)
        val px = cell * 0.75f
        val head = phase * (rows + 2f)
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val trail = head - y
                val lit = trail in 0f..1.4f
                val core = trail in 0.6f..1.4f
                val ox = center.x - (cols * cell) / 2f + x * cell
                val oy = center.y - (rows * cell) / 2f + y * cell
                drawRect(
                    color = when {
                        core -> primary
                        lit -> primary.copy(alpha = 0.45f)
                        else -> secondary.copy(alpha = 0.12f)
                    },
                    topLeft = Offset(ox - px / 2f, oy - px / 2f),
                    size = Size(px, px),
                )
            }
        }
    }
}

@Composable
private fun PixelMatrixLoader(modifier: Modifier, primary: Color, secondary: Color) {
    val phase = rememberLoaderPhase(1400, "pixel-matrix")
    Canvas(modifier) {
        val cols = 8
        val rows = 8
        val cell = size.minDimension / (cols + 0.5f)
        val px = cell * 0.7f
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val idx = x + y * cols
                val spark = sin(phase * 2f * PI.toFloat() * 2.2f + idx * 0.71f) * 0.5f + 0.5f
                val scan = sin(phase * 2f * PI.toFloat() + y * 0.4f) > 0.1
                val lit = spark > 0.62f && scan
                val ox = center.x - (cols * cell) / 2f + x * cell
                val oy = center.y - (rows * cell) / 2f + y * cell
                drawRect(
                    color = if (lit) primary else secondary.copy(alpha = 0.1f + spark * 0.15f),
                    topLeft = Offset(ox - px / 2f, oy - px / 2f),
                    size = Size(px, px),
                )
            }
        }
    }
}

@Composable
private fun PixelSparkLoader(modifier: Modifier, primary: Color, secondary: Color) {
    val phase = rememberLoaderPhase(1900, "pixel-spark")
    Canvas(modifier) {
        val px = size.minDimension * 0.08f
        val corePulse = sin(phase * 2f * PI.toFloat()) * 0.5f + 0.5f
        drawRect(
            primary,
            topLeft = Offset(center.x - px * (0.9f + corePulse * 0.4f), center.y - px * (0.9f + corePulse * 0.4f)),
            size = Size(px * (1.8f + corePulse * 0.8f), px * (1.8f + corePulse * 0.8f)),
        )
        val orbit = 8
        for (i in 0 until orbit) {
            val a = phase * 2f * PI.toFloat() + (i / orbit.toFloat()) * 2f * PI.toFloat()
            val r = size.minDimension * (0.22f + 0.06f * sin(phase * 2f * PI.toFloat() + i))
            val trail = (i + (phase * orbit).toInt()) % orbit
            drawRect(
                color = if (trail < 3) primary else secondary.copy(alpha = 0.35f),
                topLeft = Offset(
                    center.x + cos(a) * r - px / 2f,
                    center.y + sin(a) * r - px / 2f,
                ),
                size = Size(px, px),
            )
        }
        val sparks = 4
        for (s in 0 until sparks) {
            val t = (s / sparks.toFloat() + phase) % 1f
            val sx = center.x + (t - 0.5f) * size.width * 0.5f
            val sy = center.y - size.height * 0.35f + t * size.height * 0.7f
            if (sin(phase * 2f * PI.toFloat() * 4f + s * 2.1f) > 0.3f) {
                drawRect(primary.copy(alpha = 0.7f), topLeft = Offset(sx - px / 3f, sy - px / 3f), size = Size(px * 0.66f, px * 0.66f))
            }
        }
    }
}

@Composable
private fun ScanGlitchLoader(modifier: Modifier, primary: Color, secondary: Color) {
    val phase = rememberLoaderPhase(1800)
    Canvas(modifier) {
        val rows = 9
        val h = size.height / rows
        for (i in 0 until rows) {
            val flicker = sin(phase * 2f * PI.toFloat() * 3f + i * 1.2f) > 0.2
            if (flicker) {
                val wobble = sin(phase * 2f * PI.toFloat() + i) * size.width * 0.08f
                drawRect(
                    color = if (i % 3 == 0) primary else primary.copy(alpha = 0.45f),
                    topLeft = Offset(wobble, i * h),
                    size = Size(size.width - kotlin.math.abs(wobble), h * 0.65f),
                )
            } else {
                drawRect(
                    color = secondary.copy(alpha = 0.2f),
                    topLeft = Offset(0f, i * h),
                    size = Size(size.width, h * 0.35f),
                )
            }
        }
        val scanY = phase * size.height
        drawLine(
            color = EazColors.TextPrimary.copy(alpha = 0.85f),
            start = Offset(0f, scanY),
            end = Offset(size.width, scanY),
            strokeWidth = 2f,
        )
    }
}
