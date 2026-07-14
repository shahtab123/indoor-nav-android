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
 * Replaceable localization backend.
 *
 * v1: [GoogleCloudBackend] (ARCore Persistent Cloud Anchors)
 * v2: [RTABMapBackend] (self-hosted / open-source VPS)
 *
 * UI, waypoints, A*, and AR screens must only talk to this interface —
 * never to Google Cloud APIs directly.
 */
interface LocalizationBackend {
  val capabilities: BackendCapabilities

  /** Apply backend-specific AR session flags via [hooks] (no vendor types here). */
  fun configureArSession(hooks: ArSessionHooks)

  suspend fun hostAnchor(request: HostAnchorRequest): HostAnchorResult

  suspend fun resolveAnchor(request: ResolveAnchorRequest): ResolveAnchorResult

  suspend fun deleteAnchor(backendAnchorId: String): DeleteAnchorResult

  /**
   * Persist a full environment map when the backend supports it (RTAB-Map, etc.).
   * Google Cloud Anchors are per-anchor; this may no-op successfully.
   */
  suspend fun saveMap(mapId: String): SaveMapResult

  suspend fun loadMap(mapId: String): LoadMapResult
}
