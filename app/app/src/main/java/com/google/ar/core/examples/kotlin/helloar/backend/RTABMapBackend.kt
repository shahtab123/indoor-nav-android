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
package com.google.ar.core.examples.kotlin.helloar.backend

/**
 * v2 placeholder: self-hosted open-source localization (RTAB-Map or similar VPS).
 *
 * Will implement the same [LocalizationBackend] contract so UI / A* / waypoints
 * need little or no change when Google Cloud is replaced.
 *
 * TODO(v2):
 * - Run RTAB-Map (or Placeframe / OpenVPS) on a local or home server
 * - Map apartment once → save map artifact via [saveMap]
 * - Guests [loadMap] + localize camera frames → resolve waypoint poses
 * - [hostAnchor] becomes “register landmark in map” instead of Google host
 */
class RTABMapBackend : LocalizationBackend {
  override val capabilities =
    BackendCapabilities(
      displayName = "RTAB-Map (not implemented)",
      usesArCoreCloudAnchors = false,
      supportsMultiUserShare = true,
    )

  override fun configureArSession(hooks: ArSessionHooks) {
    // No Google Cloud Anchor mode. Future: depth / custom tracking hints.
    hooks.setCloudAnchorModeEnabled(false)
  }

  override suspend fun hostAnchor(request: HostAnchorRequest): HostAnchorResult =
    HostAnchorResult(
      success = false,
      errorMessage = "RTABMapBackend not implemented yet — use GoogleCloudBackend for v1.",
    )

  override suspend fun resolveAnchor(request: ResolveAnchorRequest): ResolveAnchorResult =
    ResolveAnchorResult(
      success = false,
      backendAnchorId = request.backendAnchorId,
      waypointId = request.waypointId,
      errorMessage = "RTABMapBackend not implemented yet.",
    )

  override suspend fun deleteAnchor(backendAnchorId: String): DeleteAnchorResult =
    DeleteAnchorResult(success = false, errorMessage = "RTABMapBackend not implemented yet.")

  override suspend fun saveMap(mapId: String): SaveMapResult =
    SaveMapResult(success = false, errorMessage = "TODO: export RTAB-Map / VPS map for $mapId")

  override suspend fun loadMap(mapId: String): LoadMapResult =
    LoadMapResult(success = false, errorMessage = "TODO: load self-hosted map $mapId")
}
