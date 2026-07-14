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
package com.google.ar.core.examples.kotlin.helloar.waypoints

import java.util.UUID

enum class WaypointType(val label: String) {
  DOOR("Door"),
  ROOM("Room"),
  HALLWAY("Hallway"),
  OTHER("Spot"),
}

/**
 * Named place in the apartment. Independent of which localization backend created the anchor.
 *
 * [backendAnchorId] is Google cloud id today, RTAB landmark id tomorrow — never a Google type.
 */
data class Waypoint(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val type: WaypointType,
  val backendAnchorId: String? = null,
  val floor: Int = 0,
  val metadata: Map<String, String> = emptyMap(),
) {
  val isLocalizedAcrossSessions: Boolean
    get() = !backendAnchorId.isNullOrBlank()
}

/** List row for Compose — no ARCore types. */
data class UiMarker(
  val id: String,
  val name: String,
  val type: WaypointType,
  val cloudReady: Boolean,
)

/** One undirected connection for the Connections UI. */
data class UiEdge(
  val fromId: String,
  val toId: String,
  val fromName: String,
  val toName: String,
  val weightMeters: Float,
)
