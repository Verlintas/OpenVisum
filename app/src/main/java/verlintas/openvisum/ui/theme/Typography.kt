package verlintas.openvisum.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Base = Typography()

val OpenVisumTypography: Typography = Base.copy(
    headlineSmall = Base.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = Base.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = Base.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = Base.titleSmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
    ),
    labelLarge = Base.labelLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = Base.labelMedium.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = Base.labelSmall.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
    ),
    bodyLarge = Base.bodyLarge.copy(
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = Base.bodyMedium.copy(
        lineHeight = 21.sp,
        letterSpacing = 0.15.sp,
    ),
)

val OpenVisumShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)
