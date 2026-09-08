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

---

# Second pass — build errors and UI

## 8. Two compile errors in `FarmerUserVectorAvatar`

`:app:compileDebugKotlin` failed on `CommonComponents.kt`. Two separate bugs in
the same composable, both present in the original code:

- `val w = size.width` — inside the Canvas `DrawScope`, `size` resolved to the
  composable's `size: Dp` parameter, which shadows `DrawScope.size` and has no
  `width`/`height`. Fixed with an explicit `this.size`.
- `drawArc(..., Rect(...), ...)` — `DrawScope.drawArc` takes `topLeft: Offset`
  plus `size: Size`; the `Rect` overload belongs to `Canvas`, not `DrawScope`.
  Converted to `topLeft` + `size`.

## 9. Avatar redrawn

The old avatar was a face circle with two dots. Redrawn with a pagri and band,
beard, moustache, eyebrows, neck and kurta with a collar notch. All coordinates
are ratios of `w`/`h`, so it stays correct at every size the app requests.

## 10. Signup had no email field

`signupEmail` was declared and passed to `UserRepository.signup()`, but no input
field ever set it, so it was always `""`. In `UserRepository.signup`:

```kotlin
phone = if (cleanEmail == null) normalizePhone(cleanPhone) else null
```

Empty email meant every signup went to Supabase as a *phone* signup, which needs
an SMS provider. Without one, Supabase returned no session — yet the app still
created the local Room account and let the user in.

That single gap explains both reported failures: with no access token, every
authenticated endpoint fails. Chat returned "assistant unavailable" and nothing
reached the database, while offline features (weather, on-device detection)
carried on working, which made it look like two unrelated bugs.

An email field was added to the signup form, and validation now requires a
valid email while treating phone as optional profile data.

**Existing accounts created before this fix have no cloud identity.** Clear the
app's data and sign up again with an email.

## 11. Scan completion sound

New `util/ScanSound.kt` plays a ~1s tone via `ToneGenerator` when a scan
finishes — a double beep when a plant is recognised, a distinct error tone when
the image is rejected. Wired into `MainViewModel.analyzePlantBitmap`. No audio
asset ships in the APK, and failures (silent mode, no audio focus) are swallowed
so the scan result is never blocked.

## 12. Camera and gallery buttons

The two buttons had no fixed height, so their size shifted with label length
(worse in Urdu). Both now use `height(48.dp)` with `maxLines = 1`, and gallery
became an `OutlinedButton` — camera is the primary action, gallery secondary,
instead of two filled green blocks competing.

## 13. Navigation bar

Navigation was a `Crossfade` over a single state value with no history, so the
hardware/gesture back button closed the app from any screen.

Added a `backStack` plus a `BackHandler`. Back now returns to the previous
screen; on the dashboard with an empty stack the handler is disabled so the
system closes the app as expected. Splash and auth never accept back, and
auth-success and logout reset the stack so back cannot re-enter a signed-out
session. Nav labels got `maxLines = 1` and `softWrap = false` to stop the longer
Urdu labels wrapping to two lines.

---

# Third pass — settings screen

## 14. Language switcher was cramped

A two-line description and both language chips shared a single
`Arrangement.SpaceBetween` row. On narrow phones the text and the chips ran into
each other, and the chips were different widths because "English" and "اردو"
render at different lengths.

Now stacked: title and one-line description on top, then a full-width row of two
equal-weight buttons below. The selected side is filled green with a check mark,
the other is outlined, so the current language is obvious at a glance. Both
labels are `maxLines = 1`.

New `LanguageChoiceButton` composable in `SettingsScreen.kt`.

## 15. Notification rows

Each row was a bare title plus a switch, with no indication of what the alert
actually does. Rows now carry an icon and a one-line description, with more
breathing room between them.

The `diseaseAlerts` toggle existed on `FarmerProfile` but had no row in the UI at
all — it was impossible to change. Added.

Six new string keys in `LanguageManager.kt` cover the new labels and subtitles in
both English and Urdu.

## 16. Notification toggles did not persist

`UserRepository.toggleNotification()` only updated the in-memory `_profile`
flow. Nothing was written to Room, so every switch reset itself on the next app
start — the setting appeared to work until you closed the app.

It now writes the four flags back to the active Room user row. No schema change:
the columns already existed on `UserEntity` and were simply never updated.

---

# Fourth pass — dashboard and chat visuals

## 17. Quick action tiles redrawn as vector art

All four tiles used generic Material icons in the same layout, so at a glance
they were hard to tell apart. Two titles also carried emoji ("🌱 Field Work",
"📷 AI Doctor") that duplicated the icon beside them.

New `QuickActionArt` composable draws each action on a Canvas:

- Income and Expense — a stack of coins with a rising or falling arrow
- Field work — sun over ploughed furrows with a sprout in front
- Crop scan — a leaf under a magnifier

Everything is drawn from ratios of the available size, so it stays sharp at any
density and ships no extra drawables. Tiles changed from a 68dp horizontal row
to a 112dp vertical card: artwork in a tinted rounded square on top, then title
and subtitle. Emoji removed from labels, and the header no longer shouts in caps
or repeats the English name in brackets on the Urdu side.

Functionality is untouched — same four callbacks, same test tags.

## 18. Chat screen background

`KisanChatScreen` was a flat background colour. A farm photo
(`farm_hero_banner`) now sits behind the message list, blurred at 18dp and
faded to 30% opacity, with a vertical scrim over it so bubble text keeps full
contrast at the top and bottom where the header and input sit.

`Modifier.blur` is a no-op below API 31, so the low alpha plus the scrim carry
the effect on older devices rather than leaving a sharp photo behind the text.

---

# Fifth pass — dashboard emphasis swapped

## 19. Disease detection promoted, advisory moved to a tile

The dashboard led with a large "Kisan AI Advisor" hero banner, while disease
detection — the app's own trained model and its strongest feature — sat as one
small tile among four. The two swapped places:

- The hero banner is now **AI disease detection**. It opens the scan screen, the
  gradient moved from blue to green to match the rest of the app, the icon is a
  camera instead of a voice symbol, and the two prompt chips became "Wheat leaf"
  and "Corn leaf" (the two models actually bundled). Copy now states the scan
  runs on the phone, offline. Test tag renamed to `dashboard_disease_ai_card`.
- The fourth quick action tile is now **AI advisory**, opening the chat. New
  `QuickActionArtKind.ADVISORY` draws a speech bubble with a wheat ear inside.

`QuickActionArtKind.CROP_SCAN` and its artwork are kept, since the scan is still
reachable from the banner and the art may be reused.

---

# Sixth pass — auth errors were hidden

## 20. The real login failure reason never reached the user

`MainViewModel.login()` captured the exact message from Supabase and emitted it
to `_userFeedback`, but that snackbar is hosted by the Scaffold in
`MainActivity` — and the auth screen runs full screen, so it was never visible
there. The screen then showed a hardcoded "Login failed. Please verify
credentials." instead.

That single generic line covers several completely different problems:

- `Email not confirmed` — "Confirm email" is still on in Supabase
- `Invalid login credentials` — wrong password, or the account was created with
  a different provider (phone or anonymous)
- `Backend connection failed...` — the app never reached Vercel at all

Added `lastAuthError` to `MainViewModel`, set on every failed login and signup
and cleared on success. The auth screen now displays it and falls back to the
generic line only when there is nothing more specific.

Also `.trim()` on the login identifier — a trailing space from the keyboard was
enough to fail the match.

---

# Seventh pass — batch 1 of the UI request list

## 21. Weather never got a location

`refreshWeatherForCurrentLocation()` only called `getLastKnownLocation()` on the
GPS and network providers. That returns null whenever no app has requested a
position recently — the normal state on a fresh install and on most emulators —
so the app silently fell back to the saved district every time.

Rewritten in three steps: cached fix from *every* enabled provider (not just two),
then an active single-shot `requestLocationUpdates` with a 12-second timeout when
nothing is cached, then the district fallback. Fixes older than ten minutes are
skipped when a fresher one exists. If location services are switched off
entirely, the request returns immediately rather than hanging.

## 22. Chemical treatment card was cramped

The heading and the dosage-calculator toggle shared one `SpaceBetween` row. The
Urdu toggle label is long, so it squeezed the heading and the card looked
crushed. The toggle is now a full-width outlined button below the treatment text.

## 23. Ask AI advisory, from the diagnosis

New button under the treatment card opens the chat with the question already
written from the diagnosis — crop, disease name and severity — so the farmer does
not retype a disease name they just read. `MainViewModel.askAdvisoryAboutDiagnosis()`
builds it in the active language and `MainActivity` navigates to the chat.

## 24. Contact a pathologist

A model prediction is not a diagnosis, so the result screen now offers a route to
a person: a dialog with the Punjab Agriculture and Kisan Dost helplines, a dial
intent, and a note that the local extension officer can be reached at the tehsil
office. Both new buttons use the app's own greens (`ForestGreen` filled,
`EmeraldGreen` outlined) rather than new colours.

## 25. Save and Scan-another are now the same size

Neither button had a fixed height, so they sized to their own labels and the pair
looked mismatched. Both are `height(48.dp)` with `maxLines = 1`, and the outlined
one picked up a matching `SuccessGreen` border.

## 26. Dashboard order and season badge

The disease detection banner moved above the quick action grid — it is the app's
own trained model and the main reason the app gets opened, so it now sits where
the quick actions were.

The "Rabi 2026 / Wheat/Potato" badge was a flat grey-green box that read like a
disabled button. Redrawn as a warm stamp: soft amber gradient, gold border, and
`SeasonCropArt` — a wheat ear beside a potato — drawn on a Canvas.

---

# Eighth pass — batch 2

## 27. White-on-white text (root cause, not just the two reports)

The dosage-calculator input and the pathologist dialog were invisible because
`FarmifyTheme` switched to `MidnightDarkScheme` whenever the *system* was in dark
mode. Every screen in this app paints its own light surfaces as hardcoded colours
(`SoftWhite`, `Color.White`, `PaleGreenBg`), so `onSurface` flipped to near-white
while those backgrounds stayed white.

The system dark-mode flag no longer switches the scheme. Dark mode remains
available as an explicit choice — picking the Midnight theme in settings still
works. Following the system properly would first require moving every hardcoded
surface colour onto `MaterialTheme.colorScheme`, which is a larger change.

Two specific fixes on top: the acres field now pins its own text, label, cursor
and container colours instead of inheriting them, and the pathologist dialog sets
`titleContentColor`, `textContentColor` and `iconContentColor`.

## 28. Season badge shrunk

The two-line amber card was still too heavy for a header. Now a single compact
pill: 14dp crop art, the word "Rabi", nothing else.

## 29. Smart Khata card recoloured

Was a flat white card that disappeared into the page. Now a soft blue-to-white
gradient with a blue gradient wallet icon — blue keeps the money section visually
separate from the green crop cards around it.

## 30. Mandi rates got crop artwork

Each row now leads with a 46dp tinted tile containing artwork for that crop, so a
farmer can find a row by shape and colour instead of reading every name. `CropArt`
covers wheat, rice, maize, cotton, sugarcane, potato, tomato and onion, with a
generic leafy fallback, and `cropTint` gives each its own colour. Drawn on a
Canvas — no drawable assets added.

## 31. Farmer profile photo

`profilePhotoPath` added to `UserEntity` and `FarmerProfile`. Tapping the avatar
in settings opens the picker; a camera badge marks it as tappable.

`UserRepository.updateProfilePhoto()` copies the image into app-private storage
and stores the path. The gallery `Uri` is deliberately not stored — that
permission is revoked on restart, so a saved Uri would show a broken avatar the
next day. Images are downscaled to 512px before saving, and the previous photo is
deleted so files do not accumulate. `removeProfilePhoto()` reverts to the drawn
avatar.

New `FarmerAvatar` composable shows the photo when one exists and falls back to
`FarmerUserVectorAvatar` otherwise. The dashboard and settings both use it, so a
new photo appears in both places at once.

### Database migration

This is the first schema change, and the database had **no migrations and no
fallback at all** — any schema change would have crashed existing installs on
launch. Version moved 5 → 6 with `MIGRATION_5_6` adding the column to
`user_profiles`, plus a destructive fallback as a safety net for databases from
unreleased builds.

---

# Ninth pass — real dark mode, depth, header photo

## 32. Two themes, and dark mode that actually works

`AppThemeMode` had four entries; GOLDEN and EARTH were extra light palettes
differing only in accent colour, which is not a decision a farmer needs to make.
Reduced to **LIGHT** and **DARK**, and a theme toggle added to the settings
Appearance section.

Making dark mode usable needed real work. Screens reference `SoftWhite`,
`TextPrimary`, `BorderLight` and friends by name in roughly three hundred places,
all as top-level constants pinned to light values. Selecting a dark scheme
previously left those surfaces white while text turned near-white.

Rather than rewriting three hundred call sites, the constants became
**composable getters backed by a `CompositionLocal`**:

```kotlin
val SoftWhite: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.softWhite
```

Call sites are untouched; the values now follow the active theme. The raw light
values remain as `LightSoftWhite` and so on, because `Theme.kt` builds its
`ColorScheme` outside a composable context.

The system dark-mode flag is still ignored. Theme is an explicit choice, so a
farmer who wants the light UI in bright sunlight keeps it regardless of the phone
setting.

## 33. Smart Khata card

A white card with a blue tint was still not distinct enough. It is now the only
card on the dashboard with a solid colour: a deep teal-to-indigo gradient, an
amber wallet icon on a translucent tile, and a tinted drop shadow.

Every colour inside was re-picked for a dark surface — muted teal for labels,
white for headings, soft green and coral for profit and loss, translucent white
for dividers and the breakdown strip. Reusing the light-theme values would have
put dark text on a dark card.

## 34. Quick action tiles read as physical tiles

Each tile now carries a drop shadow tinted to its own accent colour, plus a faint
top-down wash that gives the surface a lit edge. The weather and disease hero
cards were lifted to matching elevation with tinted spot and ambient colours, so
the dashboard has a consistent sense of depth rather than a mix of flat and raised
cards.

## 35. Profile photo in the header

The dashboard greeting card was already switched to `FarmerAvatar`, but the
shared `FarmifyTopAppBar` — the bar carrying the Assalam-o-Alaikum greeting — was
still calling the drawn vector directly. It now takes a `profilePhotoPath` and
`MainActivity` passes it through, so the farmer's own photo appears in both
places. The bar's hardcoded white background also became theme-aware.

---

# Tenth pass — chat failures now name themselves

## 36. "Temporarily unavailable" hid four different problems

`getAgriAiResponse` treated a null from the backend call as one condition and
always showed the same line. That single message covered:

- no Supabase session on the device (the account exists only in Room)
- `BACKEND_BASE_URL` empty in the build
- the backend reachable but returning 401, 404, 503 or 504
- no network at all

`callCustomAgricultureApi` now returns a sealed `ChatOutcome` instead of a
nullable string, and each failure carries its own bilingual explanation:

| Condition | What the farmer sees |
|---|---|
| No backend URL compiled in | Rebuild with `backendBaseUrl` set |
| No cloud session | Sign in again with your email |
| Network unreachable | Check your connection |
| `401` | Session expired, sign in again |
| `404` | Chat endpoint missing — redeploy the backend |
| `503` | AI not configured on the server — check `GEMINI_API_KEY` |
| `504` | AI took too long — ask something shorter |
| Empty answer | Rephrase the question |

The most common cause is the second one. Chat, ledger sync and profile sync are
all authenticated endpoints, so an account that never obtained a Supabase session
fails at all three while offline features keep working — which makes them look
like separate bugs.

---

# Eleventh pass — a build problem was reporting itself as bad credentials

Diagnosis of a live failure: the backend, the Supabase account, the password and
the token issue all verified fine over curl, while the same credentials failed
inside the app. The APK had been built with an empty `BACKEND_BASE_URL`.

## 37. The empty-URL case was indistinguishable from a network outage

`ApiConfig.endpoint()` throws when no URL is compiled in. That throw happened
*inside* `SupabaseAuthService.post()`'s try block, so it was caught and turned
into `"Backend connection failed: ..."` — the exact string `UserRepository` uses
to decide the server is temporarily down and the offline path should be taken.

The user was then logged in locally, and every cloud feature failed for the rest
of the session. A wrong password and a missing build configuration produced
identical symptoms.

Three changes:

- `post()` checks `ApiConfig.isConfigured` **before** the try block and returns a
  message naming the real problem.
- `UserRepository` requires `ApiConfig.isConfigured` before treating a failure as
  a network outage, in both login and signup. A misconfigured build now fails
  loudly instead of degrading into offline mode.
- `app/build.gradle.kts` falls back to the known production URL when neither
  `-PbackendBaseUrl` nor `app/.env` yields one, so an unreadable or mis-encoded
  `.env` cannot produce an empty URL. Both overrides still take priority.

## 38. Backend status visible in the app

Settings → About now shows the compiled backend URL with an OK or MISSING badge.
Answering "does this APK have a backend URL?" previously meant scrolling Gradle
output; it is now one screen away on the device.

---

# Twelfth pass — a status code that pointed at the wrong thing

`POST /api/chat` returned 503 for two unrelated conditions: `GEMINI_API_KEY`
genuinely missing, and a catch-all around `chatbot.answer()` that swallowed every
runtime failure — a rejected key, a quota limit, a timeout, a Supabase read error.

The Android client mapped every 503 to "The AI service is not configured. Check
GEMINI_API_KEY", which sent the user to verify configuration that `/health` had
already reported as correct.

## 39. Upstream failures are now 502

The catch-all raises 502 with the underlying error text. 503 is reserved for
"Supabase or Gemini is not configured", which is what it actually means.

## 40. The client stops guessing

For 502 and 503 the client shows the server's own `detail` instead of a
hardcoded sentence. Attaching one presumed cause to a status code that carries
several is worse than showing the raw message.

---

# Thirteenth pass — Gemini key sent the wrong way

Google has migrated Gemini from `AIza` "traffic keys" to `AQ.` "authentication
keys", and AI Studio now issues only the new format. The new keys work on the
native `generativelanguage.googleapis.com` endpoint that this backend uses.

## 41. Key moved from the query string to a header

`GeminiService.chat()` appended the key as `?key=...`. It now goes in the
`x-goog-api-key` header, which is the documented way to pass either key format.

This also keeps the key out of the URL, where it would otherwise appear in proxy
logs, access logs and error traces.

---

# Fourteenth pass — Smart Khata card, third attempt

## 42. Text was unreadable on the dark card

The deep teal card from pass 33 was the wrong call. Making one card dark meant
hand-picking a replacement colour for every label inside it, and anything missed
stayed a light-theme colour on a dark surface. It also broke again under the dark
theme added in pass 32, where the surrounding page went dark too and the card
stopped standing out at all.

Rebuilt as a **tinted light card**, with the tint pulled from the app's own
emerald family so it matches the rest of the dashboard:

- Light theme: mint-to-white gradient (`#F1FBF4 → #DFF3E6 → #F6FCF8`)
- Dark theme: deep green gradient (`#162A22 → #1B3A2C`)

The gradient is chosen from `LocalFarmifySurfaces.current.isDark`, so it follows
the theme instead of being pinned to one.

Every label inside went back to the palette getters — `TextPrimary`,
`TextSecondary`, `SuccessGreen`, `ErrorRed`, `BorderLight`. Contrast is now
guaranteed in both themes by construction rather than by remembering to override
each colour.

The card still reads as its own section: an emerald-tinted background, an emerald
border, an emerald-tinted shadow, and a solid emerald-to-forest wallet tile with a
white glyph.
