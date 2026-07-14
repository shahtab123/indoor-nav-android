# Setup — phone, laptop, and first install

**Author:** Shahtab | Licensed under [Apache 2.0](../LICENSE)

Get Indoor Nav running on your phone. The app is **AR-only** (Map + Navigate). There is **no 2D floor-plan tab**.

**App folder:** `d:\indoor map\app`

Also see: [cloud-anchors.md](cloud-anchors.md) (API key) · [phone-capture.md](phone-capture.md) (marking your apartment)

---

## 1. Phone supports AR

1. Search: **ARCore supported devices**
2. Open Google’s list and find your model (**Settings → About phone → Model**)
3. If it is not listed, this app will not work on that phone

Use a **data** USB cable (not charge-only).

---

## 2. Developer Options + USB debugging (one-time)

1. **Settings → About phone** → tap **Build number** 7 times  
2. Back → **Developer options** → turn on  
3. Enable **USB debugging** → confirm Allow  

---

## 3. Android SDK / `adb` on the laptop (one-time)

You need `adb` from the Android SDK. Easiest path: install **Android Studio once**, then close it (Cursor is the editor).

1. Download Android Studio (Windows) from Google  
2. Install with **Android SDK** checked  
3. Open Android Studio once → **Standard** setup → wait for SDK download → close  

Default SDK path:

```
C:\Users\<YOU>\AppData\Local\Android\Sdk
```

`adb` lives in `platform-tools\adb.exe`.

If missing: Android Studio → SDK Manager → **SDK Tools** → check **Android SDK Platform-Tools** → Apply.

*(Optional: put Studio/SDK on D: — see appendix at the bottom.)*

You also need **JDK 17+** on the laptop for Gradle builds.

---

## 4. Connect the phone

1. Plug in USB  
2. On phone: **Allow USB debugging** → check **Always allow**  

### Check connection

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

Want:

```
List of devices attached
XXXXXXXX    device
```

| Result | Fix |
|--------|-----|
| `device` | OK — continue |
| `unauthorized` | Unlock phone, tap Allow, run again |
| Empty | Other cable; USB mode = File transfer / MTP |
| `adb` not recognized | Use full path above, or finish Section 3 |

Optional: add `...\Android\Sdk\platform-tools` to your user **PATH**, then `adb devices` works.

---

## 5. Cloud Anchors API key (required for saving markers)

Before you map an apartment, set the key once:

→ Follow **[cloud-anchors.md](cloud-anchors.md)** (put `ARCORE_API_KEY=...` in `d:\indoor map\app\local.properties`).

Without it, Mark Spot stays local / may show not authorized.

---

## 6. Install the app

```powershell
cd "d:\indoor map\app"
.\gradlew.bat installDebug
```

First run can take several minutes. Success ends with **BUILD SUCCESSFUL** and install on the phone.

---

## 7. Open the app

1. Find **Indoor Nav** in the app drawer  
2. Allow **Camera**  
3. If prompted, install **Google Play Services for AR**, then reopen  

---

## 8. First test — AR tracking works

You should see the live camera (full screen AR), not a 2D map.

1. Point at floor / walls / furniture  
2. Move the phone slowly  
3. Colored tracking dots should appear (Map mode shows the point cloud)

**Success:** dots on surfaces.  

Then check UI:

| Control | Expected |
|---------|----------|
| **Map** / **Navigate** switch | Two modes at the top |
| **Map** | Crosshair, Mark Spot, Connections, Saved markers |
| **Navigate** | Destination list, path guidance when you pick a goal |

If tracking works and both modes open, setup is done.

Next: mark your apartment — [phone-capture.md](phone-capture.md).

---

## Checklist

- [ ] Phone on ARCore supported list  
- [ ] USB debugging on; `adb devices` → `device`  
- [ ] JDK 17+; SDK with Platform-Tools  
- [ ] `ARCORE_API_KEY` in `app\local.properties`  
- [ ] `.\gradlew.bat installDebug` succeeded  
- [ ] Camera + Play Services for AR allowed  
- [ ] Dots appear when you walk around  

---

## Problems

| Problem | Try |
|---------|-----|
| `adb` not found | Section 3; full path to `adb.exe` |
| No USB popup | Other cable; File transfer mode |
| Unauthorized | Unlock + Allow USB debugging |
| Device not AR | Wrong phone |
| Black camera / no dots | Camera permission; Play Services for AR; better light/texture |
| Missing `ARCORE_API_KEY` on build | [cloud-anchors.md](cloud-anchors.md) |
| Host “not authorized” | Fix API key, rebuild, Wi‑Fi on |

If stuck, report: phone model, `adb devices` output, what the app shows, any red Gradle text.

---

## Appendix — Android Studio / SDK on D: (optional)

For Studio + SDK on D: instead of C:.

Example installer path (yours may differ):

```
D:\Android\Installers\...
```

1. Install Studio to `D:\Android\AndroidStudio`  
2. First-launch wizard: set SDK to `D:\Android\Sdk`  
3. Verify:

```powershell
& "D:\Android\Sdk\platform-tools\adb.exe" devices
```

Optional: PATH = `D:\Android\Sdk\platform-tools` · env `ANDROID_HOME` = `D:\Android\Sdk`

Then continue from Section 4.
