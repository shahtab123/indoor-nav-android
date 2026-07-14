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
package com.google.ar.core.examples.kotlin.helloar.ui

/** Map = mark / edit. Navigate = pick destination. */
enum class AppMode(val label: String) {
  MAP("Map"),
  NAVIGATE("Navigate"),
}

/** Turn classification for the 2D instruction banner (AR path uses world chevrons). */
enum class NavTurn {
  NONE,
  FORWARD,
  LEFT,
  RIGHT,
  ARRIVED,
  LOST,
}

data class WaypointScreenLabel(
  val id: String,
  val name: String,
  val typeLabel: String,
  val screenX: Float,
  val screenY: Float,
)

data class PlacementPreview(
  val canPlace: Boolean,
  val hint: String,
)
