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
 * Opaque token for a placement that exists only in the current AR session.
 *
 * Created by the `ar` layer. Backends cast to their own token type
 * (e.g. [com.google.ar.core.examples.kotlin.helloar.ar.ArCoreTrackingToken]).
 * UI / navigation / waypoints never touch vendor APIs through this.
 */
interface LocalTrackingToken

/**
 * Hooks the localization backend can use while the AR session is configured.
 * Keeps vendor-specific Config flags out of waypoint / navigation code.
 */
interface ArSessionHooks {
  fun setCloudAnchorModeEnabled(enabled: Boolean)
}

data class Pose6Dof(
  val tx: Float,
  val ty: Float,
  val tz: Float,
  val qx: Float,
  val qy: Float,
  val qz: Float,
  val qw: Float,
)

data class HostAnchorRequest(
  val provisionalId: String,
  val localTrackingToken: LocalTrackingToken,
  val displayName: String = "",
)

data class HostAnchorResult(
  val success: Boolean,
  /** Backend-neutral id (Google cloud id, RTAB node id, …). */
  val backendAnchorId: String? = null,
  val errorMessage: String? = null,
)

data class ResolveAnchorRequest(
  val backendAnchorId: String,
  val waypointId: String,
)

data class ResolveAnchorResult(
  val success: Boolean,
  val backendAnchorId: String,
  val waypointId: String,
  val localTrackingToken: LocalTrackingToken? = null,
  val pose: Pose6Dof? = null,
  val errorMessage: String? = null,
  /**
   * If false, stop auto-retrying (expired id / auth). Visual miss or transient errors stay true
   * so walking into the room can succeed without Scan Again.
   */
  val retryable: Boolean = true,
)

data class DeleteAnchorResult(
  val success: Boolean,
  val errorMessage: String? = null,
)

data class SaveMapResult(
  val success: Boolean,
  val mapId: String? = null,
  val errorMessage: String? = null,
)

data class LoadMapResult(
  val success: Boolean,
  val mapId: String? = null,
  val errorMessage: String? = null,
)

data class BackendCapabilities(
  val displayName: String,
  /** True for Google v1 — session must enable ARCore Cloud Anchor mode. */
  val usesArCoreCloudAnchors: Boolean = false,
  val supportsMultiUserShare: Boolean = false,
)
