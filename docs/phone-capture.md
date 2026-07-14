# How to capture your apartment with your phone

**Author:** Shahtab | Licensed under [Apache 2.0](../LICENSE)

This guide explains **what you need to capture** for Indoor Nav and **exact steps**. You do **not** need a floor-plan image or a 2D map — the app is **AR-only** (Map mode + Navigate mode).

For USB / install: [`setup.md`](setup.md). For Cloud Anchors: [`cloud-anchors.md`](cloud-anchors.md). Roadmap: [`plan.md`](plan.md).

---

## Short answer

**No whole-room video. No 2D floor plan PNG. No LiDAR.**

| You do **NOT** need | You **DO** need |
|---------------------|-----------------|
| Full apartment video | A simple **paper sketch** (optional but helpful) |
| Floor plan PNG / 2D map tab | **4–6 named waypoints** marked in AR |
| Blank white walls for marks | **Good light** + textured surfaces |
| Perfect millimeter accuracy | **Connections** between walkable spots |
| Standing still for minutes | **Normal walking** while AR tracks |

**How it works:** ARCore tracks you live from the camera. You walk once, mark spots in **Map** mode, link them in **Connections**, then follow guidance in **Navigate**. Markers are hosted with Cloud Anchors (Wi‑Fi).

---

## Before you start

- [ ] App installed (`setup.md`)
- [ ] Cloud Anchors API key set (`cloud-anchors.md`)
- [ ] Open app → colored dots appear when you move (tracking OK)
- [ ] You can switch **Map** / **Navigate** at the top

---

## Step 1 — Paper sketch (optional, ~10 min)

Draw a rough top-down layout and pick **4–6 standable** spots. Same names you will type in the app:

```text
Entrance
Hallway
Kitchen
Bedroom
Bathroom
```

Good spots: doors, hallway, room centers — near texture, on paths you actually walk.

```text
+---------------------------+
|  Bedroom          Bathroom|
|      *                 *  |
|         Hallway *         |
|  Kitchen *                |
|  * Entrance               |
+---------------------------+
```

---

## Step 2 — Mark waypoints in AR (Map mode)

**Time:** ~15–20 minutes. Wi‑Fi on.

1. Open app → **Map**
2. Stand at your first spot (e.g. Entrance)
3. Move the phone slowly until dots appear
4. Aim the crosshair at a wall/floor → **Mark Spot** → type the name (e.g. `Entrance`) → confirm type (Door / Room / Hallway / Spot)
5. Wait until status shows cloud save success (not “not authorized”)
6. Walk to the next spot and repeat

### Tips

| Do | Don't |
|----|-------|
| Aim at textured walls / door frames | Blank walls, mirrors, TVs |
| Wait for tracking before Mark Spot | Mark while “tracking lost” |
| Finish in one session if you can | Mark everything from one place |
| Use names from your sketch | Rename randomly later |

### After marking

1. Force-close and reopen the app  
2. Walk those rooms — pins should reappear (auto-retry). Use **Scan again** if some stay missing  
3. After ~24 hours with API key, cloud markers expire — remake if needed  

---

## Step 3 — Link walkable connections (Map mode)

**Time:** ~5 minutes. No special walking required after pins exist.

1. Map → **Connections**
2. Tap pair of spots that you can walk between directly (no third room required)
3. Example:

```text
Entrance ↔ Hallway
Hallway ↔ Kitchen
Hallway ↔ Bedroom
Hallway ↔ Bathroom
```

Save/close when done. This builds the graph A* uses for Navigate.

---

## Step 4 — Test navigation (Navigate mode)

**Time:** ~15 minutes.

1. Switch to **Navigate**
2. Pick **Start** (or nearest) and **Destination** (e.g. Kitchen)
3. Path text should look like `Entrance → Hallway → Kitchen`
4. Hold the phone up and walk — follow cyan path dots + the turn banner (“Straight / Turn left · …”)
5. Walk until you hear/see **Arrived**

### Note problems (write a list, don’t film)

```text
- Hallway near bathroom: tracking weak (blank wall)
- Kitchen: worked well
- Dark bedroom corner: lost tracking
```

Fix: better light, remake a pin slightly elsewhere, or fix Connections — not by recording video.

---

## What the screen means

```text
Camera → textured surfaces → ARCore tracks you in 3D
                                    ↓
                     waypoints = saved Cloud Anchors in that space
                                    ↓
                     Connections + A* = route for Navigate
```

- **Dots / point cloud (Map)** = tracking is healthy  
- **No dots** = move slower, add light, aim at floor/furniture  
- **Finding markers…** after reopen = walk near those rooms; **Scan again** if stuck  

---

## Checklist (current app)

| Step | Action |
|------|--------|
| 1 | Confirm AR tracking (dots) |
| 2 | Optional paper sketch + names |
| 3 | Map → mark 4–6 waypoints (cloud save OK) |
| 4 | Map → Connections (walkable links) |
| 5 | Reopen + walk rooms so pins resolve |
| 6 | Navigate → pick destination → follow dots / banner |

---

## Common mistakes

| Mistake | Fix |
|---------|-----|
| “I need a floor plan PNG first” | No — this app has no 2D floor plan yet |
| Marking every waypoint from one spot | Walk to each place |
| Marks on mirrors / glass | Use wall / floor texture |
| Different names paper vs app | Match exactly |
| Testing in the dark | Turn lights on |
| Expecting GPS accuracy | Room-level is the goal |

---

## Minimum useful apartment

1. **4 waypoints:** Entrance, Hallway, Kitchen, Bedroom  
2. **3–4 connections** through Hallway  
3. **One Navigate test:** Entrance → Kitchen  

Add Bathroom and more later.

---

## When to redo

| Situation | Redo |
|-----------|------|
| Moved furniture a lot | Remake nearby waypoints |
| Cleared all markers | Mark + Connections again |
| Arrow/path wrong room | Fix Connections |
| Day later, pins gone | Remake (API key ~24h TTL) |

---

## Related docs

- [`setup.md`](setup.md) — install & first AR test  
- [`cloud-anchors.md`](cloud-anchors.md) — API key so marks survive reopen (24h)  
- [`plan.md`](plan.md) — product roadmap (2D floor plan is a **future** phase, not in the app now)
