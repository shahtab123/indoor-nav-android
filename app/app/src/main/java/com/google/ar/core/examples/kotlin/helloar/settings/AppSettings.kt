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
package com.google.ar.core.examples.kotlin.helloar.settings

import android.content.Context

enum class LocalizationBackendKind {
  GOOGLE_CLOUD,
  RTAB_MAP,
}

/**
 * User / app settings — separate from waypoints, graph, and anchors.
 */
class AppSettings(context: Context) {
  private val prefs = context.getSharedPreferences("indoor_nav_settings", Context.MODE_PRIVATE)

  var localizationBackend: LocalizationBackendKind
    get() =
      when (prefs.getString(KEY_BACKEND, LocalizationBackendKind.GOOGLE_CLOUD.name)) {
        LocalizationBackendKind.RTAB_MAP.name -> LocalizationBackendKind.RTAB_MAP
        else -> LocalizationBackendKind.GOOGLE_CLOUD
      }
    set(value) {
      prefs.edit().putString(KEY_BACKEND, value.name).apply()
    }

  var defaultFloor: Int
    get() = prefs.getInt(KEY_FLOOR, 0)
    set(value) {
      prefs.edit().putInt(KEY_FLOOR, value).apply()
    }

  companion object {
    private const val KEY_BACKEND = "localization_backend"
    private const val KEY_FLOOR = "default_floor"
  }
}
