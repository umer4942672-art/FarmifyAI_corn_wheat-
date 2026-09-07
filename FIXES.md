# FarmifyAI — Applied Fixes

Seven issues found during the code review, and what changed for each.

---

## 1. `BACKEND_BASE_URL` resolved to an empty string

**Was:** `app/build.gradle.kts` read the backend URL with
`providers.gradleProperty("backendBaseUrl")`. The Secrets Gradle Plugin reads
`app/.env` into its *own* BuildConfig fields — it does not register Gradle
properties — and `gradle.properties` had the key commented out. So
`BuildConfig.BACKEND_BASE_URL` was `""`.

`ApiConfig.endpoint()` then threw, `SupabaseAuthService.post()` caught it as
`"Backend connection failed"`, and `UserRepository.login()` treated that as
"backend is down" and fell through to the offline Room path. The app looked like
it worked while nothing ever reached Supabase.

**Now:** the URL is resolved in order from `-PbackendBaseUrl`, `gradle.properties`,
then `app/.env`. Placeholder values (`your-project`, `your-backend`) are ignored.
Reading uses `providers.fileContents(...)` so the configuration cache re-runs when
`.env` actually changes. Gradle logs the resolved backend at build time, and warns
loudly when it is missing.

Files: `app/build.gradle.kts`

---

## 2. Refresh token was discarded

**Was:** the backend returned `refresh_token`, `SupabaseAuthResult` carried it,
but `AuthSessionStore.save()` only persisted the access token. There was no
refresh endpoint at all. Supabase access tokens last about an hour, after which
every cloud call 401'd, `send()` returned `false`, and the farmer was never told.

**Now:**
- `POST /api/auth/refresh` on the backend, backed by
  `SupabaseService.refresh()` (`grant_type=refresh_token`).
- `AuthSessionStore` persists the refresh token and exposes `updateTokens()`
  for rotation.
- New `AuthorizedApiClient` wraps every authenticated call: on a 401 it rotates
  the token once and replays the request. A `Mutex` guards the refresh so
  parallel 401s do not burn several refresh tokens. A rejected refresh token
  clears the local session so the UI can ask for a fresh login.
- Auth, data sync, image upload and chat all route through it.
- `getCurrentUser()` is now actually called on startup, so a restored "logged in"
  flag is validated (and silently refreshed) instead of trusted blindly.
- `POST /api/auth/logout` now really calls Supabase `/auth/v1/logout`, so the
  refresh token is revoked server-side. `UserRepository.logout()` calls it
  instead of only clearing local state.

Files: `backend/app/routers/auth.py`, `backend/app/services/supabase.py`,
`app/.../remote/AuthSessionStore.kt`, `AuthorizedApiClient.kt` (new),
`SupabaseAuthService.kt`, `SupabaseDataSyncService.kt`,
`KisanChatRepository.kt`, `UserRepository.kt`

---

## 3. `PlantImageGate` was dead code

**Was:** the pre-filter was fully written but never called. Any photo went
straight into the classifier, which can only answer with one of its trained
classes. Random noise scored 0.61 on the wheat model — above the 0.60 gate — so a
photo of a car could be reported as stripe rust.

**Now:** `analyzePlantImage()` runs `PlantImageGate.isLikelyPlant()` before
inference and returns a distinct non-plant result (`isPlantImage = false`) that
the UI already knows how to display. This is kept separate from the existing
low-confidence rejection so the two failure modes read differently to the farmer.

Files: `app/.../repository/DiseaseDetectionRepository.kt`

---

## 4. `/api/sync/bootstrap` was never called

**Was:** the endpoint existed and the README described a restore flow, but no
Android code called it. Signing in on a new phone showed an empty ledger and
empty scan history even though the records were in Supabase.

**Now:** new `CloudSyncRepository.restoreFromCloud()` fetches the bootstrap
payload and merges khata rows and disease scans into Room. Merging is
duplicate-safe: each row is checked against the local table first
(`countMatching` queries on both DAOs), so restoring twice does not double the
ledger. Restored rows are marked `isSynced = true`. Wired into login, signup and
guest sign-in in `MainViewModel`.

Note: cloud disease rows store a private storage path rather than a local file,
so restored history entries appear without a thumbnail.

Files: `CloudSyncRepository.kt` (new), `KhataDao.kt`, `DiseaseScanDao.kt`,
`MainViewModel.kt`

---

## 5. IDOR in `PUT /api/crops/{crop_id}`

**Was:**
```python
supabase.upsert('user_crops', {'id': crop_id, 'user_id': user['id'], **payload})
```
With `Prefer: resolution=merge-duplicates`, a caller who supplied someone else's
`crop_id` would overwrite that row *and* reassign `user_id` to themselves.

**Now:** a new `SupabaseService.update()` issues a `PATCH` scoped by **both**
`id` and `user_id`, so the statement can only ever match the caller's own row.
`id` and `user_id` are stripped from the incoming payload. An empty result
returns 404 rather than 403, so the endpoint cannot be used to probe which crop
ids exist.

Files: `backend/app/routers/data.py`, `backend/app/services/supabase.py`

---

## 6. Guest session never set `isAuthenticated`

**Was:** `startGuestSession()` stored the token and returned `Result<Unit>`.
`_profile.isAuthenticated` stayed `false` and no local user row was created, so
the dashboard opened but scan history and khata filtered on an empty user key —
and the next app launch bounced back to the auth screen.

**Now:** it creates a `UserEntity` keyed `guest-<uid8>`, marks it the active
session, updates `_profile` to authenticated, syncs the profile, and returns
`Result<FarmerProfile>`. The stored password hash is a random unusable value so
nothing can authenticate against the guest row offline.

Files: `app/.../repository/UserRepository.kt`, `MainViewModel.kt`

---

## 7. No sync retry, and deletes never propagated

**Was:** `isSynced` and `setSynced()` existed but nothing ever queried for
unsynced rows, so a row that failed to sync once stayed local forever.
`deleteScan()` and `deleteEntry()` only touched Room, leaving orphaned rows in
Supabase that reappeared on any restore.

**Now:**
- `KhataDao.getUnsyncedEntries()` / `getUnsyncedCount()` added;
  `KhataRepository.retryUnsyncedEntries()` and
  `CloudSyncRepository.retryPendingSync()` re-push them. Recent disease scans are
  re-pushed too — the backend upsert is idempotent (uuid5 of user id + local id),
  so replaying is safe. The retry runs on app start and after every login.
- `DELETE /api/sync/khata/{local_id}` and `DELETE /api/sync/disease/{local_id}`
  added. Both re-derive the row id from the caller's own user id, so a caller
  cannot target a record that is not theirs. The disease endpoint also removes
  the stored image via a new `SupabaseService.delete_storage()`.
- `KhataRepository.deleteEntry()` and `DiseaseDetectionRepository.deleteScan()`
  call them. `deleteScan()` also deletes the orphaned local JPEG, which was
  previously left on disk forever.

Files: `backend/app/routers/sync.py`, `backend/app/services/supabase.py`,
`KhataDao.kt`, `KhataRepository.kt`, `DiseaseDetectionRepository.kt`,
`SupabaseDataSyncService.kt`, `CloudSyncRepository.kt`

---

## No database migration needed

Only new queries were added — no Room columns or tables changed. `AppDatabase`
stays at version 5, so existing installs upgrade without a migration.

## Verification performed

- `python -m compileall backend/app` — clean.
- Every changed Kotlin file type-checked with `kotlinc` against stubs for
  Android, AndroidX Security, OkHttp, Room and `org.json` — no errors.
- Both TFLite models loaded and run: `plant_disease.tflite` (1×224×224×3 float32
  → 1×3 softmax) and `corn_disease.tflite` (→ 1×5 softmax), matching
  `labels.txt` and `corn_labels.txt`. The `.keras` source confirms MobileNetV2
  preprocessing (`x/127.5 - 1`) is baked into the graph, so the Kotlin side
  feeding raw `[0,255]` is correct and was left unchanged.

A real Gradle build could not be run here — Google's Maven repository is not
reachable from this environment — so please run `./gradlew assembleDebug` once
locally before your demo.

## Still open (not part of this pass)

- `CloudCropRepository` remains unused; there is no crops CRUD screen.
- Mandi rates are still seed data, not a live feed.
- The backend uses the Supabase service-role key for all REST calls, which
  bypasses RLS. Security depends on the backend filtering by `user_id`
  everywhere — it does — but the README's RLS claim is stronger than reality.
- The legacy `public.crops` table in `schema.sql` is superseded by `user_crops`.
