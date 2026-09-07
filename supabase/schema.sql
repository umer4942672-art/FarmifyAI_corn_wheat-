-- ==============================================================================
-- FarmifyAI — Complete Supabase PostgreSQL Database Schema & Setup
-- ==============================================================================
-- Run this complete SQL script in your Supabase project's SQL Editor:
-- Supabase Dashboard -> Select Project -> SQL Editor -> New Query -> Paste & Run
-- ==============================================================================

-- 1. EXTENSIONS
create extension if not exists "uuid-ossp";

-- ==============================================================================
-- 2. FARMER PROFILES TABLE (Linked with Supabase Auth)
-- ==============================================================================
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  name text not null default 'Pakistani Kisan',
  email text,
  phone text,
  language text default 'ur',
  farm_name text default 'Al-Rehman Agri Farms',
  district text default 'Faisalabad',
  province text default 'Punjab',
  farm_location text default 'Faisalabad, Punjab',
  total_acres numeric(10,2) default 10.0,
  primary_crops text default 'Wheat, Cotton, Sugarcane',
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- ==============================================================================
-- 3. SMART KHATA LEDGER TRANSACTIONS (Income & Expenses)
-- ==============================================================================
create table if not exists public.khata_transactions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  type text not null check (type in ('income', 'expense')),
  category text not null,
  crop_name text default 'General',
  amount numeric(12,2) not null check (amount >= 0),
  quantity numeric(10,2),
  unit text default 'Mann',
  field_name text default 'Main Field',
  buyer_or_mandi text,
  description text,
  transaction_date date default current_date,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- ==============================================================================
-- 4. CROPS MANAGEMENT TABLE
-- ==============================================================================
create table if not exists public.crops (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  crop_name text not null,
  variety text,
  area numeric(10,2),
  unit text default 'Acres',
  sowing_date date,
  harvest_date date,
  expected_yield text,
  status text default 'Active',
  notes text,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- ==============================================================================
-- 5. AI DISEASE DIAGNOSIS HISTORY TABLE
-- ==============================================================================
create table if not exists public.disease_detections (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  crop_name text,
  disease_name text,
  disease_name_ur text,
  confidence numeric(5,2),
  severity text,
  image_url text,
  recommendation text,
  treatment_chemical text,
  treatment_organic text,
  treatment_cultural text,
  symptoms text,
  created_at timestamptz default now()
);

-- ==============================================================================
-- 6. MANDI RATES & MARKET INTELLIGENCE TABLE
-- ==============================================================================
create table if not exists public.mandi_rates (
  id text primary key,
  crop_name_en text not null,
  crop_name_ur text not null,
  category text not null,
  mandi_name text not null,
  city text not null,
  province text not null,
  price_per_kg numeric(10,2) not null,
  price_per_mann numeric(10,2) not null,
  min_price_per_kg numeric(10,2),
  max_price_per_kg numeric(10,2),
  trend text default 'UP' check (trend in ('UP', 'DOWN', 'STABLE')),
  change_percent numeric(5,2) default 0.0,
  last_updated text default 'Today, 09:00 AM',
  created_at timestamptz default now()
);

-- ==============================================================================
-- 7. PERFORMANCE INDEXES
-- ==============================================================================
create index if not exists idx_khata_user_id on public.khata_transactions(user_id);
create index if not exists idx_khata_created_at on public.khata_transactions(created_at desc);
create index if not exists idx_khata_type on public.khata_transactions(type);

create index if not exists idx_crops_user_id on public.crops(user_id);
create index if not exists idx_crops_created_at on public.crops(created_at desc);

create index if not exists idx_disease_user_id on public.disease_detections(user_id);
create index if not exists idx_disease_created_at on public.disease_detections(created_at desc);

create index if not exists idx_mandi_city on public.mandi_rates(city);
create index if not exists idx_mandi_crop on public.mandi_rates(crop_name_en);

-- ==============================================================================
-- 8. AUTOMATIC PROFILE CREATION TRIGGER ON AUTH SIGNUP
-- ==============================================================================
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
  insert into public.profiles (
    id,
    name,
    email,
    phone,
    language,
    farm_name,
    district,
    province,
    farm_location,
    total_acres,
    primary_crops,
    created_at,
    updated_at
  ) values (
    new.id,
    coalesce(new.raw_user_meta_data->>'name', split_part(coalesce(new.email, 'kisan'), '@', 1)),
    new.email,
    new.raw_user_meta_data->>'phone',
    coalesce(new.raw_user_meta_data->>'language', 'ur'),
    coalesce(new.raw_user_meta_data->>'farmName', new.raw_user_meta_data->>'farm_name', 'Al-Rehman Agri Farms'),
    coalesce(new.raw_user_meta_data->>'district', 'Faisalabad'),
    coalesce(new.raw_user_meta_data->>'province', 'Punjab'),
    coalesce(new.raw_user_meta_data->>'farmLocation', new.raw_user_meta_data->>'farm_location', 'Chak 124 GB, Faisalabad, Punjab'),
    coalesce((new.raw_user_meta_data->>'totalAcres')::numeric, (new.raw_user_meta_data->>'total_acres')::numeric, 10.0),
    coalesce(new.raw_user_meta_data->>'primaryCrops', new.raw_user_meta_data->>'primary_crops', 'Wheat, Cotton, Sugarcane'),
    now(),
    now()
  )
  on conflict (id) do update
  set
    name = coalesce(excluded.name, profiles.name),
    email = coalesce(excluded.email, profiles.email),
    phone = coalesce(excluded.phone, profiles.phone),
    updated_at = now();

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- ==============================================================================
-- 9. ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================

alter table public.profiles enable row level security;
alter table public.khata_transactions enable row level security;
alter table public.crops enable row level security;
alter table public.disease_detections enable row level security;
alter table public.mandi_rates enable row level security;

-- 9.1 PROFILES POLICIES
drop policy if exists "Users can view own profile" on public.profiles;
create policy "Users can view own profile"
  on public.profiles for select
  using (auth.uid() = id);

drop policy if exists "Users can insert own profile" on public.profiles;
create policy "Users can insert own profile"
  on public.profiles for insert
  with check (auth.uid() = id);

drop policy if exists "Users can update own profile" on public.profiles;
create policy "Users can update own profile"
  on public.profiles for update
  using (auth.uid() = id)
  with check (auth.uid() = id);

-- 9.2 SMART KHATA POLICIES
drop policy if exists "Users can view own khata transactions" on public.khata_transactions;
create policy "Users can view own khata transactions"
  on public.khata_transactions for select
  using (auth.uid() = user_id);

drop policy if exists "Users can insert own khata transactions" on public.khata_transactions;
create policy "Users can insert own khata transactions"
  on public.khata_transactions for insert
  with check (auth.uid() = user_id);

drop policy if exists "Users can update own khata transactions" on public.khata_transactions;
create policy "Users can update own khata transactions"
  on public.khata_transactions for update
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

drop policy if exists "Users can delete own khata transactions" on public.khata_transactions;
create policy "Users can delete own khata transactions"
  on public.khata_transactions for delete
  using (auth.uid() = user_id);

-- 9.3 CROPS POLICIES
drop policy if exists "Users can view own crops" on public.crops;
create policy "Users can view own crops"
  on public.crops for select
  using (auth.uid() = user_id);

drop policy if exists "Users can insert own crops" on public.crops;
create policy "Users can insert own crops"
  on public.crops for insert
  with check (auth.uid() = user_id);

drop policy if exists "Users can update own crops" on public.crops;
create policy "Users can update own crops"
  on public.crops for update
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

drop policy if exists "Users can delete own crops" on public.crops;
create policy "Users can delete own crops"
  on public.crops for delete
  using (auth.uid() = user_id);

-- 9.4 DISEASE DETECTIONS POLICIES
drop policy if exists "Users can view own disease detections" on public.disease_detections;
create policy "Users can view own disease detections"
  on public.disease_detections for select
  using (auth.uid() = user_id);

drop policy if exists "Users can insert own disease detections" on public.disease_detections;
create policy "Users can insert own disease detections"
  on public.disease_detections for insert
  with check (auth.uid() = user_id);

drop policy if exists "Users can delete own disease detections" on public.disease_detections;
create policy "Users can delete own disease detections"
  on public.disease_detections for delete
  using (auth.uid() = user_id);

-- 9.5 MANDI RATES POLICIES (Public read access)
drop policy if exists "Anyone can view live mandi rates" on public.mandi_rates;
create policy "Anyone can view live mandi rates"
  on public.mandi_rates for select
  to public
  using (true);

-- ==============================================================================
-- 10. STORAGE SETUP FOR PLANT DISEASE IMAGES
-- ==============================================================================
insert into storage.buckets (id, name, public)
values ('disease-images', 'disease-images', true)
on conflict (id) do update set public = true;

drop policy if exists "Users can upload disease images to own folder" on storage.objects;
create policy "Users can upload disease images to own folder"
  on storage.objects for insert
  to authenticated
  with check (
    bucket_id = 'disease-images' and
    (storage.foldername(name))[1] = auth.uid()::text
  );

drop policy if exists "Public read access for disease images" on storage.objects;
create policy "Public read access for disease images"
  on storage.objects for select
  to public
  using (bucket_id = 'disease-images');

-- ==============================================================================
-- 11. INITIAL SEED DATA FOR PAKISTAN MANDI RATES
-- ==============================================================================
insert into public.mandi_rates (id, crop_name_en, crop_name_ur, category, mandi_name, city, province, price_per_kg, price_per_mann, min_price_per_kg, max_price_per_kg, trend, change_percent, last_updated)
values
  ('wheat_lahore', 'Wheat (Gandum)', 'گندم', 'Grain', 'Badami Bagh Grain Market', 'Lahore', 'Punjab', 97.50, 3900.00, 95.00, 100.00, 'UP', 2.40, 'Today, 08:30 AM'),
  ('wheat_faisalabad', 'Wheat (Gandum)', 'گندم', 'Grain', 'Ghalla Mandi', 'Faisalabad', 'Punjab', 98.00, 3920.00, 96.00, 101.00, 'UP', 1.80, 'Today, 09:00 AM'),
  ('rice_basmati_gujranwala', 'Super Basmati Rice', 'سپر باسمتی چاول', 'Grain', 'Kamoke Rice Market', 'Gujranwala', 'Punjab', 285.00, 11400.00, 275.00, 295.00, 'UP', 3.20, 'Today, 09:15 AM'),
  ('cotton_multan', 'Cotton / Phutti', 'کپاس / پھٹی', 'Cash Crop', 'Multan Cotton Exchange', 'Multan', 'Punjab', 215.00, 8600.00, 205.00, 225.00, 'DOWN', -1.50, 'Today, 10:00 AM'),
  ('sugarcane_ryk', 'Sugarcane (Ganna)', 'کماد / گنا', 'Cash Crop', 'Rahim Yar Khan Mandi', 'Rahim Yar Khan', 'Punjab', 11.25, 450.00, 10.50, 12.00, 'STABLE', 0.00, 'Today, 08:45 AM'),
  ('maize_sahiwal', 'Maize (Makai)', 'مکئی', 'Grain', 'Grain Market Sahiwal', 'Sahiwal', 'Punjab', 67.50, 2700.00, 65.00, 70.00, 'UP', 1.10, 'Today, 09:30 AM'),
  ('potato_okara', 'Potato (Aloo)', 'آلو', 'Vegetable', 'Okara Sabzi Mandi', 'Okara', 'Punjab', 42.00, 1680.00, 38.00, 46.00, 'DOWN', -3.80, 'Today, 07:30 AM'),
  ('onion_hyderabad', 'Onion (Piyaz)', 'پیاز', 'Vegetable', 'Hyderabad Vegetable Market', 'Hyderabad', 'Sindh', 85.00, 3400.00, 80.00, 92.00, 'UP', 5.00, 'Today, 07:00 AM'),
  ('tomato_peshawar', 'Tomato (Tamatar)', 'ٹماٹر', 'Vegetable', 'Peshawar Fruit & Veg Mandi', 'Peshawar', 'KPK', 95.00, 3800.00, 85.00, 110.00, 'DOWN', -4.20, 'Today, 06:45 AM'),
  ('apple_quetta', 'Apple (Saib)', 'سیب', 'Fruit', 'Quetta Fruit Mandi', 'Quetta', 'Balochistan', 175.00, 7000.00, 160.00, 195.00, 'STABLE', 0.00, 'Today, 08:00 AM')
on conflict (id) do update set
  price_per_kg = excluded.price_per_kg,
  price_per_mann = excluded.price_per_mann,
  trend = excluded.trend,
  change_percent = excluded.change_percent,
  last_updated = excluded.last_updated;

-- ==============================================================================
-- 9. CUSTOM AGRICULTURE CHATBOT: RAG KNOWLEDGE BASE
-- ==============================================================================
-- Supabase stores the verified agriculture knowledge used as baseline context.
-- pgvector remains available for future semantic retrieval/knowledge upgrades.
create extension if not exists vector;

create table if not exists public.agriculture_documents (
  id uuid primary key default gen_random_uuid(),
  slug text unique not null,
  title text not null,
  crop text not null default 'general',
  language text not null default 'en',
  content text not null,
  embedding vector(768),
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create index if not exists idx_agriculture_documents_crop
  on public.agriculture_documents(crop);
create index if not exists idx_agriculture_documents_language
  on public.agriculture_documents(language);
create index if not exists idx_agriculture_documents_embedding
  on public.agriculture_documents using ivfflat (embedding vector_cosine_ops)
  with (lists = 10);

-- Similarity search function used by FastAPI RAG.
create or replace function public.match_agriculture_documents(
  query_embedding vector(768),
  match_threshold float,
  match_count int
)
returns table (
  id uuid,
  slug text,
  title text,
  crop text,
  language text,
  content text,
  similarity float
)
language sql
stable
as $$
  select
    d.id,
    d.slug,
    d.title,
    d.crop,
    d.language,
    d.content,
    1 - (d.embedding <=> query_embedding) as similarity
  from public.agriculture_documents d
  where d.embedding is not null
    and 1 - (d.embedding <=> query_embedding) >= match_threshold
  order by d.embedding <=> query_embedding
  limit least(greatest(match_count, 1), 10);
$$;

-- ==============================================================================
-- 10. CUSTOM CHAT HISTORY
-- ==============================================================================
create table if not exists public.chat_messages (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null check (role in ('user', 'assistant')),
  content text not null,
  language text not null default 'en',
  created_at timestamptz default now()
);

create index if not exists idx_chat_messages_user_created
  on public.chat_messages(user_id, created_at desc);

-- Backend uses the Supabase service role after validating the user's access token.
-- Keep these tables protected from direct anonymous/client writes.
alter table public.agriculture_documents enable row level security;
alter table public.chat_messages enable row level security;

-- ==============================================================================
-- 12. PRIVATE USER DATA HARDENING
-- ==============================================================================
-- Keep disease images private; clients must access only their own folder.
update storage.buckets set public = false where id = 'disease-images';
drop policy if exists "Public read access for disease images" on storage.objects;
drop policy if exists "Users can read own disease images" on storage.objects;
create policy "Users can read own disease images" on storage.objects for select to authenticated
using (bucket_id = 'disease-images' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "Users can delete own disease images" on storage.objects;
create policy "Users can delete own disease images" on storage.objects for delete to authenticated
using (bucket_id = 'disease-images' and (storage.foldername(name))[1] = auth.uid()::text);

-- Explicit user crop fields, separate from generic crop guide content.
create table if not exists public.user_crops (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  crop_name text not null,
  variety text,
  area numeric(10,2),
  area_unit text default 'Acres',
  sowing_date date,
  expected_harvest_date date,
  status text default 'Active',
  notes text,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);
create index if not exists idx_user_crops_user on public.user_crops(user_id, created_at desc);
alter table public.user_crops enable row level security;
drop policy if exists "Users manage own crops" on public.user_crops;
create policy "Users manage own crops" on public.user_crops for all to authenticated
using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- Chat history is private to the owner.
drop policy if exists "Users read own chat history" on public.chat_messages;
create policy "Users read own chat history" on public.chat_messages for select to authenticated using (auth.uid() = user_id);
