package com.talhayun.adkan.ui.blocking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.talhayun.adkan.ui.theme.BrandGreen

/**
 * Real app-selection picker: a checklist of actually-installed launchable
 * apps (see installedLaunchableApps), backed by a local mutable selection
 * set the caller persists on confirm. Not a full FamilyControls-style
 * per-category picker — package-name granularity only, which is what
 * UsageStatsManager/blocking on Android works with anyway.
 */
@Composable
fun AppPickerDialog(
    apps: List<AppInfo>,
    initiallySelected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    var selected by remember { mutableStateOf(initiallySelected) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
        ) {
            Text(
                text = "בחירת אפליקציות לחסימה",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )

            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = selected.contains(app.packageName),
                            onCheckedChange = { checked ->
                                selected = if (checked) {
                                    selected + app.packageName
                                } else {
                                    selected - app.packageName
                                }
                            },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = BrandGreen),
                        )
                        Text(text = app.label, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = "ביטול")
                }
                TextButton(onClick = { onConfirm(selected) }) {
                    Text(text = "שמירה", color = BrandGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
