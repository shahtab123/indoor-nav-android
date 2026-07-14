/*
 * Copyright 2026 Shahtab
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.kotlin.helloar

import android.opengl.GLSurfaceView
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ar.core.examples.kotlin.helloar.ui.AppMode
import com.google.ar.core.examples.kotlin.helloar.ui.NavTurn
import com.google.ar.core.examples.kotlin.helloar.ui.PlacementPreview
import com.google.ar.core.examples.kotlin.helloar.waypoints.UiEdge
import com.google.ar.core.examples.kotlin.helloar.waypoints.UiMarker
import com.google.ar.core.examples.kotlin.helloar.waypoints.WaypointType
import kotlin.math.roundToInt

/** Full-screen AR UI with Map / Navigate modes (one app, two jobs). */
@Composable
fun IndoorNavApp(
  activity: HelloArActivity,
  surfaceView: GLSurfaceView,
) {
  Box(modifier = Modifier.fillMaxSize()) {
    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = {
        (surfaceView.parent as? ViewGroup)?.removeView(surfaceView)
        surfaceView
      },
    )
    ArViewOverlay(activity = activity)
  }
}

@Composable
private fun ArViewOverlay(activity: HelloArActivity) {
  val pendingPose = activity.pendingPlacementPose
  val pendingType = activity.pendingPlacementType
  val preview = activity.placementPreview
  val revision = activity.waypointRevision
  val graphRevision = activity.graphRevision
  val appMode = activity.appModeState
  val destinationId = activity.selectedDestinationId
  val startId = activity.selectedStartId
  val pathSummary = activity.pathSummary
  var markingType by remember { mutableStateOf(WaypointType.ROOM) }
  var waypointName by remember(pendingPose) { mutableStateOf("") }
  var showClearConfirm by remember { mutableStateOf(false) }
  var showSavedList by remember { mutableStateOf(false) }
  var showConnections by remember { mutableStateOf(false) }
  val waypoints = remember(revision) { activity.markersForUi() }
  val edges = remember(graphRevision, revision) { activity.edgesForUi() }

  if (pendingPose != null && appMode == AppMode.MAP) {
    AlertDialog(
      onDismissRequest = { activity.dismissWaypointNaming() },
      title = { Text("Name this ${pendingType.label.lowercase()}") },
      text = {
        OutlinedTextField(
          value = waypointName,
          onValueChange = { waypointName = it },
          singleLine = true,
          label = {
            Text(
              when (pendingType) {
                WaypointType.DOOR -> "e.g. Bathroom door"
                WaypointType.ROOM -> "e.g. Kitchen"
                WaypointType.HALLWAY -> "e.g. Living-dining"
                WaypointType.OTHER -> "e.g. Entrance"
              },
            )
          },
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        TextButton(onClick = { activity.confirmWaypointName(waypointName) }) {
          Text("Save marker")
        }
      },
      dismissButton = {
        TextButton(onClick = { activity.dismissWaypointNaming() }) {
          Text("Cancel")
        }
      },
    )
  }

  if (showClearConfirm) {
    AlertDialog(
      onDismissRequest = { showClearConfirm = false },
      title = { Text("Clear all markers?") },
      text = { Text("This deletes saved markers from the phone. You will need to mark again.") },
      confirmButton = {
        TextButton(
          onClick = {
            showClearConfirm = false
            activity.clearAllWaypoints()
          },
        ) {
          Text("Clear")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirm = false }) {
          Text("Cancel")
        }
      },
    )
  }

  if (showSavedList) {
    SavedMarkersDialog(
      waypoints = waypoints,
      onDelete = { activity.removeWaypoint(it) },
      onClearAll = {
        showSavedList = false
        showClearConfirm = true
      },
      onDismiss = { showSavedList = false },
    )
  }

  if (showConnections) {
    ConnectionsDialog(
      waypoints = waypoints,
      edges = edges,
      onConnect = { from, to -> activity.connectWaypoints(from, to) },
      onDisconnect = { from, to -> activity.disconnectWaypoints(from, to) },
      onDismiss = { showConnections = false },
    )
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Card(
      modifier =
        Modifier
          .align(Alignment.TopCenter)
          .padding(12.dp)
          .fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.65f)),
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AppMode.entries.forEach { mode ->
            FilterChip(
              selected = appMode == mode,
              onClick = { activity.updateAppMode(mode) },
              label = { Text(mode.label) },
            )
          }
        }

        if (appMode == AppMode.MAP) {
          Text(
            "Map mode — mark spots in your home",
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
          )
          Text(
            preview.hint,
            color = if (preview.canPlace) Color(0xFF7CFC00) else Color(0xFFFFB74D),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
          )
          val status = activity.localizationStatus
          if (status.isNotBlank()) {
            Text(
              status,
              color = Color(0xFF81D4FA),
              modifier = Modifier.padding(top = 4.dp),
              style = MaterialTheme.typography.bodySmall,
            )
          }
        } else {
          Text(
            "Navigate — follow the cyan spheres toward the next stop",
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
          )
          val status = activity.localizationStatus
          if (status.isNotBlank()) {
            Text(
              status,
              color = Color(0xFF81D4FA),
              modifier = Modifier.padding(top = 4.dp),
              style = MaterialTheme.typography.bodySmall,
            )
          }
          if (waypoints.any { it.cloudReady }) {
            TextButton(onClick = { activity.requestRescan() }) {
              Text("Scan again")
            }
          }
        }
      }
    }

    if (appMode == AppMode.MAP) {
      val crosshairColor = if (preview.canPlace) Color(0xFF00E676) else Color(0xFFFF5252)
      Box(modifier = Modifier.align(Alignment.Center)) {
        Box(
          modifier =
            Modifier
              .size(22.dp)
              .border(2.dp, crosshairColor, CircleShape),
        )
        Box(
          modifier =
            Modifier
              .size(4.dp)
              .background(crosshairColor, CircleShape),
        )
      }
    }

    activity.waypointScreenLabels.forEach { label ->
      Card(
        modifier =
          Modifier.offset {
            IntOffset(label.screenX.roundToInt() - 60, label.screenY.roundToInt() - 40)
          },
        shape = RoundedCornerShape(8.dp),
        colors =
          CardDefaults.cardColors(
            containerColor =
              if (label.id == destinationId) {
                MaterialTheme.colorScheme.tertiaryContainer
              } else {
                MaterialTheme.colorScheme.primaryContainer
              },
          ),
      ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
          Text(label.typeLabel, style = MaterialTheme.typography.labelSmall)
          Text(label.name, style = MaterialTheme.typography.labelLarge)
        }
      }
    }

    // 2D guide spheres (always readable) on top of the 3D path dots.
    if (appMode == AppMode.NAVIGATE) {
      activity.guideScreenDots.forEach { (sx, sy) ->
        Box(
          modifier =
            Modifier
              .offset { IntOffset(sx.roundToInt() - 6, sy.roundToInt() - 6) }
              .size(12.dp)
              .background(Color(0xCC00E5FF), CircleShape)
              .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
        )
      }
    }

    if (appMode == AppMode.MAP) {
      MapModeControls(
        activity = activity,
        waypoints = waypoints,
        markingType = markingType,
        onMarkingTypeChange = {
          markingType = it
          activity.updateMarkingType(it)
        },
        preview = preview,
        onOpenSavedList = { showSavedList = true },
        onOpenConnections = { showConnections = true },
        edgeCount = edges.size,
        onClearClick = { showClearConfirm = true },
      )
    } else {
      NavigateModeControls(
        activity = activity,
        waypoints = waypoints,
        startId = startId,
        destinationId = destinationId,
        pathSummary = pathSummary,
      )
    }
  }
}


@Composable
private fun ConnectionsDialog(
  waypoints: List<UiMarker>,
  edges: List<UiEdge>,
  onConnect: (String, String) -> Boolean,
  onDisconnect: (String, String) -> Unit,
  onDismiss: () -> Unit,
) {
  // Tap marker A, then marker B â†’ link. No From/To chip grids.
  var firstId by remember { mutableStateOf<String?>(null) }
  val firstName = waypoints.find { it.id == firstId }?.name

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Link walkable spots") },
    text = {
      LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
        item {
          Text(
            if (firstId == null) {
              "Tap two markers that connect (door next to room, etc.)."
            } else {
              "Selected: $firstName — tap the other marker to link."
            },
            style = MaterialTheme.typography.bodyMedium,
          )
        }

        if (waypoints.size < 2) {
          item {
            Text(
              "Need at least 2 markers.",
              modifier = Modifier.padding(top = 8.dp),
              style = MaterialTheme.typography.bodySmall,
            )
          }
        } else {
          items(waypoints, key = { "c-${it.id}" }) { wp ->
            val selected = wp.id == firstId
            Text(
              wp.name,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .padding(top = 6.dp)
                  .background(
                    if (selected) Color(0xFF1B5E20) else Color(0xFFF5F5F5),
                    RoundedCornerShape(8.dp),
                  )
                  .clickable {
                    val a = firstId
                    when {
                      a == null -> firstId = wp.id
                      a == wp.id -> firstId = null
                      else -> {
                        if (onConnect(a, wp.id)) firstId = null
                      }
                    }
                  }
                  .padding(horizontal = 12.dp, vertical = 12.dp),
              color = if (selected) Color.White else Color.Black,
              style = MaterialTheme.typography.bodyLarge,
            )
          }
        }

        if (edges.isNotEmpty()) {
          item {
            Text(
              "Links (${edges.size})",
              modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
              style = MaterialTheme.typography.labelLarge,
            )
          }
          items(edges, key = { "e-${it.fromId}-${it.toId}" }) { edge ->
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .padding(vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                "${edge.fromName} â†” ${edge.toName}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
              )
              TextButton(onClick = { onDisconnect(edge.fromId, edge.toId) }) {
                Text("Remove")
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done")
      }
    },
    dismissButton =
      if (firstId != null) {
        {
          TextButton(onClick = { firstId = null }) {
            Text("Clear pick")
          }
        }
      } else {
        null
      },
  )
}

@Composable
private fun SavedMarkersDialog(
  waypoints: List<UiMarker>,
  onDelete: (String) -> Unit,
  onClearAll: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Saved markers (${waypoints.size})") },
    text = {
      if (waypoints.isEmpty()) {
        Text("No markers saved yet.")
      } else {
        LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
          items(waypoints, key = { it.id }) { waypoint ->
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(waypoint.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                  if (waypoint.cloudReady) "${waypoint.type.label} Â· cloud OK"
                  else "${waypoint.type.label} Â· remake needed",
                  style = MaterialTheme.typography.labelSmall,
                )
              }
              TextButton(onClick = { onDelete(waypoint.id) }) {
                Text("Delete")
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done")
      }
    },
    dismissButton = {
      if (waypoints.isNotEmpty()) {
        TextButton(onClick = onClearAll) {
          Text("Clear all")
        }
      }
    },
  )
}

@Composable
private fun BoxScope.MapModeControls(
  activity: HelloArActivity,
  waypoints: List<UiMarker>,
  markingType: WaypointType,
  onMarkingTypeChange: (WaypointType) -> Unit,
  preview: PlacementPreview,
  onOpenSavedList: () -> Unit,
  onOpenConnections: () -> Unit,
  edgeCount: Int,
  onClearClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Marking:", color = Color.White, style = MaterialTheme.typography.labelMedium)
    Row(
      modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      WaypointType.entries.forEach { type ->
        FilterChip(
          selected = markingType == type,
          onClick = { onMarkingTypeChange(type) },
          label = { Text(type.label) },
        )
      }
    }

    Button(
      onClick = { activity.requestDropWaypoint() },
      enabled = preview.canPlace,
    ) {
      Text("Mark Spot")
    }

    if (waypoints.any { it.cloudReady }) {
      TextButton(onClick = { activity.requestRescan() }) {
        Text("Scan again")
      }
    }

    TextButton(onClick = onOpenConnections) {
      Text("Connections ($edgeCount)")
    }

    TextButton(onClick = onOpenSavedList) {
      Text(
        if (waypoints.isEmpty()) "Saved markers (0)"
        else "Saved markers (${waypoints.size})",
      )
    }

    if (waypoints.isNotEmpty()) {
      TextButton(onClick = onClearClick) {
        Text("Clear all markers")
      }
    }
  }
}

@Composable
private fun BoxScope.NavigateModeControls(
  activity: HelloArActivity,
  waypoints: List<UiMarker>,
  startId: String?,
  destinationId: String?,
  pathSummary: String,
) {
  Card(
    modifier =
      Modifier
        .align(Alignment.BottomCenter)
        .padding(16.dp)
        .fillMaxWidth()
        .heightIn(max = 320.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      val hint = activity.navHint
      if (hint.isNotBlank()) {
        NavigationManeuverBanner(
          hint = hint,
          turn = activity.navTurn,
          arrived = activity.arrivedAtDestination,
        )
      }

      Text(
        "Start from",
        color = Color.White,
        modifier = Modifier.padding(top = if (hint.isNotBlank()) 10.dp else 0.dp),
        style = MaterialTheme.typography.labelMedium,
      )
      if (waypoints.isEmpty()) {
        Text(
          "No markers yet. Switch to Map mode and mark spots first.",
          color = Color.LightGray,
          modifier = Modifier.padding(top = 8.dp),
          style = MaterialTheme.typography.bodySmall,
        )
      } else {
        LazyColumn(modifier = Modifier.heightIn(max = 72.dp).padding(top = 4.dp)) {
          items(waypoints, key = { "start-${it.id}" }) { waypoint ->
            val selected = waypoint.id == startId
            Text(
              waypoint.name,
              color = if (selected) Color(0xFFA5D6A7) else Color.White,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clickable { activity.selectStart(waypoint.id) }
                  .background(
                    if (selected) Color(0xFF1B5E20) else Color.Transparent,
                    RoundedCornerShape(6.dp),
                  )
                  .padding(horizontal = 8.dp, vertical = 6.dp),
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }

        Text(
          "Destination",
          color = Color.White,
          modifier = Modifier.padding(top = 8.dp),
          style = MaterialTheme.typography.labelMedium,
        )
        LazyColumn(modifier = Modifier.heightIn(max = 100.dp).padding(top = 4.dp)) {
          items(waypoints, key = { "dest-${it.id}" }) { waypoint ->
            val selected = waypoint.id == destinationId
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clickable { activity.selectDestination(waypoint.id) }
                  .background(
                    if (selected) Color(0xFF2E7D32) else Color.Transparent,
                    RoundedCornerShape(8.dp),
                  )
                  .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(waypoint.name, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Text(
                  waypoint.type.label,
                  color = Color.LightGray,
                  style = MaterialTheme.typography.labelSmall,
                )
              }
              if (selected) {
                Text("Go", color = Color(0xFFA5D6A7), style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }

        if (pathSummary.isNotBlank()) {
          Text(
            pathSummary,
            color = Color(0xFF81D4FA),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall,
          )
        }

        if (destinationId != null) {
          TextButton(onClick = { activity.clearDestination() }) {
            Text("Stop navigation")
          }
        }
      }
    }
  }
}

/** Maps-style text strip: turn + destination + distance (glyphs live in AR world space). */
@Composable
private fun NavigationManeuverBanner(
  hint: String,
  turn: NavTurn,
  arrived: Boolean,
) {
  val glyph =
    when {
      arrived || turn == NavTurn.ARRIVED -> "?"
      turn == NavTurn.LEFT -> "?"
      turn == NavTurn.RIGHT -> "?"
      turn == NavTurn.FORWARD -> "?"
      turn == NavTurn.LOST -> "…"
      else -> "?"
    }
  val tint =
    when {
      arrived || turn == NavTurn.ARRIVED -> Color(0xFFA5D6A7)
      turn == NavTurn.LOST -> Color(0xFFFFB74D)
      else -> Color(0xFF80DEEA)
    }
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(Color(0xFF0D1B1E), RoundedCornerShape(10.dp))
        .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(glyph, color = tint, style = MaterialTheme.typography.headlineMedium)
    Text(hint, color = Color.White, style = MaterialTheme.typography.titleSmall)
  }
}

