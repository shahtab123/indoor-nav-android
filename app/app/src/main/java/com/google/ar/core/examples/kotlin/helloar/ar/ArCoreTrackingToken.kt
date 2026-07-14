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
package com.google.ar.core.examples.kotlin.helloar.ar

import com.google.ar.core.Anchor
import com.google.ar.core.Session
import com.google.ar.core.examples.kotlin.helloar.backend.LocalTrackingToken

/**
 * ARCore-specific [LocalTrackingToken]. Only `ar` + [com.google.ar.core.examples.kotlin.helloar.backend.GoogleCloudBackend]
 * should cast to this type.
 */
class ArCoreTrackingToken(
  val session: Session,
  val anchor: Anchor,
) : LocalTrackingToken

/**
 * Live ARCore anchors for drawing pins. Keyed by waypoint id — not by Google cloud id.
 * Swap backend without changing this registry’s role.
 */
class LiveAnchorRegistry {
  private val lock = Any()
  private val anchors = mutableMapOf<String, Anchor>()

  fun put(waypointId: String, anchor: Anchor) {
    synchronized(lock) {
      anchors[waypointId]?.let {
        try {
          it.detach()
        } catch (_: Exception) {
        }
      }
      anchors[waypointId] = anchor
    }
  }

  fun get(waypointId: String): Anchor? = synchronized(lock) { anchors[waypointId] }

  fun remove(waypointId: String) {
    synchronized(lock) {
      anchors.remove(waypointId)?.let {
        try {
          it.detach()
        } catch (_: Exception) {
        }
      }
    }
  }

  fun clear() {
    synchronized(lock) {
      anchors.values.forEach {
        try {
          it.detach()
        } catch (_: Exception) {
        }
      }
      anchors.clear()
    }
  }

  fun snapshot(): Map<String, Anchor> = synchronized(lock) { anchors.toMap() }
}
