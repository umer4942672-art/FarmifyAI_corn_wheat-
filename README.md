# FarmifyAI

An Android agriculture assistant for Pakistani farmers. It diagnoses wheat and
maize leaf diseases on the device, keeps a bilingual farm ledger, and answers crop
questions in Urdu or English — with the network treated as optional throughout.

The app is built around one constraint: a farmer standing in a field often has no
usable signal. Disease detection therefore runs entirely on the phone, records are
written locally first, and anything that needs the cloud syncs when it can.

---

## Contents

- [Features](#features)
- [Architecture](#architecture)
- [Security model](#security-model)
- [Repository layout](#repository-layout)
- [Setup](#setup)
- [API reference](#api-reference)
- [Data model](#data-model)
- [Machine learning models](#machine-learning-models)
- [Testing](#testing)
- [Known limitations](#known-limitations)
- [License](#license)

---

## Features

**Disease detection.** Two bundled TensorFlow Lite classifiers analyse a
photographed leaf on the device. A pre-filter rejects images that are not foliage,
so a photo of something else does not come back labelled as a disease. A
confidence floor and a margin check between the top two classes suppress uncertain
predictions rather than presenting them as answers.

**Bilingual throughout.** Every screen renders in Urdu or English, including
diagnoses, treatment advice and chat. Text-to-speech reads diagnoses aloud for
users who prefer listening.

**Smart Khata.** An income and expense ledger scoped per farmer, with crop-wise
profitability and a spray dosage calculator that converts acreage into water
volume and tank counts.

**Offline first.** Detection, the ledger and the crop guide work with no
connection. Records sync to the cloud when one is available, and sync state is
tracked per row so nothing is silently lost.

**AI advisory.** A retrieval-augmented chatbot answers agricultural questions from
a curated knowledge base, reachable directly or from a diagnosis with the question
pre-written.

**Weather and mandi rates.** Seven-day forecasts from the device's own location,
plus market rates per crop.

---

## Architecture

```
┌─────────────────┐      HTTPS       ┌──────────────────┐
│   Android app   │ ───────────────► │  FastAPI backend │
│                 │   bearer token   │    (Vercel)      │
│  Room (local)   │                  └────────┬─────────┘
│  TFLite models  │                           │
└─────────────────┘                  ┌────────┴─────────┐
                                     │                  │
                              ┌──────▼──────┐   ┌───────▼──────┐
                              │  Supabase   │   │ Google Gemini│
                              │ Auth · DB   │   │  (chat only) │
                              │  Storage    │   └──────────────┘
                              └─────────────┘
```

**The Android client never talks to Supabase or Gemini directly.** Every cloud
call goes through the backend. This is deliberate: routing through a server keeps
all credentials off the device, where an APK can be decompiled in seconds.

| Layer | Technology |
|---|---|
| Android | Kotlin 2.2, Jetpack Compose (BOM 2024.09), MVVM |
| Local storage | Room 2.7 |
| Networking | OkHttp, Coil for images |
| On-device ML | TensorFlow Lite |
| Backend | FastAPI 0.116, Python, serverless on Vercel |
| Auth and database | Supabase (PostgreSQL, GoTrue, Storage) |
| Conversational AI | Google Gemini, server-side only |

Minimum SDK 24 (Android 7.0), target SDK 36.

### Request lifecycle

A ledger entry illustrates the pattern:

1. The entry is written to Room immediately — the UI never waits on the network.
2. The client sends it to the backend with the Supabase access token.
3. The backend validates the token and derives the user ID from it, ignoring any
   ID supplied by the client.
4. The row is upserted in Supabase against a deterministic UUID, so retries never
   create duplicates.
5. On success the local row is marked synced.

If step 2 fails, the row stays local and unsynced. A retry pass runs at app start
and after each login.

### Token refresh

Supabase access tokens expire after roughly an hour. Every authenticated request
goes through a client that catches a `401`, rotates the token once, and replays
the request. A mutex serialises refreshes so concurrent failures do not each
consume a refresh token.

---

## Security model

1. **No AI or database credentials ship in the APK.** The app holds only the
   backend URL.
2. **The backend derives user identity from the validated JWT**, never from the
   request body. A client cannot write rows on another user's behalf.
3. **Mutations are scoped by owner.** Updates and deletes filter on both row ID
   and user ID, so a guessed identifier cannot reach another user's data.
4. **Disease images live in private per-user storage**, served through short-lived
   signed URLs after an ownership check.
5. **The service-role key is backend-only** and never leaves the server.
6. **Passwords are hashed locally** with a per-user salt for the offline path.
   Cloud authentication is handled entirely by Supabase.

---

## Repository layout

```
app/                     Android client
  src/main/java/com/example/
    data/
      local/             Room entities, DAOs, migrations
      model/             TFLite classifiers, plant pre-filter, domain models
      remote/            Backend client, session store, token refresh
      repository/        Business logic per feature area
    ui/
      screens/           auth, dashboard, disease, khata, mandi, weather,
                         chat, guide, settings, splash
      components/        Shared composables
      theme/             Colour schemes and typography
      viewmodel/         MainViewModel
  src/main/assets/model/ Bundled .tflite models and label files

backend/
  app/
    routers/             auth, sync, chat, data
    services/            Supabase REST client, Gemini client, retrieval
  knowledge/             Curated agriculture knowledge base
  scripts/               One-off ingestion utilities

supabase/schema.sql      Tables, RLS policies, triggers, storage buckets
assets/model/            Source Keras model and exported TFLite
```

---

## Setup

### Prerequisites

Android Studio (Ladybug or newer), JDK 17+, Python 3.11+, and accounts on
Supabase, Vercel and Google AI Studio.

### 1. Database

Create a Supabase project, then run `supabase/schema.sql` in the SQL editor. This
creates every table, RLS policy, trigger and the private `disease-images` bucket
in one pass.

Under **Authentication → Sign In / Providers**:

- Enable the **Email** provider and turn **Confirm email off**. With confirmation
  on, signup returns no session and the client never receives a token.
- Enable **anonymous sign-ins** if you want guest mode.
- Leave **Phone** disabled unless you have an SMS provider configured.

### 2. Backend

```bash
cd backend
python -m venv venv && source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
```

Fill in `.env`:

```
SUPABASE_URL=https://<project>.supabase.co
SUPABASE_ANON_KEY=<anon key>
SUPABASE_SERVICE_ROLE_KEY=<service role key>
GEMINI_API_KEY=<gemini key>
GEMINI_MODEL=gemini-2.5-flash
GEMINI_TIMEOUT_SECONDS=45
```

Run it and confirm the health check:

```bash
uvicorn app.main:app --reload
curl http://127.0.0.1:8000/health
```

A healthy response reports `supabase_configured` and `gemini_chatbot_configured`
as `true`.

Seed the chatbot's knowledge base once:

```bash
python -m scripts.ingest_knowledge
```

> Run this only once. Re-running conflicts on the unique `slug` constraint; clear
> `agriculture_documents` first if you need to re-ingest.

### 3. Deployment

Import the repository on Vercel and **set the root directory to `backend`** —
otherwise the build cannot find `requirements.txt`. Add the same environment
variables as above for all three environments.

Keep `GEMINI_TIMEOUT_SECONDS` below the platform's function timeout, or slow chat
responses return a gateway error instead of an answer.

Environment variable changes do not trigger a redeploy automatically.

### 4. Android

Point the app at the deployed backend in `app/.env`:

```
backendBaseUrl=https://<project>.vercel.app
```

This file must contain nothing else. Any key placed here is compiled into
`BuildConfig` and ships inside the APK, and an empty value produces invalid
generated Java.

```bash
./gradlew assembleDebug
```

The build log prints the resolved backend URL. If it warns instead, the URL was
not picked up — pass it explicitly:

```bash
./gradlew assembleRelease -PbackendBaseUrl=https://<project>.vercel.app
```

---

## API reference

All endpoints except `/health` require `Authorization: Bearer <token>`.

### Authentication — `/api/auth`

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/signup` | Register with email or phone |
| `POST` | `/login` | Exchange credentials for a session |
| `POST` | `/refresh` | Rotate an expiring access token |
| `POST` | `/guest` | Anonymous session |
| `POST` | `/forgot-password` | Send a recovery email |
| `GET` | `/me` | Validate the current session |
| `POST` | `/logout` | Revoke the session server-side |

### Synchronisation — `/api/sync`

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/bootstrap` | Restore profile, crops, ledger and history |
| `POST` | `/profile` | Upsert the farmer profile |
| `POST` | `/khata` | Upsert a ledger entry |
| `POST` | `/disease` | Upsert a diagnosis |
| `DELETE` | `/khata/{local_id}` | Remove a ledger entry |
| `DELETE` | `/disease/{local_id}` | Remove a diagnosis and its image |

### Data — `/api`

| Method | Path | Purpose |
|---|---|---|
| `GET` `POST` | `/crops` | List or create crops |
| `PUT` `DELETE` | `/crops/{id}` | Update or delete a crop |
| `POST` | `/disease-images/upload` | Upload to private storage |
| `GET` | `/disease-images/signed-url` | Time-limited access URL |
| `GET` | `/mandi/rates` | Market rates |
| `POST` | `/chat` | Ask the advisory chatbot |

---

## Data model

### Supabase

| Table | Contents |
|---|---|
| `profiles` | Farmer details, created by trigger on signup |
| `user_crops` | Crop records with sowing and harvest dates |
| `khata_transactions` | Income and expense ledger |
| `disease_detections` | Diagnosis history with storage paths |
| `chat_messages` | Conversation history |
| `agriculture_documents` | Knowledge base for retrieval |
| `mandi_rates` | Market prices |

Row-level security is enabled on every user-scoped table.

### Room

Three entities — `user_profiles`, `khata_entries`, `disease_scans` — currently at
schema version 6. Migrations are declared explicitly; add one for any schema change
rather than relying on the destructive fallback.

---

## Machine learning models

| Model | Input | Output | Classes |
|---|---|---|---|
| `plant_disease.tflite` | 1×224×224×3 float32 | 1×3 softmax | Healthy, Septoria, Stripe rust |
| `corn_disease.tflite` | 1×224×224×3 float32 | 1×5 softmax | Blight, Common rust, Gray leaf spot, Healthy, Insect damage |

Both use a MobileNetV2 backbone with preprocessing (`x / 127.5 − 1`) compiled into
the graph, so the classifier feeds raw `[0, 255]` pixel values without normalising
first.

Three safeguards sit between a photograph and a diagnosis:

1. A colour and texture pre-filter rejects images that do not look like foliage.
2. Predictions below a confidence floor are reported as uncertain.
3. A margin check between the top two classes suppresses ambiguous results.

The first matters most. A classifier only knows its own trained classes — shown
anything else, it will still return one of them with a plausible-looking score.
Without the pre-filter, an unrelated photograph produces a confident and entirely
wrong diagnosis.

---

## Testing

```bash
./gradlew test                  # unit and Robolectric tests
./gradlew connectedAndroidTest  # instrumented tests
```

`PlantDiseaseModelTest` covers classifier behaviour including the rejection paths.

---

## Known limitations

These are documented rather than hidden, and each is a reasonable next step.

**Mandi rates are seed data.** Prices come from a seeded table, not a live feed.
Integrate a verified provider before presenting them as current market rates.

**RLS is not the enforcing layer in practice.** The backend uses the service-role
key, which bypasses row-level security. Isolation currently depends on the backend
filtering by user ID on every query — which it does — with RLS as defence in
depth. Switching user-scoped reads to the anon key with the caller's JWT would make
the database itself enforce the boundary.

**Profile photos are local only.** They are stored in app-private storage and lost
on uninstall, unlike other data which restores from the cloud.

**Dark mode is opt-in.** Screens paint hardcoded light surfaces, so following the
system theme would leave text unreadable. A dark scheme is available as an explicit
choice in settings; full support requires moving those surfaces onto the Material
colour scheme.

**Notification preferences are stored but not delivered.** No push infrastructure
is wired up yet.

**Diagnoses are advisory.** The models are trained on a limited set of diseases for
two crops. Serious or spreading damage needs a plant pathologist, and the app
provides a route to one from every result.

---

## License

Add a license before distributing this project.
