package com.ev.ai.assistant.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ev.ai.assistant.ui.EvIcon
import com.ev.ai.assistant.ui.EvIconButton
import com.ev.ai.assistant.ui.SettingsCard
import com.ev.ai.assistant.ui.SettingsDivider
import com.ev.ai.assistant.ui.SettingsSectionTitle
import com.ev.ai.assistant.ui.util.OneUiColors
import com.ev.ai.assistant.ui.util.OneUiDimensions
import com.ev.ai.assistant.ui.util.OneUiElevation
import com.ev.ai.assistant.ui.util.OneUiMotion
import com.ev.ai.assistant.ui.util.OneUiShapes
import com.ev.ai.assistant.ui.util.OneUiTypography

private data class DynamicPermission(
    val permission: String,
    val label: String,
    val description: String,
    val isGranted: Boolean,
    val icon: EvIcon
)

@Composable
fun PermissionsScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()

    val bgColor = if (darkTheme) OneUiColors.BackgroundDark else OneUiColors.BackgroundLight
    val textPrimary = if (darkTheme) OneUiColors.TextPrimaryDark else OneUiColors.TextPrimaryLight
    val textSecondary = if (darkTheme) OneUiColors.TextSecondaryDark else OneUiColors.TextSecondaryLight

    // Auto-refresh when returning from System Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Dynamic Permission Request Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshTrigger++
    }

    // Query all declared permissions dynamically from PackageManager
    val permissions = remember(context, refreshTrigger) {
        loadRequestedPermissions(context)
    }

    val allowedPermissions = permissions.filter { it.isGranted }
    val notAllowedPermissions = permissions.filter { !it.isGranted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = OneUiDimensions.ScreenHorizontal)
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(OneUiDimensions.SettingsTopBarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EvIconButton(
                icon = EvIcon.Back,
                description = "Back to settings",
                onClick = onBack
            )
            Text(
                text = "Permissions",
                color = textPrimary,
                style = OneUiTypography.ScreenTitle,
                modifier = Modifier.padding(start = OneUiDimensions.Space3)
            )
        }

        // --- SUBTITLE DESCRIPTION ---
        Text(
            text = "E.V. uses these permissions to give you full functionality and help you complete tasks. You can change them in App info at any time.",
            color = textSecondary,
            style = OneUiTypography.Body,
            modifier = Modifier.padding(
                horizontal = OneUiDimensions.Space2,
                vertical = OneUiDimensions.Space3
            )
        )

        Spacer(modifier = Modifier.height(OneUiDimensions.Space3))

        // --- NOT ALLOWED SECTION ---
        if (notAllowedPermissions.isNotEmpty()) {
            SettingsSectionTitle("Not allowed")
            SettingsCard {
                notAllowedPermissions.forEachIndexed { index, perm ->
                    PermissionRow(
                        item = perm,
                        onClick = {
                            permissionLauncher.launch(perm.permission)
                        },
                        darkTheme = darkTheme
                    )
                    if (index < notAllowedPermissions.lastIndex) {
                        SettingsDivider()
                    }
                }
            }
            Spacer(modifier = Modifier.height(OneUiDimensions.Space5))
        }

        // --- ALLOWED SECTION ---
        if (allowedPermissions.isNotEmpty()) {
            SettingsSectionTitle("Allowed")
            SettingsCard {
                allowedPermissions.forEachIndexed { index, perm ->
                    PermissionRow(
                        item = perm,
                        onClick = {
                            openAppSettings(context)
                        },
                        darkTheme = darkTheme
                    )
                    if (index < allowedPermissions.lastIndex) {
                        SettingsDivider()
                    }
                }
            }
            Spacer(modifier = Modifier.height(OneUiDimensions.Space5))
        }

        // Fallback when no permissions are registered in the manifest
        if (permissions.isEmpty()) {
            SettingsCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(OneUiDimensions.CardContentVertical),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No permissions requested by E.V.",
                        color = textSecondary,
                        style = OneUiTypography.Body
                    )
                }
            }
            Spacer(modifier = Modifier.height(OneUiDimensions.Space5))
        }

        // --- MANAGE IN PHONE SETTINGS BUTTON ---
        Spacer(modifier = Modifier.height(OneUiDimensions.Space4))
        ManageSettingsButton(
            onClick = { openAppSettings(context) },
            darkTheme = darkTheme
        )

        Spacer(modifier = Modifier.height(OneUiDimensions.ContentBottom))
        Spacer(modifier = Modifier.height(OneUiDimensions.Space8))
    }
}

@Composable
private fun PermissionRow(
    item: DynamicPermission,
    onClick: () -> Unit,
    darkTheme: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val rowShape = OneUiShapes.ListItem

    val textPrimary = if (darkTheme) OneUiColors.TextPrimaryDark else OneUiColors.TextPrimaryLight
    val textSecondary = if (darkTheme) OneUiColors.TextSecondaryDark else OneUiColors.TextSecondaryLight

    val statusBadgeColor = if (item.isGranted) {
        if (darkTheme) OneUiColors.PrimaryActionDark else OneUiColors.BrandPrimary
    } else {
        if (darkTheme) OneUiColors.TextSecondaryDark else OneUiColors.TextSecondaryLight
    }

    val statusBgColor = if (item.isGranted) {
        statusBadgeColor.copy(alpha = if (darkTheme) 0.18f else 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick
            )
            .heightIn(min = OneUiDimensions.MinimumTouchTarget)
            .padding(
                horizontal = OneUiDimensions.ListItemHorizontal,
                vertical = OneUiDimensions.ListItemVertical
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading Icon Container
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(OneUiShapes.OuterContainer)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            EvIcon(
                icon = item.icon,
                modifier = Modifier.size(OneUiDimensions.IconSmall),
                tint = if (item.isGranted) statusBadgeColor else textSecondary
            )
        }

        // Title and Description
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = OneUiDimensions.Space3)
        ) {
            Text(
                text = item.label,
                color = textPrimary,
                style = OneUiTypography.BodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (item.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    color = textSecondary,
                    style = OneUiTypography.Caption,
                    lineHeight = 16.sp
                )
            }
        }

        // Status Badge Pill
        Box(
            modifier = Modifier
                .clip(OneUiShapes.Suggestion)
                .background(statusBgColor)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (item.isGranted) {
                    EvIcon(
                        icon = EvIcon.Check,
                        modifier = Modifier.size(12.dp),
                        tint = statusBadgeColor
                    )
                }
                Text(
                    text = if (item.isGranted) "Allowed" else "Not allowed",
                    color = statusBadgeColor,
                    style = OneUiTypography.Caption,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ManageSettingsButton(
    onClick: () -> Unit,
    darkTheme: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = OneUiMotion.PressSpring,
        label = "manage_settings_scale"
    )

    val shape = OneUiShapes.OuterContainer
    val buttonBg = if (darkTheme) {
        OneUiColors.SurfaceVariantDark.copy(alpha = 0.85f)
    } else {
        OneUiColors.SurfaceVariantLight
    }

    val buttonBorderColor = if (darkTheme) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color(0xFFE2E5E8)
    }

    val textPrimary = if (darkTheme) {
        OneUiColors.TextPrimaryDark
    } else {
        OneUiColors.TextPrimaryLight
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(buttonBg, shape)
            .border(
                width = OneUiElevation.Hairline,
                color = buttonBorderColor,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = "Manage permissions in phone settings" }
            .heightIn(min = OneUiDimensions.MinimumTouchTarget)
            .padding(
                horizontal = OneUiDimensions.Space6,
                vertical = OneUiDimensions.Space3
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OneUiDimensions.Space2)
        ) {
            Text(
                text = "Manage in phone Settings",
                color = textPrimary,
                style = OneUiTypography.Label,
                fontWeight = FontWeight.Medium
            )
            EvIcon(
                icon = EvIcon.ChevronRight,
                modifier = Modifier.size(OneUiDimensions.IconSmall),
                tint = textPrimary.copy(alpha = 0.6f)
            )
        }
    }
}

private fun openAppSettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback
    }
}

/**
 * Dynamically queries all declared permissions from the app's manifest via PackageManager,
 * resolves system labels, descriptions, and current grant state without hardcoding.
 */
private fun loadRequestedPermissions(context: Context): List<DynamicPermission> {
    val pm = context.packageManager

    val requested = try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        }
        packageInfo.requestedPermissions?.toList() ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    return requested.map { permString ->
        val permInfo = try {
            pm.getPermissionInfo(permString, 0)
        } catch (_: Exception) {
            null
        }

        // Dynamic human-readable label from system
        val rawLabel = permInfo?.loadLabel(pm)?.toString()?.trim()
        val label = if (!rawLabel.isNullOrBlank()) {
            rawLabel.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            permString.substringAfterLast(".").replace("_", " ").lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        // Dynamic description from system, with contextual assistant fallback
        val rawDesc = permInfo?.loadDescription(pm)?.toString()?.trim()
        val description = if (!rawDesc.isNullOrBlank()) {
            rawDesc
        } else {
            getPermissionFallbackDescription(permString)
        }

        val isGranted = ContextCompat.checkSelfPermission(
            context,
            permString
        ) == PackageManager.PERMISSION_GRANTED

        val icon = resolvePermissionIcon(permString)

        DynamicPermission(
            permission = permString,
            label = label,
            description = description,
            isGranted = isGranted,
            icon = icon
        )
    }
}

private fun getPermissionFallbackDescription(permission: String): String {
    return when {
        permission.contains("RECORD_AUDIO", ignoreCase = true) ->
            "Record audio to listen to your voice and process voice commands."
        permission.contains("CAMERA", ignoreCase = true) ->
            "Take pictures and video to analyze visual assistant queries."
        permission.contains("LOCATION", ignoreCase = true) ->
            "Access device location for weather, maps, and nearby recommendations."
        permission.contains("CONTACTS", ignoreCase = true) ->
            "Access contacts to make calls or send messages on your behalf."
        permission.contains("NOTIFICATION", ignoreCase = true) ->
            "Display assistant updates, reminders, and status notifications."
        else ->
            "Required to provide full assistant functionality on your device."
    }
}

private fun resolvePermissionIcon(permission: String): EvIcon {
    return when {
        permission.contains("AUDIO", ignoreCase = true) -> EvIcon.Microphone
        permission.contains("CONTACT", ignoreCase = true) ||
            permission.contains("ACCOUNT", ignoreCase = true) -> EvIcon.Account
        else -> EvIcon.Info
    }
}
