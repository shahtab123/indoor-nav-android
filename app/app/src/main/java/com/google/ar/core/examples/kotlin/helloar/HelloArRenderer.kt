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

import android.opengl.Matrix
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper
import com.google.ar.core.examples.java.common.samplerender.Mesh
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.examples.java.common.samplerender.Shader
import com.google.ar.core.examples.java.common.samplerender.VertexBuffer
import com.google.ar.core.examples.java.common.samplerender.arcore.BackgroundRenderer
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.SessionPausedException
import com.google.ar.core.examples.kotlin.helloar.ui.AppMode
import com.google.ar.core.examples.kotlin.helloar.ui.NavTurn
import com.google.ar.core.examples.kotlin.helloar.ui.PlacementPreview
import com.google.ar.core.examples.kotlin.helloar.ui.WaypointScreenLabel
import com.google.ar.core.examples.kotlin.helloar.waypoints.WaypointType
import java.io.IOException
import kotlin.math.min
import kotlin.math.sqrt

/**
 * AR rendering. Nav uses spaced world-space guide spheres toward the next waypoint.
 * Turn left/right stays in the Compose banner only.
 */
class HelloArRenderer(val activity: HelloArActivity) :
  SampleRender.Renderer, DefaultLifecycleObserver {
  companion object {
    private const val TAG = "HelloArRenderer"
    private const val Z_NEAR = 0.1f
    private const val Z_FAR = 100f
    private const val MARKER_SCALE = 2.5f
    /** Mesh radius ~0.08m; keep dots small so they don’t dominate the view. */
    private const val PATH_SPHERE_SCALE = 0.85f
    /** Spaced breadcrumbs (meters ahead of phone toward next pin). */
    private val GUIDE_DISTANCES_M = floatArrayOf(0.85f, 1.9f, 3.0f)
    private const val GUIDE_HEIGHT_OFFSET_M = 0.35f
  }

  lateinit var backgroundRenderer: BackgroundRenderer
  lateinit var pointCloudVertexBuffer: VertexBuffer
  lateinit var pointCloudMesh: Mesh
  lateinit var pointCloudShader: Shader
  lateinit var waypointMarkerMesh: Mesh
  lateinit var waypointMarkerShader: Shader
  lateinit var navSphereMesh: Mesh
  lateinit var navSphereShader: Shader

  var lastPointCloudTimestamp: Long = 0
  var hasSetTextureNames = false
  private var surfaceReady = false

  val projectionMatrix = FloatArray(16)
  val viewMatrix = FloatArray(16)
  val modelMatrix = FloatArray(16)
  val modelViewMatrix = FloatArray(16)
  val modelViewProjectionMatrix = FloatArray(16)

  val session
    get() = activity.arCoreSessionHelper.session

  val displayRotationHelper = DisplayRotationHelper(activity)
  val trackingStateHelper = TrackingStateHelper(activity)

  override fun onResume(owner: LifecycleOwner) {
    displayRotationHelper.onResume()
    hasSetTextureNames = false
  }

  override fun onPause(owner: LifecycleOwner) {
    displayRotationHelper.onPause()
  }

  override fun onSurfaceCreated(render: SampleRender) {
    try {
      backgroundRenderer = BackgroundRenderer(render)
      backgroundRenderer.setUseDepthVisualization(render, /*useDepthVisualization=*/ false)

      pointCloudShader =
        Shader.createFromAssets(
            render,
            "shaders/point_cloud.vert",
            "shaders/point_cloud.frag",
            /*defines=*/ null,
          )
          .setVec4("u_Color", floatArrayOf(31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f))
          .setFloat("u_PointSize", 5.0f)

      pointCloudVertexBuffer = VertexBuffer(render, /*numberOfEntriesPerVertex=*/ 4, /*entries=*/ null)
      pointCloudMesh =
        Mesh(render, Mesh.PrimitiveMode.POINTS, /*indexBuffer=*/ null, arrayOf(pointCloudVertexBuffer))

      waypointMarkerMesh = Mesh.createFromAsset(render, "models/pawn.obj")
      waypointMarkerShader =
        Shader.createFromAssets(
            render,
            "shaders/waypoint_marker.vert",
            "shaders/waypoint_marker.frag",
            /*defines=*/ null,
          )
          .setDepthTest(false)
          .setDepthWrite(false)

      navSphereMesh = Mesh.createFromAsset(render, "models/nav_sphere.obj")
      navSphereShader =
        Shader.createFromAssets(
            render,
            "shaders/waypoint_marker.vert",
            "shaders/waypoint_marker.frag",
            /*defines=*/ null,
          )
          .setDepthTest(false)
          .setDepthWrite(false)

      surfaceReady = true
    } catch (e: IOException) {
      Log.e(TAG, "Failed to read a required asset file", e)
      showError("Failed to read a required asset file: $e")
    }
  }

  override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
    displayRotationHelper.onSurfaceChanged(width, height)
    activity.viewportWidth = width
    activity.viewportHeight = height
  }

  override fun onDrawFrame(render: SampleRender) {
    val session = session ?: return
    if (!surfaceReady) return

    if (!hasSetTextureNames) {
      session.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))
      hasSetTextureNames = true
    }

    displayRotationHelper.updateSessionIfNeeded(session)

    val frame =
      try {
        session.update()
      } catch (_: SessionPausedException) {
        return
      } catch (e: CameraNotAvailableException) {
        Log.e(TAG, "Camera not available during onDrawFrame", e)
        showError("Camera not available. Try restarting the app.")
        return
      }

    val camera = frame.camera
    trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

    backgroundRenderer.updateDisplayGeometry(frame)
    if (frame.timestamp != 0L) {
      backgroundRenderer.drawBackground(render)
    }

    val inMapMode = activity.appMode == AppMode.MAP
    val inNavigateMode = activity.appMode == AppMode.NAVIGATE

    if (camera.trackingState == TrackingState.TRACKING) {
      activity.updateCameraPose(camera.pose)
      activity.tryResolveWhenTracking(session)
      if (inNavigateMode) {
        activity.tickNavigation(camera.pose)
      }
    }

    val centerX = activity.viewportWidth * 0.5f
    val centerY = activity.viewportHeight * 0.5f

    if (camera.trackingState == TrackingState.PAUSED) {
      if (inMapMode) {
        activity.updatePlacementPreview(
          PlacementPreview(canPlace = false, hint = "Move phone slowly until tracking starts."),
        )
      }
      if (activity.consumeDropWaypointRequest()) {
        showError("Wait for tracking, then aim at a wall or floor.")
      }
      return
    }

    if (inMapMode) {
      val placementHit =
        WaypointPlacementHelper.findPlacementHit(
          frame,
          camera,
          centerX,
          centerY,
          activity.markingType,
        )
      if (placementHit != null) {
        activity.updatePlacementPreview(
          PlacementPreview(
            canPlace = true,
            hint = WaypointPlacementHelper.aimHint(activity.markingType, canPlace = true),
          ),
        )
      } else {
        activity.updatePlacementPreview(
          PlacementPreview(
            canPlace = false,
            hint = WaypointPlacementHelper.aimHint(activity.markingType, canPlace = false),
          ),
        )
      }

      if (activity.consumeDropWaypointRequest()) {
        if (placementHit != null) {
          activity.onPlacementHitReady(placementHit)
        } else {
          activity.onPlacementMissed()
        }
      }
    } else {
      activity.consumeDropWaypointRequest()
    }

    camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR)
    camera.getViewMatrix(viewMatrix, 0)

    if (inMapMode) {
      frame.acquirePointCloud().use { pointCloud ->
        if (pointCloud.timestamp > lastPointCloudTimestamp) {
          pointCloudVertexBuffer.set(pointCloud.points)
          lastPointCloudTimestamp = pointCloud.timestamp
        }
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
        render.draw(pointCloudMesh, pointCloudShader)
      }
    } else {
      frame.acquirePointCloud().close()
    }

    drawWaypointMarkers(render)
    if (inNavigateMode && camera.trackingState == TrackingState.TRACKING) {
      drawPathSpheresTowardNext(render, camera)
    } else {
      activity.updateGuideScreenDots(emptyList())
    }
    updateWaypointLabels(camera)
  }

  private fun drawWaypointMarkers(render: SampleRender) {
    val waypoints = activity.waypointRepository.waypoints
    for (waypoint in waypoints) {
      try {
        val anchor = activity.liveAnchors.get(waypoint.id) ?: continue
        if (anchor.trackingState != TrackingState.TRACKING) continue

        anchor.pose.toMatrix(modelMatrix, 0)
        Matrix.scaleM(modelMatrix, 0, MARKER_SCALE, MARKER_SCALE, MARKER_SCALE)

        waypointMarkerShader.setVec4("u_Color", markerColorFor(waypoint.type))
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
        waypointMarkerShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
        render.draw(waypointMarkerMesh, waypointMarkerShader)
      } catch (_: Exception) {
      }
    }
  }

  /**
   * Spaced cyan spheres toward the next waypoint (3D in world + 2D screen overlays).
   */
  private fun drawPathSpheresTowardNext(render: SampleRender, camera: com.google.ar.core.Camera) {
    if (activity.arrivedAtDestination) {
      activity.updateGuideScreenDots(emptyList())
      return
    }
    if (activity.navTurn == NavTurn.NONE || activity.navTurn == NavTurn.LOST) {
      activity.updateGuideScreenDots(emptyList())
      return
    }
    val targetId = activity.nextWaypointId ?: run {
      activity.updateGuideScreenDots(emptyList())
      return
    }
    val targetAnchor = activity.liveAnchors.get(targetId)
    if (targetAnchor == null || targetAnchor.trackingState != TrackingState.TRACKING) {
      activity.updateGuideScreenDots(emptyList())
      return
    }

    try {
      val cameraPose = camera.pose
      val target = targetAnchor.pose
      val dx = target.tx() - cameraPose.tx()
      val dz = target.tz() - cameraPose.tz()
      val len = sqrt(dx * dx + dz * dz)
      if (len < 0.4f) {
        activity.updateGuideScreenDots(emptyList())
        return
      }

      val nx = dx / len
      val nz = dz / len
      val y = cameraPose.ty() - GUIDE_HEIGHT_OFFSET_M
      val maxDist = min(len - 0.3f, GUIDE_DISTANCES_M.last())
      val color = floatArrayOf(0.1f, 0.95f, 1.0f, 1f)
      val screenDots = ArrayList<Pair<Float, Float>>()

      for (dist in GUIDE_DISTANCES_M) {
        if (dist > maxDist) break
        val pose =
          Pose.makeTranslation(cameraPose.tx() + nx * dist, y, cameraPose.tz() + nz * dist)
        drawGuideSphere(render, pose, color)
        activity.projectToScreen(pose, camera, labelHeightMeters = 0f)?.let { screenDots.add(it) }
      }
      activity.updateGuideScreenDots(screenDots)
    } catch (e: Exception) {
      Log.w(TAG, "drawPathSpheresTowardNext failed", e)
      activity.updateGuideScreenDots(emptyList())
    }
  }

  private fun drawGuideSphere(render: SampleRender, pose: Pose, color: FloatArray) {
    pose.toMatrix(modelMatrix, 0)
    Matrix.scaleM(modelMatrix, 0, PATH_SPHERE_SCALE, PATH_SPHERE_SCALE, PATH_SPHERE_SCALE)
    navSphereShader.setVec4("u_Color", color)
    Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
    Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
    navSphereShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
    render.draw(navSphereMesh, navSphereShader)
  }

  private fun markerColorFor(type: WaypointType): FloatArray =
    when (type) {
      WaypointType.DOOR -> floatArrayOf(0.2f, 0.55f, 1.0f, 1.0f)
      WaypointType.ROOM -> floatArrayOf(1.0f, 0.55f, 0.0f, 1.0f)
      WaypointType.HALLWAY -> floatArrayOf(0.2f, 0.85f, 0.35f, 1.0f)
      WaypointType.OTHER -> floatArrayOf(1.0f, 0.95f, 0.2f, 1.0f)
    }

  private fun updateWaypointLabels(camera: com.google.ar.core.Camera) {
    val waypoints = activity.waypointRepository.waypoints
    if (waypoints.isEmpty()) {
      activity.updateWaypointScreenLabels(emptyList())
      return
    }

    val labels = ArrayList<WaypointScreenLabel>()
    for (waypoint in waypoints) {
      try {
        val anchor = activity.liveAnchors.get(waypoint.id) ?: continue
        if (anchor.trackingState != TrackingState.TRACKING) continue
        val screen = activity.projectToScreen(anchor.pose, camera) ?: continue
        labels.add(
          WaypointScreenLabel(
            id = waypoint.id,
            name = waypoint.name,
            typeLabel = waypoint.type.label,
            screenX = screen.first,
            screenY = screen.second,
          ),
        )
      } catch (_: Exception) {
      }
    }
    activity.updateWaypointScreenLabels(labels)
  }

  private fun showError(errorMessage: String) {
    activity.runOnUiThread {
      Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show()
    }
  }
}
