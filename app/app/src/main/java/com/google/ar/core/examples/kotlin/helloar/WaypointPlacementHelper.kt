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

import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.core.examples.java.common.samplerender.arcore.PlaneRenderer
import com.google.ar.core.examples.kotlin.helloar.waypoints.WaypointType

/**
 * Finds a real-world surface under the screen center (crosshair).
 * [WaypointType] changes what we prefer: floor for rooms, walls for doors.
 */
object WaypointPlacementHelper {
  fun findPlacementHit(
    frame: Frame,
    camera: Camera,
    screenX: Float,
    screenY: Float,
    type: WaypointType,
  ): HitResult? {
    val hits = frame.hitTest(screenX, screenY).filter { isValidPlacementHit(it, camera) }
    if (hits.isEmpty()) return null

    return when (type) {
      WaypointType.ROOM, WaypointType.HALLWAY -> pickHorizontalFloor(hits) ?: hits.first()
      WaypointType.DOOR -> pickVerticalWall(hits) ?: hits.first()
      WaypointType.OTHER -> hits.first()
    }
  }

  private fun pickHorizontalFloor(hits: List<HitResult>): HitResult? {
    return hits.firstOrNull { hit ->
      val plane = hit.trackable as? Plane
      plane?.type == Plane.Type.HORIZONTAL_UPWARD_FACING
    }
  }

  private fun pickVerticalWall(hits: List<HitResult>): HitResult? {
    return hits.firstOrNull { hit ->
      val plane = hit.trackable as? Plane
      plane?.type == Plane.Type.VERTICAL
    }
  }

  private fun isValidPlacementHit(hit: HitResult, camera: Camera): Boolean {
    when (val trackable = hit.trackable) {
      is Plane ->
        return trackable.trackingState == TrackingState.TRACKING &&
          trackable.isPoseInPolygon(hit.hitPose) &&
          PlaneRenderer.calculateDistanceToPlane(hit.hitPose, camera.pose) > 0f
      is Point ->
        return trackable.trackingState == TrackingState.TRACKING &&
          trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
      else -> return false
    }
  }

  fun aimHint(type: WaypointType, canPlace: Boolean): String {
    if (!canPlace) {
      return when (type) {
        WaypointType.ROOM -> "Aim crosshair at the floor in the center of the room."
        WaypointType.HALLWAY -> "Aim crosshair at the hallway floor."
        WaypointType.DOOR -> "Aim crosshair at the door frame or wall."
        WaypointType.OTHER -> "Aim crosshair at a floor or wall."
      }
    }
    return "Crosshair locked — tap Mark Spot."
  }

  fun hintForMiss(type: WaypointType): String =
    when (type) {
      WaypointType.ROOM -> "Point the phone down at the room floor, wait for green crosshair, then Mark Spot."
      WaypointType.HALLWAY -> "Point at the hallway floor, wait for green crosshair, then Mark Spot."
      WaypointType.DOOR -> "Point at the door frame or wall, wait for green crosshair, then Mark Spot."
      WaypointType.OTHER -> "Aim at a floor or wall, wait for green crosshair, then Mark Spot."
    }

  fun poseForMarker(hit: HitResult): Pose {
    return hit.hitPose.compose(Pose.makeTranslation(0f, 0.02f, 0f))
  }
}
