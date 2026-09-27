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

---

# Fifteenth pass — a self-inflicted startup stall

## 43. The retry pass re-uploaded fifty scans on every launch

`retryPendingSync` re-pushed the fifty most recent disease scans each time the
app started:

```kotlin
diseaseScanDao.getRecentScansForUser(userKey, 50).forEach { scan ->
    if (sync.syncDiseaseDetection(scan)) pushed++
}
```

That shortcut was taken in pass 7 because `disease_scans` had no sync flag and I
wanted to avoid a schema change. The backend upsert is idempotent, so no data was
harmed — but it meant up to fifty sequential HTTP requests on every cold start,
whether or not anything actually needed syncing. Once cloud sync began working
properly, that turned into a visible stall.

Fixed the right way rather than the cheap way:

- `isSyncedCloud` added to `DiseaseScanEntity`, with `MIGRATION_6_7`.
- `getUnsyncedScans()` replaces `getRecentScansForUser()`; the retry touches only
  rows that never reached the cloud.
- The save path marks a scan synced when its upsert succeeds, and rows arriving
  from `bootstrap` are inserted already marked synced.

On a device where everything is in sync, startup now issues **zero** retry
requests instead of fifty.

## 44. Overlapping retry passes

App start and a successful login both call `retryPendingCloudSync()`, so the two
passes could run concurrently and push the same rows twice. A `retryMutex`
serialises them, matching the existing guard on the restore pass.

---

# Sixteenth pass — landowner and contractor verification

A new feature rather than a fix. It was Phase 4 of the original proposal and is
built on the parts the app already had: Supabase auth, private storage, the
ledger, the location helper and the backend's ownership checks.

## The problem

A landowner who does not live at the farm has no record of what a contractor
actually did. Disputes are about the work ("you sprayed six acres, not ten"),
the timing, and above all the money ("I paid you" / "I never received it").

## Database

`supabase/migrations/002_contractor_verification.sql`, also appended to
`schema.sql`. Five additions:

- `profiles.role` — landowner or contractor, defaulting to landowner so no
  existing account changes
- `contractor_links` — a landowner may only assign work to a contractor who has
  accepted their invitation
- `work_orders` — the job and its agreed rate. `total_amount` is a generated
  column, so it cannot drift from `area_acres × rate_per_acre`
- `work_proofs` — photo path, GPS, accuracy, and **two** timestamps: the device
  clock and the server clock, because the device clock can be changed
- `work_payments` — a payment carries `status`, and counts only once the payee
  confirms it
- `work_order_balances` — a view computing `payable − paid`, where payable comes
  from **verified** acres, not the agreed area

## Backend

`backend/app/routers/work.py`, thirteen endpoints. Every one derives the caller
from the token and confirms they are a party to the record before touching it;
a record the caller is not party to returns 404, not 403, so ids cannot be
probed.

Rules enforced on the server, not the client:

- only the assigned contractor may accept, upload evidence or submit
- only the landowner may review, and only a submitted job
- only the payee may confirm a payment
- a digital payment requires a transaction reference
- submission requires at least one photo of the finished work
- partial approval must be above zero and below the agreed area
- a confirmed payment posts to both parties' ledgers, and the job closes itself
  once the balance reaches zero

`backend/tests/test_work_flow.py` exercises all of this against an in-memory
stand-in for Supabase. Twenty-eight checks, all passing.

## Android

- Room gains `pending_proofs` and `MIGRATION_7_8`. Evidence captured without a
  signal keeps the field's GPS fix and capture time and uploads later, the same
  offline-first path the ledger already uses
- `FieldLocation` takes a single fresh fix and reports Android's mock-location
  flag
- Capture is **camera only**. A gallery picker would allow an old photo taken
  elsewhere, which is the thing this evidence exists to rule out
- Sign-up asks for the account type; the choice is stored and sent once a
  session exists
- Reached from a dashboard card and from Settings, rather than a sixth item in
  the bottom bar

## What it does not do

Stated plainly because these will be asked about:

- distance from the field is **reported, not enforced**; the landowner decides
- a photo shows the contractor was there, not how much of the field was covered
- no money moves through the app. A transaction reference is recorded; sending
  payments needs a merchant agreement, and holding them needs a State Bank
  licence
- the mock-location flag is the only spoofing check

## 45. Every Supabase call opened its own connection pool

`supabase.py` created a fresh `httpx.AsyncClient` for each of its seventeen
call sites, and `gemini.py` did the same:

```python
async with httpx.AsyncClient(timeout=30) as client:
```

Each request therefore built a connection pool, performed a TLS handshake and
tore the pool down again. On Vercel's current Python runtime this exhausts the
sandbox's socket handles, and the next connect fails before it reaches Supabase
at all:

```
httpx.ConnectError: [Errno 16] Device or resource busy
```

The symptom is misleading. The service is reachable, the credentials are valid,
and the same request from a laptop succeeds — only calls made from inside the
deployed function fail, and they fail with a connection error rather than an
authentication one.

Replaced with a single shared client behind `http_client()`, an async context
manager that yields the same instance and does not close it. Connections are now
kept alive between calls, which also removes a TLS handshake from every request.

## 46. Nothing could reach Supabase from a new deployment

Every outbound call from a freshly deployed function failed at the TCP connect,
before any credential was checked:

```
httpx.ConnectError: [Errno 16] Device or resource busy
```

The symptom was misleading throughout. `/health` returned `healthy` because it
only inspects configuration and makes no network call. The Supabase project was
running, the keys were valid, and the identical request from a laptop succeeded.
Only calls made from inside the deployed function failed.

Three changes, each addressing a separate part of it:

**The HTTP client.** `supabase.py` created a fresh `httpx.AsyncClient` at each of
its seventeen call sites, and `gemini.py` did the same, so every request built
and tore down a connection pool. That exhausts the sandbox's socket handles.
A single module-level client is not the answer either, because a serverless
invocation may run on a different event loop from the one that created it. The
client is now keyed by event loop, its transport retries connection failures,
and a `ConnectError` discards the pooled client so the next call starts clean.

**The deployment config.** Switching `vercel.json` to a `rewrites` entry was
tried and reverted: rewriting to `/api/index` changes the path the application
receives, so every route returned 404. That 404 was still informative, because
it proved the application starts and serves requests on the current runtime -
the original failure was in its outbound calls, not its startup. The routing is
back to the form that preserves the request path.

**Error visibility.** An unhandled exception reached the client as a bare
"Internal Server Error" with an empty body, which is why this took so long to
place. A handler now returns the exception type, its message and the path, and
logs the traceback.

## 47. CORS blocked half the API

`allow_methods` listed only GET, POST and OPTIONS, while the API uses PUT for
crop updates and DELETE for ledger and scan removal. The Android client is
unaffected, since CORS is a browser rule, but any web client would have failed
at the preflight check. All the methods the API actually serves are now listed.

---

# Seventeenth pass — the microphone never listened

## 48. Voice input was an animation, not a feature

The advisory chat showed a microphone button that turned red and pulsed when
tapped. It did nothing else:

```kotlin
onClick = { viewModel.setVoiceListening(!isListening) }
```

That call only flipped a boolean driving the animation. The codebase contained
no `SpeechRecognizer`, no `RecognizerIntent`, and no `RECORD_AUDIO` permission.
The panel that opened offered three canned questions to tap, so a farmer
pressing the microphone and speaking got silence.

Text-to-speech was real — answers could be read aloud — so sound came out of the
app but never went in. For an application whose stated purpose is serving
farmers with limited literacy, spoken input is closer to the point than spoken
output.

Now implemented in `util/VoiceInputManager.kt`:

- recognition runs in the interface language, `ur-PK` or `en-PK`, with the other
  offered as a fallback because farmers mix the two mid-sentence
- partial results are shown live, so the speaker can see words being picked up
  rather than speaking into silence
- the silence window is widened to two seconds; the default cuts people off
  part-way through a question
- recognised text lands in the input box instead of being sent straight to the
  assistant, so a misheard question can be corrected first
- each failure gets its own message in both languages: no recogniser on the
  device, language unavailable, no network, permission refused, nothing heard
- the recogniser is released when the screen is left, rather than holding the
  microphone open

The manifest gained `RECORD_AUDIO` and a `<queries>` entry for
`RecognitionService`. Without that entry `isRecognitionAvailable()` returns
false on Android 11 and above no matter how capable the device is, which is a
common reason this appears to fail on modern phones.

**A limit worth stating:** Urdu speech recognition depends on the Google app
installed on the device and usually needs a connection. Where it is missing the
app now says so and invites the farmer to type, rather than failing silently.

## 49. The release APK could not be installed on any phone

`app/build.gradle.kts` defined a release build type with minification and
ProGuard, but no `signingConfig`. `assembleRelease` therefore produced an
**unsigned** APK, and Android refuses to install an unsigned package. Sharing
that file with anyone gave "App not installed" with no useful explanation.

A `release` signing config now reads from `keystore.properties`, which is
git-ignored along with `*.jks` and `*.keystore`. When that file is absent the
release build falls back to the debug key and logs a warning, so a fresh
checkout still produces an installable APK for testing without anyone having to
share a private key.

`keystore.properties.example` documents the four values and the `keytool`
command that creates the keystore.


## 50. Profile photos never left the phone

The profile photo was written to the app's private files directory and nowhere
else. Unlike the ledger, crops and scan history, it was lost on reinstall and
did not follow a farmer to a new phone. This was recorded as a limitation
rather than fixed.

Now stored in Supabase:

- `profiles.avatar_path` holds the object path; the image itself goes to a
  private `avatars` bucket, like the disease photos
- `POST /api/profile/avatar` uploads it. The object path is fixed per user
  (`{user_id}/avatar.jpg`), so a new photo replaces the old one instead of
  leaving orphaned files in storage
- `GET /api/profile/avatar` returns a short-lived signed URL for the caller's
  own photo only
- the upload happens after the local copy is written, so the photo appears
  immediately and an offline farmer is never blocked from setting one
- `restoreProfilePhoto()` runs as part of the existing cloud restore, and skips
  the download when a local photo already exists, so a photo changed offline is
  not overwritten by an older cloud copy

`AuthorizedApiClient.download()` fetches signed storage URLs without attaching
the session token, since those links carry their own authorisation and the
token has no business being sent to the storage host.

Run `supabase/migrations/003_profile_avatar.sql` before deploying this.

---

# Eighteenth pass — messages attached to a job

## 51. The two parties had nowhere to talk inside the record

A landowner and contractor could agree a job, submit evidence and settle
payment, but any discussion happened in a messaging app outside the system.
When they later disagreed, the conversation that explained the disagreement was
somewhere else, and could be deleted.

This is deliberately not a general chat. Every message belongs to one work
order, so the exchange sits beside that job's photos, GPS evidence and money.
A general chat would only duplicate WhatsApp, which both already have.

`work_messages` holds both typed messages and **system entries** written by the
backend at each transition: the rate accepted, the work submitted, the acres
verified, the payment confirmed. A readable history therefore builds itself
even when neither party types anything, and those entries carry no sender so
they cannot be mistaken for something a person said.

Read marks are per side, so each party sees its own unread count, and a sender
never sees their own message counted as unread. Opening a thread marks it read
on the server and refreshes the list, so a badge never lingers after it has
been seen.

The composer takes voice input, reusing the recogniser built for the advisory
chat. Spoken words land in the box for review rather than being sent straight
away, which matters for a contractor who may not read back easily.

Run `supabase/migrations/004_work_messages.sql` before deploying this.
The backend suite covers it: 40 checks, all passing.

---

# Nineteenth pass — the weather card

## 52. Four readings squeezed into one row

The weather card put humidity, wind and rain in a single row of pills with 9sp
labels and 11sp values. That is below what is comfortable to read outdoors, and
in Urdu the labels ran into each other. The agri advisory sat beside the
temperature in a narrow column, where the Urdu text wrapped badly.

Rebuilt with the temperature leading, since that is what the app is opened to
check, and the readings in a two-by-two grid instead of one cramped row:

- temperature at 68sp, with the condition beneath it in the accent colour
- four tiles, each with its icon in a tinted square, labels at 11sp and values
  at 16sp
- a fourth reading added: `feelsLikeC` was already in `CurrentWeather` and had
  never been shown
- the advisory now runs the full width, so Urdu has room
- a drawn radial glow behind the temperature rather than a bundled image

The dark green gradient and white text stay. A pale, low-contrast card in the
style of the design this borrows from looks better on a desk and is unreadable
in a field at midday, which is where this screen is actually used.

---

# Twentieth pass — themes that change the whole app

## 53. Accents were hardcoded, so a theme could only change the background

Surfaces already came from `LocalFarmifySurfaces`, which is what made dark mode
work. The accents did not: `EmeraldGreen` and `ForestGreen` were plain
top-level constants used directly in around two hundred places. A new theme
could therefore repaint the background and nothing else.

The same treatment the surfaces got has been applied to the accents. The raw
constants keep their values under new names (`GreenEmerald`, `GreenForest`,
`AccentGoldenYellow`), and the familiar names are now `@Composable
@ReadOnlyComposable` getters reading the active palette. Every existing call
site is untouched; which colour arrives depends on the chosen theme.

`Theme.kt` was the one place these were read outside composition, in the
Material colour schemes, and now uses the raw names. That is exactly the
mistake made once before when the surfaces were converted, so it was checked
across the whole source tree rather than assumed.

Four themes, each with its own surfaces, accents and Material scheme:

| Theme | Character |
|---|---|
| Light | the existing forest and emerald green |
| Dark | green on near-black, accents lifted so they still read |
| Harvest | wheat and clay, for farmers who find the green cool |
| Midnight | deep blue-teal, a cooler dark |

Harvest borrows from the warm agricultural designs that inspired it but keeps
the green theme's text contrast. Those designs place pale cream on cream, which
settles well indoors and disappears in a field at midday.

The palette also carries `heroGradient`, so the weather card's gradient follows
the theme instead of staying green whatever is chosen.

## 54. The chosen theme was forgotten at every launch

`_currentTheme` lived only in memory, so the selection reset to Light each time
the app opened. Now written to preferences and read back on start, with an
unknown stored value falling back to Light rather than crashing.

## 55. Harvest given its own layout, not just its own colours

Harvest now carries a distinct layout as well as a palette, so switching to it
changes how the app is arranged rather than only what colour it is. Everything
here is gated on the active theme, so Light, Dark and Midnight keep the
existing arrangement untouched.

**Floating navigation.** A rounded bar inset from the edges instead of the
full-width bar flush against the bottom. Only the selected item shows its
label: five labels in a pill this narrow would each be clipped, and a clipped
Urdu label is worse than no label, so the selected item expands to show its own
while the rest stay as icons large enough to hit.

**Crop chips and a field card.** The designs this draws on get most of their
warmth from stock photography. Photographs would add megabytes to the APK, need
licensing, and be the same generic fields every other farming app uses, so the
imagery is drawn with Canvas instead: six crop marks and a wheat field with sky,
sun, hills and rows that shorten towards the horizon. It costs nothing to ship,
recolours with the theme, and stays sharp at any density.

There is a second reason for drawing the field rather than photographing one: a
stock photograph of somebody else's farm sitting under the heading "My fields"
misrepresents what the farmer is looking at.

## 56. Palette colours read from places that are not composable

Turning the accents into `@Composable` getters introduced a class of error the
constants could never have: a palette colour read from a scope that is not
composable. Two cases turned up on the first build.

`LazyColumn`'s builder lambda is a `LazyListScope`, not a composable scope, so
`LocalAppTheme.current` cannot be read there. The theme is now read once in the
composable body and the lazy scope uses the resulting boolean.

`Canvas`'s `onDraw` is a `DrawScope`, so a palette colour cannot be read inside
it either. The weather card's glow colour is now resolved just outside the
lambda and captured.

The whole `ui` tree was then scanned by matching braces from each `Canvas`,
`drawBehind` and `drawWithContent` opening and checking the body for palette
names, rather than assuming those two were the only ones. No other site.

---

# Twenty-first pass — speech that says when it cannot speak

## 57. Text-to-speech failed silently, and could stick

Three faults, each hidden by a `catch` that did nothing.

**Urdu fell through to an engine that cannot read it.** When Urdu was
unavailable the helper tried Hindi, then English. Urdu is written in a
Perso-Arabic script, so a Hindi or English voice produces nothing useful from
it. The farmer heard silence or noise and was told nothing. Urdu now falls back
only to another Urdu locale; if none exists the farmer is told to read the text
or switch to English.

**The button could stick.** `tts.speak()` returns `ERROR` when the engine
refuses a request, and that result was ignored while `_isSpeaking` had already
been set true. The flag then never cleared, so the next tap was read as "stop"
and nothing played again until the app restarted. The result is now checked and
the flag cleared on refusal.

**`setLanguage()` was never checked.** Only `isLanguageAvailable()` was, and the
two can disagree: a voice is listed but its data has not been downloaded. That
case now reports `LANG_MISSING_DATA` and points at the phone's settings.

Also fixed: the currency and bullet substitutions were applied to Urdu as well,
inserting the English word "rupees" into Urdu sentences. They are now
English-only. Initialisation retries are capped at three, since a failed engine
keeps failing and the old code re-initialised on every tap.

Failures now travel through an `errors` flow to the existing snackbar, in both
languages.

## 58. A collector placed above the property it read

The first version of that wiring put the collector in an `init` block beside
`voiceHelper`, near the top of `MainViewModel`, while `_userFeedback` is
declared some two hundred lines below. Kotlin runs property initialisers and
init blocks in source order, so this would have read an uninitialised property
at construction and crashed the app on launch. The block now sits directly
after the flow it uses.

---

# Twenty-second pass — a full inspection of v33

Everything in the project was checked rather than only the recent work.

## What passed

- The backend compiles, loads all 36 endpoints, and its suite passes 40 of 40
- All 64 Kotlin sources parse cleanly
- No palette colour is read from a `Canvas`, `drawBehind` or `drawWithContent`
  body, checked by matching braces from each opening rather than by eye
- `PendingProofEntity` matches `MIGRATION_7_8` column for column, including
  nullability, which is what Room verifies at open
- Every endpoint the Android client calls exists on the backend
- No key, keystore or `.env` is committed; `.gitignore` covers all three. The
  two files that matched a key pattern were comments explaining Google's key
  format change, not keys
- No leftover TODO, patch script or dead function

## 59. The README described a system two versions old

The only real problem the inspection found, and it was in the documentation
rather than the code.

Two limitations listed there had already been fixed. Profile photos were still
described as local-only and lost on uninstall, which stopped being true when
they moved to Supabase Storage. Dark mode was still described as opt-in because
screens paint hardcoded light surfaces, which stopped being true when the
accents moved into the palette.

Nothing added since the contractor work appeared at all: no work orders, no
evidence, no payments, no messages, no voice input, no themes. A reader would
have taken the README for the whole system and missed roughly a third of it.

Now covering the contractor and messaging features, their fifteen endpoints,
the five new tables, the generated `total_amount` column and the
`work_order_balances` view, Room at schema version 8 with `pending_proofs`, the
migrations directory, the three storage buckets, and how to run the backend
test suite.

---

# Twenty-third pass — the dashboard, and two invisible-text bugs

## 60. Section headings were close to invisible on the dark themes

`SectionHeader` in `CommonComponents.kt` painted its title with a fixed slate,
`Color(0xFF334155)`. That is dark-on-light by design, so on Dark and Midnight it
sat almost on top of the background, and on Harvest it was the wrong tone
entirely. It now uses `TextPrimary`, which follows the palette.

This is the same class of bug as the original white-on-white problem: a colour
chosen for one theme and then used everywhere.

## 61. The season pill disappeared on Harvest

The Rabi chip hardcoded a cream background with brown text. Against Harvest's
cream surface it had almost no edge. Now tinted from the palette's gold, so it
keeps its contrast whichever theme is on.

## 62. The main features were three cards down the page

Reaching the scanner or the ledger meant scrolling past the weather card, the
financial snapshot and a disease banner. The four things the app exists to do
now sit directly under the weather as a two-by-two grid: Smart Khata, Disease
Detection, Contractor Work and the advisory.

Each tile carries a 26dp icon in a 52dp block rather than the 20dp used
elsewhere, because farmers who read slowly navigate by shape. On the pale
themes a tile takes a wash of its own accent, which is what separates the four
at a glance; on the dark themes a wash would muddy them, so the surface colour
is used instead.

The scanner tile breathes, scaling between 1.0 and 1.04 over 1.6 seconds.
Anything faster is tiring on a screen checked many times a day.

## 63. The weather card cost a scroll

It had grown tall enough to push everything else below the fold: a 68sp
temperature and four readings in a two-by-two grid. Now a 46sp temperature and
three readings across one row, which roughly halves its height. The fourth
reading, feels-like, was dropped rather than shrunk: humidity, wind and rain are
what irrigation and spraying decisions turn on.

## 64. The illustrated sections were Harvest-only

The crop row and field card were written for Harvest and gated on it, so Meadow
arrived with the palette and the feature grid but none of the illustration that
made the reference design look the way it does.

Both now appear on Harvest and Meadow, renamed to `IllustratedCropChips` and
`IllustratedFieldCard` since they no longer belong to one theme, and moved above
the financial snapshot rather than sitting at the foot of the page.

The artwork adapts. Each crop carries two tints: Harvest keeps the clay browns,
while Meadow takes fresher colours, because those browns look muddy on a
near-white background. The field itself is ripened gold on Harvest and green on
the cooler themes - a golden landscape on a pale green page reads as a mistake
rather than a choice.

The first version of that switch inferred the theme from a colour channel
(`amberOrange.red > 0.7f`), which is both obscure and wrong, since both palettes
have a high red component in that accent. It reads `LocalAppTheme` directly now.

## 65. Meadow

A fifth theme, near-white with saturated greens, in the style of the reference
design. Its accents are deliberately strong: on a near-white background weak
accents leave the feature tiles indistinguishable, which is how pale designs
usually fail.

## 66. A tasks section built from real state, not invented rows

The reference design shows a "Today's Tasks" list. This app has no task
feature, so building that literally would have meant printing plausible rows
that correspond to nothing — "Field Inspection, 09:00 AM" against no stored
task. In a project whose whole argument is that it refuses to guess, a
decorative list of fake work would be the wrong thing to add.

The section instead reports what is genuinely outstanding:

- work waiting on this user's decision, which differs by role: a landowner
  reviews submitted jobs, a contractor accepts new ones and resubmits disputed
  ones
- unread job messages, summed from the counts the backend already returns
- ledger rows that never reached the cloud, derived from the entries flow so it
  settles by itself once a retry succeeds
- a spraying warning when rain is 50% or more likely, and an irrigation nudge
  when it is 20% or less

The rain rows are worth their own line rather than a note in the weather card:
a spray washed off by rain costs both the chemical and the day.

When nothing is outstanding the section is hidden rather than showing an empty
state, so a farmer with nothing to do sees a shorter page instead of a box
telling them so.
