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

import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.Session
import com.google.ar.core.examples.kotlin.helloar.ar.ArCoreTrackingToken
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * v1 localization: ARCore Persistent Cloud Anchors + Google OAuth / ARCore API.
 *
 * **This is the only class that may call Google Cloud Anchor host/resolve APIs.**
 */
class GoogleCloudBackend : LocalizationBackend {
  companion object {
    private const val TAG = "GoogleCloudBackend"
    /**
     * TTL in days. Use 1 until keyless Android OAuth is confirmed working.
     * Docs: TTL > 1 requires working keyless auth; otherwise host returns ERROR_NOT_AUTHORIZED.
     * After auth works, raise back to 365.
     */
    const val TTL_DAYS = 1
  }

  @Volatile
  private var session: Session? = null

  override val capabilities =
    BackendCapabilities(
      displayName = "Google Cloud Anchors",
      usesArCoreCloudAnchors = true,
      supportsMultiUserShare = true,
    )

  /** AR layer binds the live session after resume — never called from UI/navigation. */
  fun bindSession(session: Session?) {
    this.session = session
  }

  override fun configureArSession(hooks: ArSessionHooks) {
    hooks.setCloudAnchorModeEnabled(true)
  }

  override suspend fun hostAnchor(request: HostAnchorRequest): HostAnchorResult {
    val token =
      request.localTrackingToken as? ArCoreTrackingToken
        ?: return HostAnchorResult(
          success = false,
          errorMessage = "GoogleCloudBackend requires an ArCoreTrackingToken.",
        )
    return hostWithArCore(token.session, token.anchor)
  }

  override suspend fun resolveAnchor(request: ResolveAnchorRequest): ResolveAnchorResult {
    val session =
      session
        ?: return ResolveAnchorResult(
          success = false,
          backendAnchorId = request.backendAnchorId,
          waypointId = request.waypointId,
          errorMessage = "AR session not bound to GoogleCloudBackend.",
        )
    return resolveWithSession(session, request)
  }

  override suspend fun deleteAnchor(backendAnchorId: String): DeleteAnchorResult {
    Log.i(TAG, "deleteAnchor($backendAnchorId): local-only for v1.")
    return DeleteAnchorResult(success = true)
  }

  override suspend fun saveMap(mapId: String): SaveMapResult =
    SaveMapResult(success = true, mapId = mapId)

  override suspend fun loadMap(mapId: String): LoadMapResult =
    LoadMapResult(success = true, mapId = mapId)

  private suspend fun resolveWithSession(
    session: Session,
    request: ResolveAnchorRequest,
  ): ResolveAnchorResult =
    suspendCancellableCoroutine { cont ->
      try {
        val future =
          session.resolveCloudAnchorAsync(request.backendAnchorId) { anchor, state ->
            if (!cont.isActive) return@resolveCloudAnchorAsync
            when (state) {
              Anchor.CloudAnchorState.SUCCESS -> {
                val pose = anchor.pose
                cont.resume(
                  ResolveAnchorResult(
                    success = true,
                    backendAnchorId = request.backendAnchorId,
                    waypointId = request.waypointId,
                    localTrackingToken = ArCoreTrackingToken(session, anchor),
                    pose =
                      Pose6Dof(
                        pose.tx(),
                        pose.ty(),
                        pose.tz(),
                        pose.rotationQuaternion[0],
                        pose.rotationQuaternion[1],
                        pose.rotationQuaternion[2],
                        pose.rotationQuaternion[3],
                      ),
                    retryable = false,
                  ),
                )
              }
              else ->
                cont.resume(
                  ResolveAnchorResult(
                    success = false,
                    backendAnchorId = request.backendAnchorId,
                    waypointId = request.waypointId,
                    errorMessage = humanResolveError(state),
                    retryable = isRetryableResolveFailure(state),
                  ),
                )
            }
          }
        cont.invokeOnCancellation {
          try {
            future.cancel()
          } catch (_: Exception) {
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "resolveCloudAnchorAsync threw", e)
        cont.resume(
          ResolveAnchorResult(
            success = false,
            backendAnchorId = request.backendAnchorId,
            waypointId = request.waypointId,
            errorMessage = e.message ?: "Resolve failed.",
            retryable = true,
          ),
        )
      }
    }

  private suspend fun hostWithArCore(session: Session, localAnchor: Anchor): HostAnchorResult =
    suspendCancellableCoroutine { cont ->
      try {
        val future =
          session.hostCloudAnchorAsync(localAnchor, TTL_DAYS) { cloudAnchorId, state ->
            if (!cont.isActive) return@hostCloudAnchorAsync
            when (state) {
              Anchor.CloudAnchorState.SUCCESS -> {
                if (cloudAnchorId.isNullOrBlank()) {
                  cont.resume(
                    HostAnchorResult(success = false, errorMessage = "Host OK but empty id."),
                  )
                } else {
                  Log.i(TAG, "Hosted cloud anchor id=$cloudAnchorId")
                  cont.resume(HostAnchorResult(success = true, backendAnchorId = cloudAnchorId))
                }
              }
              else ->
                cont.resume(
                  HostAnchorResult(success = false, errorMessage = humanHostError(state)),
                )
            }
          }
        cont.invokeOnCancellation {
          try {
            future.cancel()
          } catch (_: Exception) {
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "hostCloudAnchorAsync threw", e)
        cont.resume(HostAnchorResult(success = false, errorMessage = e.message ?: "Host failed."))
      }
    }

  private fun humanHostError(state: Anchor.CloudAnchorState): String =
    when (state) {
      Anchor.CloudAnchorState.ERROR_NOT_AUTHORIZED ->
        "ARCore API not authorized. Create an API key in Google Cloud, set ARCORE_API_KEY in local.properties, rebuild."
      Anchor.CloudAnchorState.ERROR_HOSTING_SERVICE_UNAVAILABLE ->
        "No internet / ARCore Cloud unreachable."
      Anchor.CloudAnchorState.ERROR_HOSTING_DATASET_PROCESSING_FAILED ->
        "Not enough visual features — walk around the spot and remake."
      Anchor.CloudAnchorState.ERROR_RESOURCE_EXHAUSTED ->
        "Cloud Anchor quota exceeded."
      else -> "Cloud host failed ($state)."
    }

  private fun humanResolveError(state: Anchor.CloudAnchorState): String =
    when (state) {
      Anchor.CloudAnchorState.ERROR_CLOUD_ID_NOT_FOUND ->
        "Cloud marker expired or missing — remake in Map mode."
      Anchor.CloudAnchorState.ERROR_NOT_AUTHORIZED ->
        "ARCore API not authorized."
      Anchor.CloudAnchorState.ERROR_HOSTING_SERVICE_UNAVAILABLE ->
        "No internet while resolving."
      Anchor.CloudAnchorState.ERROR_RESOURCE_EXHAUSTED ->
        "Cloud Anchor quota exceeded — backing off."
      else -> "Could not find marker in room ($state)."
    }

  /**
   * Permanent: expired id / auth. Transient: no visual match yet, network, quota —
   * keep retrying as the user walks (ARCore docs: match improves with better view).
   */
  private fun isRetryableResolveFailure(state: Anchor.CloudAnchorState): Boolean =
    when (state) {
      Anchor.CloudAnchorState.ERROR_CLOUD_ID_NOT_FOUND,
      Anchor.CloudAnchorState.ERROR_NOT_AUTHORIZED,
      -> false
      else -> true
    }
}
