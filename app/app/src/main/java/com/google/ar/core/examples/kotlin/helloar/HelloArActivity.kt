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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.Camera
import com.google.ar.core.Config
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.examples.kotlin.common.helpers.ARCoreSessionLifecycleHelper
import com.google.ar.core.examples.kotlin.helloar.ar.ArCoreTrackingToken
import com.google.ar.core.examples.kotlin.helloar.ar.LiveAnchorRegistry
import com.google.ar.core.examples.kotlin.helloar.backend.ArSessionHooks
import com.google.ar.core.examples.kotlin.helloar.backend.BackendFactory
import com.google.ar.core.examples.kotlin.helloar.backend.GoogleCloudBackend
import com.google.ar.core.examples.kotlin.helloar.backend.HostAnchorRequest
import com.google.ar.core.examples.kotlin.helloar.backend.LocalizationBackend
import com.google.ar.core.examples.kotlin.helloar.backend.ResolveAnchorRequest
import com.google.ar.core.examples.kotlin.helloar.navigation.GraphRepository
import com.google.ar.core.examples.kotlin.helloar.navigation.Pathfinder
import com.google.ar.core.examples.kotlin.helloar.settings.AppSettings
import com.google.ar.core.examples.kotlin.helloar.ui.AppMode
import com.google.ar.core.examples.kotlin.helloar.ui.NavTurn
import com.google.ar.core.examples.kotlin.helloar.ui.PlacementPreview
import com.google.ar.core.examples.kotlin.helloar.ui.WaypointScreenLabel
import com.google.ar.core.examples.kotlin.helloar.waypoints.UiEdge
import com.google.ar.core.examples.kotlin.helloar.waypoints.UiMarker
import com.google.ar.core.examples.kotlin.helloar.waypoints.Waypoint
import com.google.ar.core.examples.kotlin.helloar.waypoints.WaypointRepository
import com.google.ar.core.examples.kotlin.helloar.waypoints.WaypointType
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.ar.core.TrackingState

/**
 * AR shell + Compose host. Talks to [LocalizationBackend] only — never Google Cloud APIs directly
 * (except ARCore Session/Config, which all AR modes need).
 */
class HelloArActivity : ComponentActivity() {
  companion object {
    private const val TAG = "HelloArActivity"
    /** Advance to the next path node when this close (meters). */
    private const val ARRIVAL_DISTANCE_M = 1.5f
    /** |heading| below this → Straight (degrees). Wider cone = less false left/right. */
    private const val FORWARD_HEADING_DEG = 50f
    /** Under ARCore's 40 concurrent Cloud Anchor ops; fine for apartment-sized maps. */
    private const val MAX_CONCURRENT_RESOLVES = 8
    private const val RESOLVE_RETRY_DELAY_MS = 4_000L
    private const val RESOLVE_QUOTA_BACKOFF_MS = 20_000L
  }

  lateinit var arCoreSessionHelper: ARCoreSessionLifecycleHelper
  lateinit var view: HelloArView
  lateinit var renderer: HelloArRenderer

  lateinit var appSettings: AppSettings
  lateinit var localizationBackend: LocalizationBackend
  lateinit var waypointRepository: WaypointRepository
  lateinit var graphRepository: GraphRepository
  val liveAnchors = LiveAnchorRegistry()

  var waypointRevision by mutableIntStateOf(0)
    private set

  @Volatile
  var appMode: AppMode = AppMode.MAP
    private set

  var selectedDestinationId by mutableStateOf<String?>(null)
    private set

  var selectedStartId by mutableStateOf<String?>(null)
    private set

  var pathSummary by mutableStateOf("")
    private set

  var pathWaypointIds by mutableStateOf<List<String>>(emptyList())
    private set

  /** Index into [pathWaypointIds] for the waypoint the arrow currently targets. */
  var pathIndex by mutableIntStateOf(0)
    private set

  var nextWaypointId by mutableStateOf<String?>(null)
    private set

  var nextWaypointName by mutableStateOf("")
    private set

  var nextDistanceM by mutableStateOf<Float?>(null)
    private set

  var navHint by mutableStateOf("")
    private set

  var arrivedAtDestination by mutableStateOf(false)
    private set

  /** Center-screen turn cue: left / right / forward. */
  var navTurn by mutableStateOf(NavTurn.NONE)
    private set

  /** Signed heading error in degrees: negative = turn left, positive = turn right. */
  var navHeadingDeg by mutableStateOf(0f)
    private set

  var graphRevision by mutableIntStateOf(0)
    private set

  var appModeRevision by mutableIntStateOf(0)
    private set

  private val dropWaypointRequested = AtomicBoolean(false)

  /** Background Cloud Anchor resolve loop (concurrent + auto-retry). */
  private var resolveLoopJob: Job? = null
  private val resolveInFlightIds = Collections.synchronizedSet(mutableSetOf<String>())
  /** Permanent failures (expired / auth) until Scan Again clears them. */
  private val resolveDoNotRetryIds = Collections.synchronizedSet(mutableSetOf<String>())
  @Volatile
  private var boundArSession: Session? = null

  @Volatile
  private var lastCameraPose: Pose? = null

  var localizationStatus by mutableStateOf("")
    private set

  @Volatile
  var markingType: WaypointType = WaypointType.ROOM

  var pendingPlacementPose by mutableStateOf<Pose?>(null)
    private set

  var pendingPlacementType by mutableStateOf(WaypointType.ROOM)
    private set

  var appModeState by mutableStateOf(AppMode.MAP)
    private set

  var placementPreview by mutableStateOf(
    PlacementPreview(canPlace = false, hint = "Move phone slowly to scan the room."),
  )
    private set

  private val _waypointScreenLabels = mutableStateListOf<WaypointScreenLabel>()
  val waypointScreenLabels: List<WaypointScreenLabel> = _waypointScreenLabels

  /** Screen pixels for path guide dots (2D overlays on top of AR). */
  private val _guideScreenDots = mutableStateListOf<Pair<Float, Float>>()
  val guideScreenDots: List<Pair<Float, Float>> = _guideScreenDots

  var viewportWidth = 1
  var viewportHeight = 1

  override fun onCreate(savedInstanceState: android.os.Bundle?) {
    super.onCreate(savedInstanceState)

    appSettings = AppSettings(this)
    localizationBackend = BackendFactory.create(appSettings)
    waypointRepository = WaypointRepository(this)
    graphRepository = GraphRepository(this)
    waypointRepository.loadFromDisk()
    graphRepository.load()

    val resolvable = waypointRepository.resolvable()
    if (resolvable.isNotEmpty()) {
      localizationStatus =
        "Backend: ${localizationBackend.capabilities.displayName}. " +
          "${resolvable.size} saved spot(s) — walk those rooms; missing ones auto-retry."
    } else if (waypointRepository.waypoints.isNotEmpty()) {
      localizationStatus =
        "Markers have no backend id — clear and remake so they survive app close."
    }

    view = HelloArView(this)
    renderer = HelloArRenderer(this)
    lifecycle.addObserver(renderer)

    arCoreSessionHelper = ARCoreSessionLifecycleHelper(this)
    arCoreSessionHelper.exceptionCallback = { exception ->
      val message =
        when (exception) {
          is UnavailableUserDeclinedInstallationException ->
            "Please install Google Play Services for AR"
          is UnavailableApkTooOldException -> "Please update ARCore"
          is UnavailableSdkTooOldException -> "Please update this app"
          is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
          is CameraNotAvailableException -> "Camera not available. Try restarting the app."
          else -> "Failed to create AR session: $exception"
        }
      Log.e(TAG, "ARCore threw an exception", exception)
      Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    arCoreSessionHelper.beforeSessionResume = ::configureSession
    arCoreSessionHelper.afterSessionResume = { session ->
      boundArSession = session
      (localizationBackend as? GoogleCloudBackend)?.bindSession(session)
      if (waypointRepository.resolvable().isNotEmpty()) {
        ensureResolveLoop(session)
      }
    }
    lifecycle.addObserver(arCoreSessionHelper)

    SampleRender(view.surfaceView, renderer, assets)

    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      CameraPermissionHelper.requestCameraPermission(this)
    }

    setContent {
      MaterialTheme {
        Surface {
          IndoorNavApp(activity = this, surfaceView = view.surfaceView)
        }
      }
    }

    if (waypointRepository.waypoints.isNotEmpty()) {
      Toast.makeText(
          this,
          "Loaded ${waypointRepository.waypoints.size} waypoint(s) " +
            "(${localizationBackend.capabilities.displayName}).",
          Toast.LENGTH_LONG,
        )
        .show()
    }
  }

  fun updateAppMode(mode: AppMode) {
    appMode = mode
    appModeState = mode
    appModeRevision++
    if (mode != AppMode.MAP) {
      pendingPlacementPose = null
    } else {
      clearNavigation()
    }
  }

  fun updateCameraPose(pose: Pose) {
    lastCameraPose = pose
  }

  /**
   * Called each AR frame in Navigate mode.
   * Advances path nodes when close to the next pin, and updates left/right/forward HUD cue.
   */
  fun tickNavigation(cameraPose: Pose) {
    if (appMode != AppMode.NAVIGATE) return
    val path = pathWaypointIds
    if (path.isEmpty() || selectedDestinationId == null) {
      clearNavProgressUi()
      return
    }

    var index = pathIndex.coerceIn(0, path.lastIndex)
    while (index <= path.lastIndex) {
      val id = path[index]
      val anchor = liveAnchors.get(id)
      val name = waypointRepository.find(id)?.name ?: "spot"
      if (anchor == null || anchor.trackingState != TrackingState.TRACKING) {
        pathIndex = index
        nextWaypointId = id
        nextWaypointName = name
        nextDistanceM = null
        arrivedAtDestination = false
        navTurn = NavTurn.LOST
        navHeadingDeg = 0f
        navHint = "Finding \"$name\" — walk that area / Scan again if needed"
        return
      }
      val distance = poseDistance(cameraPose, anchor.pose)
      if (distance <= ARRIVAL_DISTANCE_M) {
        if (index == path.lastIndex) {
          pathIndex = index
          nextWaypointId = null
          nextWaypointName = name
          nextDistanceM = distance
          navTurn = NavTurn.ARRIVED
          navHeadingDeg = 0f
          if (!arrivedAtDestination) {
            arrivedAtDestination = true
            runOnUiThread {
              Toast.makeText(this, "Arrived: $name", Toast.LENGTH_LONG).show()
            }
          }
          navHint = "Arrived: $name"
          return
        }
        index++
        continue
      }
      pathIndex = index
      nextWaypointId = id
      nextWaypointName = name
      nextDistanceM = distance
      arrivedAtDestination = false
      val heading = headingErrorDegrees(cameraPose, anchor.pose)
      navHeadingDeg = heading
      navTurn =
        when {
          heading < -FORWARD_HEADING_DEG -> NavTurn.LEFT
          heading > FORWARD_HEADING_DEG -> NavTurn.RIGHT
          else -> NavTurn.FORWARD
        }
      val turnLabel =
        when (navTurn) {
          NavTurn.LEFT -> "Turn left"
          NavTurn.RIGHT -> "Turn right"
          else -> "Straight"
        }
      navHint = "$turnLabel · $name · ${"%.1f".format(distance)} m"
      return
    }
  }

  /**
   * Signed horizontal angle (degrees) from phone look direction to the target pin.
   * Uses only the XZ plane. If the phone is pointed mostly at the ceiling/floor,
   * returns 0 (Straight) — tilted phones used to spuriously report Left/Right.
   * Negative → left, positive → right.
   */
  private fun headingErrorDegrees(cameraPose: Pose, targetPose: Pose): Float {
    val ahead = cameraPose.compose(Pose.makeTranslation(0f, 0f, -1f))
    var fx = ahead.tx() - cameraPose.tx()
    var fz = ahead.tz() - cameraPose.tz()
    val flen = sqrt(fx * fx + fz * fz)
    if (flen < 0.25f) return 0f // looking too far up/down — don't invent a turn
    fx /= flen
    fz /= flen

    var dx = targetPose.tx() - cameraPose.tx()
    var dz = targetPose.tz() - cameraPose.tz()
    val dlen = sqrt(dx * dx + dz * dz)
    if (dlen < 0.05f) return 0f
    dx /= dlen
    dz /= dlen

    val cross = fx * dz - fz * dx
    val dot = fx * dx + fz * dz
    return Math.toDegrees(atan2(cross.toDouble(), dot.toDouble())).toFloat()
  }

  fun selectStart(id: String) {
    selectedStartId = id
    recomputePath()
  }

  fun selectDestination(id: String) {
    selectedDestinationId = id
    recomputePath()
    val name = waypointRepository.find(id)?.name ?: return
    Toast.makeText(this, "Go to $name — follow the arrow", Toast.LENGTH_SHORT).show()
  }

  fun markersForUi(): List<UiMarker> =
    waypointRepository.waypoints.map {
      UiMarker(
        id = it.id,
        name = it.name,
        type = it.type,
        cloudReady = it.isLocalizedAcrossSessions,
      )
    }

  fun edgesForUi(): List<UiEdge> {
    val names = waypointRepository.waypoints.associate { it.id to it.name }
    return graphRepository.graph.edges.mapNotNull { e ->
      val fromName = names[e.fromId] ?: return@mapNotNull null
      val toName = names[e.toId] ?: return@mapNotNull null
      UiEdge(
        fromId = e.fromId,
        toId = e.toId,
        fromName = fromName,
        toName = toName,
        weightMeters = e.weight,
      )
    }
  }

  fun connectWaypoints(fromId: String, toId: String): Boolean {
    if (fromId == toId) {
      Toast.makeText(this, "Pick two different markers.", Toast.LENGTH_SHORT).show()
      return false
    }
    val a = waypointRepository.find(fromId)?.name ?: fromId
    val b = waypointRepository.find(toId)?.name ?: toId
    if (graphRepository.graph.hasEdge(fromId, toId)) {
      Toast.makeText(this, "Already linked: $a ↔ $b", Toast.LENGTH_SHORT).show()
      return true
    }
    val weight = distanceBetweenWaypoints(fromId, toId) ?: 1f
    graphRepository.connect(fromId, toId, weight)
    graphRevision++
    recomputePath()
    Toast.makeText(this, "Linked $a ↔ $b", Toast.LENGTH_SHORT).show()
    return true
  }

  fun disconnectWaypoints(fromId: String, toId: String) {
    graphRepository.disconnect(fromId, toId)
    graphRevision++
    recomputePath()
  }

  fun clearDestination() {
    clearNavigation()
  }

  private fun clearNavigation() {
    selectedDestinationId = null
    selectedStartId = null
    pathSummary = ""
    pathWaypointIds = emptyList()
    pathIndex = 0
    clearNavProgressUi()
  }

  private fun clearNavProgressUi() {
    nextWaypointId = null
    nextWaypointName = ""
    nextDistanceM = null
    navHint = ""
    arrivedAtDestination = false
    navTurn = NavTurn.NONE
    navHeadingDeg = 0f
  }

  private fun recomputePath() {
    val goal = selectedDestinationId
    if (goal == null) {
      pathSummary = ""
      pathWaypointIds = emptyList()
      pathIndex = 0
      clearNavProgressUi()
      return
    }
    val start = selectedStartId ?: findNearestWaypointId()
    if (start == null) {
      pathSummary = "Pick a start marker (or wait until pins resolve)."
      pathWaypointIds = emptyList()
      pathIndex = 0
      clearNavProgressUi()
      return
    }
    if (selectedStartId == null) {
      selectedStartId = start
    }
    val result =
      Pathfinder.findPath(
        graphRepository.graph,
        start,
        goal,
        heuristic = ::heuristicDistance,
      )
    if (result == null) {
      pathSummary = "No path — open Map → Connections and link the rooms."
      pathWaypointIds = emptyList()
      pathIndex = 0
      clearNavProgressUi()
    } else {
      pathWaypointIds = result.waypointIds
      pathIndex = 0
      arrivedAtDestination = false
      val names =
        result.waypointIds.map { id -> waypointRepository.find(id)?.name ?: id }
      val cost =
        if (result.totalCost > 0f && result.totalCost != result.waypointIds.size - 1f) {
          " (~${"%.1f".format(result.totalCost)} m)"
        } else {
          ""
        }
      pathSummary = names.joinToString(" → ") + cost
      navHint = "Follow the arrows on the path"
    }
  }

  /** Nearest resolved pin to the phone — used as default Navigate start. */
  private fun findNearestWaypointId(): String? {
    val cam = lastCameraPose ?: return null
    var bestId: String? = null
    var bestDist = Float.POSITIVE_INFINITY
    for (wp in waypointRepository.waypoints) {
      val anchor = liveAnchors.get(wp.id) ?: continue
      if (anchor.trackingState != TrackingState.TRACKING) continue
      val d = poseDistance(cam, anchor.pose)
      if (d < bestDist) {
        bestDist = d
        bestId = wp.id
      }
    }
    return bestId
  }

  private fun heuristicDistance(fromId: String, toId: String): Float =
    distanceBetweenWaypoints(fromId, toId) ?: 0f

  private fun distanceBetweenWaypoints(fromId: String, toId: String): Float? {
    val a = liveAnchors.get(fromId) ?: return null
    val b = liveAnchors.get(toId) ?: return null
    if (a.trackingState != TrackingState.TRACKING || b.trackingState != TrackingState.TRACKING) {
      return null
    }
    return poseDistance(a.pose, b.pose)
  }

  private fun poseDistance(a: Pose, b: Pose): Float {
    val dx = a.tx() - b.tx()
    val dy = a.ty() - b.ty()
    val dz = a.tz() - b.tz()
    return sqrt(dx * dx + dy * dy + dz * dz)
  }

  fun requestDropWaypoint() {
    if (appMode != AppMode.MAP) return
    dropWaypointRequested.set(true)
  }

  fun consumeDropWaypointRequest(): Boolean = dropWaypointRequested.getAndSet(false)

  fun updateMarkingType(type: WaypointType) {
    markingType = type
  }

  fun onPlacementHitReady(hit: HitResult) {
    val pose = WaypointPlacementHelper.poseForMarker(hit)
    runOnUiThread {
      pendingPlacementPose =
        Pose(
          floatArrayOf(pose.tx(), pose.ty(), pose.tz()),
          pose.rotationQuaternion.copyOf(),
        )
      pendingPlacementType = markingType
    }
  }

  fun onPlacementMissed() {
    runOnUiThread {
      Toast.makeText(this, WaypointPlacementHelper.hintForMiss(markingType), Toast.LENGTH_LONG).show()
    }
  }

  fun updatePlacementPreview(preview: PlacementPreview) {
    runOnUiThread {
      if (
        placementPreview.canPlace != preview.canPlace || placementPreview.hint != preview.hint
      ) {
        placementPreview = preview
      }
    }
  }

  fun confirmWaypointName(name: String) {
    val pose = pendingPlacementPose ?: return
    val type = pendingPlacementType
    val session = arCoreSessionHelper.session
    if (session == null) {
      Toast.makeText(this, "AR session not ready yet.", Toast.LENGTH_SHORT).show()
      return
    }
    val trimmed = name.trim()
    if (trimmed.isEmpty()) {
      Toast.makeText(this, "Please enter a name.", Toast.LENGTH_SHORT).show()
      return
    }

    try {
      val localAnchor = session.createAnchor(pose)
      val waypoint =
        Waypoint(
          name = trimmed,
          type = type,
          floor = appSettings.defaultFloor,
        )
      waypointRepository.add(waypoint)
      liveAnchors.put(waypoint.id, localAnchor)
      pendingPlacementPose = null
      waypointRevision++
      localizationStatus = "Hosting \"$trimmed\" via ${localizationBackend.capabilities.displayName}…"
      Toast.makeText(this, "Saving \"$trimmed\"…", Toast.LENGTH_SHORT).show()

      (localizationBackend as? GoogleCloudBackend)?.bindSession(session)
      lifecycleScope.launch {
        val result =
          withContext(Dispatchers.IO) {
            localizationBackend.hostAnchor(
              HostAnchorRequest(
                provisionalId = waypoint.id,
                localTrackingToken = ArCoreTrackingToken(session, localAnchor),
                displayName = trimmed,
              ),
            )
          }
        if (result.success && result.backendAnchorId != null) {
          waypointRepository.setBackendAnchorId(waypoint.id, result.backendAnchorId)
          waypointRevision++
          localizationStatus = "\"$trimmed\" locked (survives app close)."
          Toast.makeText(this@HelloArActivity, "Saved \"$trimmed\"", Toast.LENGTH_SHORT).show()
        } else {
          localizationStatus = "Host failed for \"$trimmed\": ${result.errorMessage}"
          Toast.makeText(
              this@HelloArActivity,
              "Local pin OK this session only. ${result.errorMessage}",
              Toast.LENGTH_LONG,
            )
            .show()
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to create waypoint", e)
      Toast.makeText(this, "Could not save marker. Try again.", Toast.LENGTH_SHORT).show()
    }
  }

  fun dismissWaypointNaming() {
    pendingPlacementPose = null
  }

  /**
   * Re-try missing Cloud Anchors only — already resolved pins stay put
   * (accuracy intact; no full wipe unless Clear all).
   */
  fun requestRescan() {
    val pending = waypointRepository.resolvable()
    if (pending.isEmpty()) {
      Toast.makeText(this, "No cloud markers to rescan. Remake in Map mode.", Toast.LENGTH_LONG)
        .show()
      return
    }
    resolveDoNotRetryIds.clear()
    localizationStatus =
      "Retrying missing markers — placed ones stay. Walk near unfinished spots."
    Toast.makeText(this, localizationStatus, Toast.LENGTH_LONG).show()
    resolveLoopJob?.cancel()
    resolveLoopJob = null
    val session = boundArSession
    if (session != null) {
      ensureResolveLoop(session)
    }
  }

  fun removeWaypoint(id: String) {
    if (selectedDestinationId == id) selectedDestinationId = null
    if (selectedStartId == id) selectedStartId = null
    val backendId = waypointRepository.find(id)?.backendAnchorId
    liveAnchors.remove(id)
    resolveDoNotRetryIds.remove(id)
    resolveInFlightIds.remove(id)
    waypointRepository.remove(id)
    graphRepository.removeWaypoint(id)
    waypointRevision++
    graphRevision++
    recomputePath()
    if (backendId != null) {
      lifecycleScope.launch(Dispatchers.IO) {
        localizationBackend.deleteAnchor(backendId)
      }
    }
  }

  fun clearAllWaypoints() {
    stopResolveLoop()
    liveAnchors.clear()
    resolveDoNotRetryIds.clear()
    resolveInFlightIds.clear()
    waypointRepository.clearAll()
    graphRepository.clear()
    clearNavigation()
    localizationStatus = ""
    waypointRevision++
    graphRevision++
    Toast.makeText(this, "All waypoints cleared.", Toast.LENGTH_SHORT).show()
  }

  /**
   * Starts (or keeps) a background loop that:
   * - Resolves unresolved Cloud Anchors concurrently (capped under ARCore's 40-op limit)
   * - Keeps successful anchors in [liveAnchors]
   * - Auto-retries visual/transient failures as the user walks (no Scan Again required)
   * Does not change poses of already-resolved anchors — localization accuracy preserved.
   */
  fun tryResolveWhenTracking(session: Session) {
    boundArSession = session
    if (waypointRepository.resolvable().isEmpty()) return
    ensureResolveLoop(session)
  }

  private fun stopResolveLoop() {
    resolveLoopJob?.cancel()
    resolveLoopJob = null
  }

  private fun ensureResolveLoop(session: Session) {
    if (resolveLoopJob?.isActive == true) return
    (localizationBackend as? GoogleCloudBackend)?.bindSession(session)
    resolveLoopJob =
      lifecycleScope.launch {
        while (isActive) {
          val allResolvable = waypointRepository.resolvable()
          if (allResolvable.isEmpty()) {
            delay(RESOLVE_RETRY_DELAY_MS)
            continue
          }

          val placed = allResolvable.count { liveAnchors.get(it.id) != null }
          val pending =
            allResolvable.filter {
              liveAnchors.get(it.id) == null &&
                it.id !in resolveDoNotRetryIds &&
                it.id !in resolveInFlightIds
            }

          if (pending.isEmpty()) {
            val blocked = allResolvable.count { it.id in resolveDoNotRetryIds }
            val status =
              when {
                placed == allResolvable.size ->
                  "All ${allResolvable.size} marker(s) placed."
                blocked > 0 ->
                  "Placed $placed/${allResolvable.size}. $blocked expired/auth — remake those."
                else ->
                  "Placed $placed/${allResolvable.size} — walk near missing spots (auto-retrying)."
              }
            runOnUiThread { localizationStatus = status }
            delay(RESOLVE_RETRY_DELAY_MS)
            continue
          }

          runOnUiThread {
            localizationStatus =
              "Finding markers… $placed/${allResolvable.size} placed. Walk rooms — retries automatic."
          }

          var hitQuota = AtomicBoolean(false)
          coroutineScope {
            pending
              .chunked(MAX_CONCURRENT_RESOLVES)
              .forEach { batch ->
                batch
                  .map { wp ->
                    async(Dispatchers.IO) {
                      if (!resolveInFlightIds.add(wp.id)) return@async
                      try {
                        if (liveAnchors.get(wp.id) != null) return@async
                        val result =
                          localizationBackend.resolveAnchor(
                            ResolveAnchorRequest(
                              backendAnchorId = wp.backendAnchorId!!,
                              waypointId = wp.id,
                            ),
                          )
                        if (result.success) {
                          val token = result.localTrackingToken as? ArCoreTrackingToken
                          if (token != null) {
                            liveAnchors.put(wp.id, token.anchor)
                            withContext(Dispatchers.Main) { waypointRevision++ }
                          }
                        } else {
                          Log.w(TAG, "Resolve ${wp.name}: ${result.errorMessage}")
                          if (!result.retryable) {
                            resolveDoNotRetryIds.add(wp.id)
                          }
                          if (result.errorMessage?.contains("quota", ignoreCase = true) == true) {
                            hitQuota.set(true)
                          }
                        }
                      } finally {
                        resolveInFlightIds.remove(wp.id)
                      }
                    }
                  }
                  .awaitAll()
              }
          }

          val placedAfter = allResolvable.count { liveAnchors.get(it.id) != null }
          runOnUiThread {
            localizationStatus =
              if (placedAfter == allResolvable.size) {
                "All ${allResolvable.size} marker(s) placed."
              } else {
                "Placed $placedAfter/${allResolvable.size} — keep walking; missing auto-retry."
              }
            waypointRevision++
          }

          delay(
            if (hitQuota.get()) RESOLVE_QUOTA_BACKOFF_MS else RESOLVE_RETRY_DELAY_MS,
          )
        }
      }
  }

  private var lastLabelUpdateMs = 0L

  fun updateWaypointScreenLabels(labels: List<WaypointScreenLabel>) {
    val now = System.currentTimeMillis()
    if (now - lastLabelUpdateMs < 100) return
    lastLabelUpdateMs = now
    runOnUiThread {
      if (_waypointScreenLabels.size == labels.size &&
        _waypointScreenLabels.zip(labels).all { (a, b) ->
          a.id == b.id && a.screenX == b.screenX && a.screenY == b.screenY
        }
      ) {
        return@runOnUiThread
      }
      _waypointScreenLabels.clear()
      _waypointScreenLabels.addAll(labels)
    }
  }

  fun updateGuideScreenDots(dots: List<Pair<Float, Float>>) {
    runOnUiThread {
      if (_guideScreenDots.size == dots.size &&
        _guideScreenDots.zip(dots).all { (a, b) -> a.first == b.first && a.second == b.second }
      ) {
        return@runOnUiThread
      }
      _guideScreenDots.clear()
      _guideScreenDots.addAll(dots)
    }
  }

  fun projectToScreen(pose: Pose, camera: Camera, labelHeightMeters: Float = 0.25f): Pair<Float, Float>? {
    if (viewportWidth <= 0 || viewportHeight <= 0) return null
    val modelMatrix = FloatArray(16)
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, pose.tx(), pose.ty() + labelHeightMeters, pose.tz())
    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    camera.getViewMatrix(viewMatrix, 0)
    camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
    val modelViewMatrix = FloatArray(16)
    val modelViewProjectionMatrix = FloatArray(16)
    Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
    Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
    val clipSpace = FloatArray(4)
    Matrix.multiplyMV(clipSpace, 0, modelViewProjectionMatrix, 0, floatArrayOf(0f, 0f, 0f, 1f), 0)
    if (clipSpace[3] <= 0f) return null
    val ndcX = clipSpace[0] / clipSpace[3]
    val ndcY = clipSpace[1] / clipSpace[3]
    return (ndcX + 1f) * 0.5f * viewportWidth to (1f - ndcY) * 0.5f * viewportHeight
  }

  override fun onResume() {
    super.onResume()
    view.surfaceView.post {
      if (view.surfaceView.parent != null) view.surfaceView.onResume()
    }
  }

  override fun onPause() {
    view.surfaceView.onPause()
    super.onPause()
  }

  fun configureSession(session: Session) {
    CameraConfigHelper.selectWidestBackCamera(session, this)
    var cloudAnchors = false
    localizationBackend.configureArSession(
      object : ArSessionHooks {
        override fun setCloudAnchorModeEnabled(enabled: Boolean) {
          cloudAnchors = enabled
        }
      },
    )
    session.configure(
      session.config.apply {
        lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
        planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        depthMode = Config.DepthMode.DISABLED
        instantPlacementMode = Config.InstantPlacementMode.DISABLED
        cloudAnchorMode =
          if (cloudAnchors) Config.CloudAnchorMode.ENABLED
          else Config.CloudAnchorMode.DISABLED
      },
    )
    (localizationBackend as? GoogleCloudBackend)?.bindSession(session)
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    results: IntArray,
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, results)
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
        .show()
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        CameraPermissionHelper.launchPermissionSettings(this)
      }
      finish()
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
  }
}
