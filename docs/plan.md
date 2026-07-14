# IndoorNav plan

**Author:** Shahtab | Licensed under [Apache 2.0](../LICENSE)

**This app today:** AR-only apartment nav — **Map** (mark + connections) and **Navigate** (path + guidance).  
**No 2D floor-plan map yet** (that comes later with import / Phase 2–3).

**Product:** open-source indoor navigation platform. Localization is a plug-in, not the product.

| Mode | Job |
|------|-----|
| **Map** | Mark spots, edit/clear, link Connections |
| **Navigate** | Pick start + destination, follow path cues |

Same APK, same `waypoints.json` + `nav_graph.json`.

---

## Architecture (short)

**v1 localization:** Google Cloud Anchors ([cloud-anchors.md](cloud-anchors.md)).  
**Later:** **ISP** client via `LocalizationBackend` — separate repo ([future-plan.md](future-plan.md)).  
**Not in this app:** OpenVPS / RTAB / ORB-SLAM integrations.

```
IndoorNav UI / AR / Waypoints / A*
              │
      LocalizationBackend
              │
   GoogleCloudBackend (today) | IspBackend (future → ISP server)
```

Details: [architecture.md](architecture.md)

---

# Product roadmap

## Phase 1 ✅ — Core (done)

| Piece | Status |
|-------|--------|
| AR Map / Navigate modes | ✅ |
| Waypoints + save/load | ✅ |
| Connections + A* | ✅ |
| Cloud Anchors (host + resolve + auto-retry) | ✅ |
| Path dots + turn banner | ✅ |
| **2D floor-plan view** | ❌ not built — later |

**How to map a home:** [phone-capture.md](phone-capture.md)

---

## Phase 2 ⏳ — Navigation platform (next)

Do this **before** import tools.

- Better route visualization  
- Stronger turn-by-turn + voice  
- Search destinations / favorites  
- Multi-floor  
- Route profiles (stairs / wheelchair / shortest)  
- Route preview + recalculate  

Near-term slice (same phase): polish + small upgrades below.

---

## Phase 3 — Apartment import

**Do not build a CAD editor.** Import existing layouts → internal nav map.

Inputs (target): PNG/JPG, PDF, SVG, GLB/glTF, OBJ, sketch photo; simple creator only if nothing else exists.

---

## Phase 4 — Apartment intelligence

Auto-suggest rooms, doors, walls, waypoints, connections, stairs/elevators — user reviews and edits.

---

## Phase 5 — Sharing

Export/import apartment, QR share, sync, library, collaboration, versions.

---

## Phase 6 — ISP (separate project)

**Indoor Spatial Platform** — full plan in [future-plan.md](future-plan.md).

```
Phone camera frames → ISP server → Pose → IndoorNav (LocalizationBackend)
```

This repo stays navigation-only. ISP is its own track (Phase 1B → 7 there).

---

## Phase 7 — AI features

Auto labels/POIs, voice destination, NL search, accessibility (optional furniture-aware).

---

## Long-term shape

```
Apartment sources (images / 3D / sketches)
              ↓
     Internal floor plan   ← Phase 3–4; not in app yet
              ↓
   Rooms / doors / POIs → waypoints → graph → A*
              ↓
        ┌─────┴─────┐
   2D nav (later)  AR nav (today)
        └─────┬─────┘
              ↓
        Localization
   Cloud Anchors today | ISP later
```

### Do not build

- CAD / full apartment editor  
- OpenVPS / RTAB / ORB-SLAM inside IndoorNav  
- Duplicating floor-planning apps  

---

# Build history + near-term work

Setup (JDK, SDK, USB) — see [setup.md](setup.md) if you need a new machine.

| Stage | Status | Notes |
|-------|--------|--------|
| 1 ARCore works | ✅ | Point cloud / tracking |
| 2 Full-screen AR | ✅ | Camera + tracking |
| 3 Mark Spot | ✅ | Types + labels |
| 4 Save + Map/Navigate | ✅ | JSON persistence |
| 5 A* + Connections | ✅ | `nav_graph.json` |
| 6 AR guidance | ✅ | Dots + turn banner + arrival |
| 7 Polish | ⏳ | Feeds Phase 2 |
| 6.5 Nav upgrades | ⏳ | Feeds Phase 2 |

### Optional — QR assist

Only if drift is painful: scan a QR at a known waypoint to snap pose. App must work without QR.

### Stage 7 — Polish (todo)

1. Clear banner when tracking is lost  
2. UI cleanup  
3. Camera permission rationale on first launch  
4. Rename in Saved markers list  
5. Keep Navigate free of point-cloud clutter  

### Stage 6.5 — Nav upgrades (todo)

1. Clearer next-stop distance (banner already has some — improve UX)  
2. Stronger path visualization  
3. Arrival haptic  
4. Search/filter destinations  
5. Export map JSON (share sheet); import later  

### Later — multiple saved maps

Today: one `waypoints.json`. Later (with Phase 5): named maps, switch, export/import.

---

## Current focus

1. Stage 7 + Stage 6.5 → **Phase 2**  
2. Keep Cloud Anchors for v1  
3. ISP work lives in [future-plan.md](future-plan.md) — not this checklist  

**Rule:** ship navigation value first; localization is a plug-in.
