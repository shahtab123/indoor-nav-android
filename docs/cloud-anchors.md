# Cloud Anchors setup

**Author:** Shahtab | Licensed under [Apache 2.0](../LICENSE)

This app saves markers with Google’s **ARCore Cloud Anchor API**.  
You need one thing configured correctly: an **API key** in `local.properties`.

Official docs: [Authorization](https://developers.google.com/ar/develop/authorization) · [Cloud Anchors](https://developers.google.com/ar/develop/java/cloud-anchors/developer-guide)

---

## What you are doing

1. Create your own Google Cloud project.
2. Enable the ARCore API in that project.
3. Create an API key.
4. Put that key in `app/local.properties`.
5. Rebuild and install the app.
6. Mark a spot with Wi‑Fi on — it should say saved to cloud (not “not authorized”).

**Do not** use `client_secret….json` (Desktop OAuth). The Android app ignores it.

---

## Values you will use

| Item | Value |
|------|--------|
| Google Cloud project | Create your own (for example, `my-indoor-nav`) |
| App package name | `com.google.ar.core.examples.kotlin.helloar` |
| Where the key goes | `d:\indoor map\app\local.properties` |
| Line to add | `ARCORE_API_KEY=your_key_here` |
| Marker lifetime (API key) | **About 24 hours** |

Links:

- [Create a Google Cloud project](https://console.cloud.google.com/projectcreate)
- [Enable ARCore API](https://console.cloud.google.com/apis/library/arcore.googleapis.com)
- [Credentials](https://console.cloud.google.com/apis/credentials)

---

## Step 1 — Create your Google Cloud project

1. Open [Create a Google Cloud project](https://console.cloud.google.com/projectcreate).
2. Enter any project name, such as **My Indoor Nav**.
3. Choose your organization and billing account if Google asks for them.
4. Click **Create** and wait for the project to finish provisioning.
5. Select your new project from the project picker in the Google Cloud top bar.

---

## Step 2 — Enable ARCore API

1. With your new project selected, open [ARCore API](https://console.cloud.google.com/apis/library/arcore.googleapis.com).
2. Confirm the project name shown in the Google Cloud top bar is **your project**.
3. Click **Enable**. If the button says **Manage**, it is already enabled.

---

## Step 3 — Create an API key

1. Open [Credentials](https://console.cloud.google.com/apis/credentials).
2. Confirm your project is still selected in the top bar.
3. Click **+ Create credentials**.
4. Choose **API key** (not “OAuth client ID”).
5. Copy the key string that appears.

---

## Step 4 — Restrict the key (recommended)

1. Still on Credentials, click the key you just created (or **Edit API key**).
2. Under **API restrictions**:
   - Choose **Restrict key**
   - Select only **ARCore API**
3. Under **Application restrictions**:
   - Leave **None** while testing
4. Click **Save**.

---

## Step 5 — Put the key in the project

1. Open this file in Cursor:

```
d:\indoor map\app\local.properties
```

2. Add or edit this line (one line, no quotes, no spaces around `=`):

```properties
ARCORE_API_KEY=paste_your_key_here
```

Example shape (fake key):

```properties
sdk.dir=D\:\\Android\\Sdk
ARCORE_API_KEY=AIzaSy........................
```

3. Save the file.

**Never commit** `local.properties` or the key to git.

---

## Step 6 — Install the app on your phone

1. Phone plugged in, USB debugging on, Wi‑Fi on.
2. In a terminal:

```powershell
cd "d:\indoor map\app"
.\gradlew.bat installDebug
```

3. If the build fails complaining about `ARCORE_API_KEY`, Step 5 is wrong or the file is in the wrong place (`app\local.properties`, next to `settings.gradle` / project root of the Gradle app module parent — path above).

---

## Step 7 — Confirm hosting works

1. Open the app on the phone.
2. Switch to **Map**.
3. Aim at a textured wall/floor → **Mark Spot** → give it a name.
4. Wait a few seconds.

**Success:** status like “Saved …” / “locked (survives app close).”

**Failure:** “Not authorized” / host failed →  
- Key missing or wrong in `local.properties`  
- ARCore API not enabled  
- No internet on the phone  
- Rebuild after changing the key (`installDebug` again)

---

## After marks are hosted

| Situation | What to do |
|-----------|------------|
| Close and reopen app | Walk near rooms; pins auto-retry resolve. Use **Scan again** if some stay missing. |
| Next day markers gone | Normal with API key — lifetime is ~**24 hours**. Remake spots in Map mode. |
| Want markers for months | Optional keyless Android OAuth (below). Not required for daily testing. |

---

## Cost (short)

Cloud Anchors are limited by **rate quotas**, not a per-host price:

| Quota | Limit |
|-------|--------|
| Hosts | 30 / minute |
| Resolves | 300 / minute |

A few apartment markers will not burn billable Cloud Anchor usage. Restrict the key to **ARCore API** only.

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Build: Missing `ARCORE_API_KEY` | Add the line in `d:\indoor map\app\local.properties`, save, rebuild |
| `ERROR_NOT_AUTHORIZED` | Enable ARCore API; check key; reinstall; Wi‑Fi on |
| Using `client_secret….json` | Delete that path from your mental model — app never reads it |
| Host works, reopen can’t find pins | Walk near the places you marked; tap **Scan again**; after ~24h remake markers |

---

## Optional later — 365-day markers (keyless)

Only when you want long TTL. Skip for now if the API key path works.

1. Select your project, then open [Credentials](https://console.cloud.google.com/apis/credentials) → **+ Create credentials** → **OAuth client ID**.
2. Application type: **Android**.
3. Package name: `com.google.ar.core.examples.kotlin.helloar`
4. Get the SHA-1 for your own debug signing key:

```powershell
cd "d:\indoor map\app"
.\gradlew.bat signingReport
```

5. Copy the **SHA1** shown under the `debug` variant into Google Cloud.
6. Wait a few minutes for Google to propagate.
7. In code, raise `TTL_DAYS` in `GoogleCloudBackend.kt` from `1` to `365`, then rebuild.

You do **not** paste any JSON into the app for this method.

---

## Code / architecture

- Host and resolve only run in `GoogleCloudBackend.kt`.
- Manifest gets the key as `com.google.android.ar.API_KEY` from the Gradle placeholder.
- See [architecture.md](architecture.md).
