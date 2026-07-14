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
package com.google.ar.core.examples.kotlin.helloar.navigation

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Navigation graph file only — separate from waypoints.json. */
object GraphPersistence {
  private const val TAG = "GraphPersistence"
  private const val FILE_NAME = "nav_graph.json"

  fun save(context: Context, graph: NavigationGraph) {
    val array = JSONArray()
    for (e in graph.edges) {
      array.put(
        JSONObject().apply {
          put("fromId", e.fromId)
          put("toId", e.toId)
          put("weight", e.weight.toDouble())
        },
      )
    }
    val file = File(context.filesDir, FILE_NAME)
    val tmp = File(context.filesDir, "$FILE_NAME.tmp")
    tmp.writeText(JSONObject().put("edges", array).toString(2))
    if (!tmp.renameTo(file)) {
      tmp.copyTo(file, overwrite = true)
      tmp.delete()
    }
    Log.i(TAG, "Saved graph (${graph.edges.size} edges)")
  }

  fun load(context: Context): NavigationGraph {
    val file = File(context.filesDir, FILE_NAME)
    if (!file.exists()) return NavigationGraph()
    return try {
      val root = JSONObject(file.readText())
      val array = root.getJSONArray("edges")
      val edges = buildList {
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          add(
            GraphEdge(
              fromId = obj.getString("fromId"),
              toId = obj.getString("toId"),
              weight = obj.optDouble("weight", 1.0).toFloat(),
            ),
          )
        }
      }
      NavigationGraph(edges)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to load graph", e)
      NavigationGraph()
    }
  }

  fun clear(context: Context) {
    File(context.filesDir, FILE_NAME).delete()
  }
}
