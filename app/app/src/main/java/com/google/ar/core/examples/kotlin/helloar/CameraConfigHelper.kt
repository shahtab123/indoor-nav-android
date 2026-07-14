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

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Session

/**
 * Selects the widest rear lens that ARCore has actually calibrated on this device.
 *
 * This is the only supported way to get a wider field of view in ARCore. Shader UV scaling or
 * lower CPU resolutions do not change the physical lens and will look wrong.
 *
 * Stock camera "0.6x" on many phones switches to a separate ultrawide sensor. If Google has not
 * calibrated that sensor for ARCore, it will not appear in [Session.getSupportedCameraConfigs].
 */
object CameraConfigHelper {
  private const val TAG = "CameraConfigHelper"

  fun selectWidestBackCamera(session: Session, context: Context) {
    val filter = CameraConfigFilter(session).setFacingDirection(CameraConfig.FacingDirection.BACK)
    val configs = session.getSupportedCameraConfigs(filter)
    if (configs.isEmpty()) {
      Log.w(TAG, "No supported back camera configs.")
      return
    }

    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val configsWithFocal =
      configs.mapNotNull { config ->
        val focalLength = minFocalLengthMm(cameraManager, config.cameraId)
        if (focalLength == null) {
          Log.w(TAG, "Skipping config cameraId=${config.cameraId}: no focal length.")
          null
        } else {
          config to focalLength
        }
      }

    for ((index, config) in configs.withIndex()) {
      val focal = minFocalLengthMm(cameraManager, config.cameraId)
      Log.i(
        TAG,
        "ARCore[$index] cameraId=${config.cameraId}, focal=${focal}mm, " +
          "image=${config.imageSize}, texture=${config.textureSize}, fps=${config.fpsRange}",
      )
    }

    if (configsWithFocal.isEmpty()) {
      Log.w(TAG, "Could not read focal lengths; keeping ARCore default camera config.")
      return
    }

    val widest = configsWithFocal.minByOrNull { it.second }!!
    val mainFocal = configsWithFocal.maxOf { it.second }
    val zoomRatio = widest.second / mainFocal

    session.cameraConfig = widest.first
    Log.i(
      TAG,
      "Using cameraId=${widest.first.cameraId} (${widest.second}mm, ~${"%.2f".format(zoomRatio)}x vs main).",
    )

    if (configsWithFocal.map { it.first.cameraId }.distinct().size == 1) {
      Log.i(
        TAG,
        "Only one physical lens is exposed to ARCore on this device. " +
          "Stock camera 0.6x ultrawide is unavailable until Google calibrates it for ARCore.",
      )
    }
  }

  private fun minFocalLengthMm(cameraManager: CameraManager, cameraId: String): Float? {
    return try {
      val focalLengths =
        cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
      focalLengths?.minOrNull()
    } catch (e: Exception) {
      Log.w(TAG, "Failed to read focal length for cameraId=$cameraId", e)
      null
    }
  }
}
