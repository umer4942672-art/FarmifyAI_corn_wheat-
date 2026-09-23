-- ==============================================================================
-- FarmifyAI - Landowner and Contractor Verification
-- ==============================================================================
-- Run this once in the Supabase SQL editor on an existing project. It is safe to
-- re-run: every statement is guarded with IF NOT EXISTS or DROP ... IF EXISTS.
--
-- What it adds
--   * a role on every profile (landowner or contractor)
--   * contractor_links   - which contractors a landowner works with
--   * work_orders        - the job, its agreed rate and its status
--   * work_proofs        - photo, GPS and time evidence for each job
--   * work_payments      - payments confirmed by both parties
--   * work_order_balances - a view computing what is owed from verified work
--   * a private work-proofs storage bucket
--
-- The backend reaches these tables with the service key and enforces ownership
-- itself, as it does for every other table. The RLS policies below are defence
-- in depth, so a client holding only its own token can never read another
-- party's work.
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. Roles
-- ------------------------------------------------------------------------------
alter table public.profiles
  add column if not exists role text not null default 'landowner';

alter table public.profiles drop constraint if exists profiles_role_check;
alter table public.profiles
  add constraint profiles_role_check check (role in ('landowner', 'contractor'));

-- ------------------------------------------------------------------------------
-- 2. Contractor links
-- ------------------------------------------------------------------------------
create table if not exists public.contractor_links (
  id            uuid primary key default gen_random_uuid(),
  landowner_id  uuid not null references public.profiles(id) on delete cascade,
  contractor_id uuid not null references public.profiles(id) on delete cascade,
  status        text not null default 'pending'
                check (status in ('pending', 'active', 'removed')),
  created_at    timestamptz not null default now(),
  unique (landowner_id, contractor_id),
  check (landowner_id <> contractor_id)
);

create index if not exists contractor_links_contractor_idx
  on public.contractor_links (contractor_id);

-- ------------------------------------------------------------------------------
-- 3. Work orders
-- ------------------------------------------------------------------------------
create table if not exists public.work_orders (
  id              uuid primary key default gen_random_uuid(),
  landowner_id    uuid not null references public.profiles(id) on delete cascade,
  contractor_id   uuid not null references public.profiles(id) on delete cascade,
  task_type       text not null,
  field_name      text not null,
  crop_name       text,
  notes           text,
  area_acres      numeric(8, 2)  not null check (area_acres > 0),
  rate_per_acre   numeric(12, 2) not null check (rate_per_acre >= 0),
  -- Computed by the database so it can never be edited out of step with the
  -- agreed rate. This is what removes the "we agreed a different price" dispute.
  total_amount    numeric(14, 2) generated always as (area_acres * rate_per_acre) stored,
  advance_amount  numeric(12, 2) not null default 0 check (advance_amount >= 0),
  field_lat       double precision,
  field_lng       double precision,
  verified_acres  numeric(8, 2) check (verified_acres >= 0),
  review_note     text,
  status          text not null default 'proposed'
                  check (status in ('proposed', 'accepted', 'submitted', 'approved',
                                    'partial', 'disputed', 'closed', 'cancelled')),
  scheduled_date  date,
  accepted_at     timestamptz,
  submitted_at    timestamptz,
  reviewed_at     timestamptz,
  created_at      timestamptz not null default now(),
  check (landowner_id <> contractor_id),
  check (verified_acres is null or verified_acres <= area_acres)
);

create index if not exists work_orders_landowner_idx  on public.work_orders (landowner_id, created_at desc);
create index if not exists work_orders_contractor_idx on public.work_orders (contractor_id, created_at desc);

-- ------------------------------------------------------------------------------
-- 4. Work proofs
-- ------------------------------------------------------------------------------
create table if not exists public.work_proofs (
  id                    uuid primary key default gen_random_uuid(),
  work_order_id         uuid not null references public.work_orders(id) on delete cascade,
  contractor_id         uuid not null references public.profiles(id) on delete cascade,
  stage                 text not null check (stage in ('before', 'after', 'receipt')),
  image_path            text not null,
  latitude              double precision not null,
  longitude             double precision not null,
  accuracy_m            real,
  -- Two clocks. The device clock can be changed by the user; the server clock
  -- cannot. A large gap between them is shown to the landowner as a warning.
  captured_at           timestamptz not null,
  uploaded_at           timestamptz not null default now(),
  distance_from_field_m real,
  is_mock_location      boolean not null default false
);

create index if not exists work_proofs_order_idx on public.work_proofs (work_order_id);

-- ------------------------------------------------------------------------------
-- 5. Payments
-- ------------------------------------------------------------------------------
create table if not exists public.work_payments (
  id                  uuid primary key default gen_random_uuid(),
  work_order_id       uuid not null references public.work_orders(id) on delete cascade,
  payer_id            uuid not null references public.profiles(id),
  payee_id            uuid not null references public.profiles(id),
  kind                text not null check (kind in ('advance', 'balance', 'expense')),
  amount              numeric(12, 2) not null check (amount > 0),
  method              text not null check (method in ('cash', 'jazzcash', 'easypaisa', 'bank')),
  transaction_ref     text,
  note                text,
  -- A payment counts only once the person receiving it has confirmed it.
  status              text not null default 'pending'
                      check (status in ('pending', 'confirmed', 'rejected')),
  payer_marked_at     timestamptz not null default now(),
  payee_confirmed_at  timestamptz,
  created_at          timestamptz not null default now(),
  check (payer_id <> payee_id)
);

create index if not exists work_payments_order_idx on public.work_payments (work_order_id);

-- ------------------------------------------------------------------------------
-- 6. Balance - always computed from verified acres, never entered by hand
-- ------------------------------------------------------------------------------
create or replace view public.work_order_balances as
select
  w.id            as work_order_id,
  w.landowner_id,
  w.contractor_id,
  w.total_amount  as agreed_amount,
  w.rate_per_acre * coalesce(w.verified_acres, 0) as payable,
  coalesce(sum(p.amount) filter (where p.status = 'confirmed' and p.kind <> 'expense'), 0) as paid,
  coalesce(sum(p.amount) filter (where p.status = 'confirmed' and p.kind = 'expense'), 0)  as expenses_paid,
  w.rate_per_acre * coalesce(w.verified_acres, 0)
    - coalesce(sum(p.amount) filter (where p.status = 'confirmed' and p.kind <> 'expense'), 0) as balance
from public.work_orders w
left join public.work_payments p on p.work_order_id = w.id
group by w.id;

-- ------------------------------------------------------------------------------
-- 7. Link ledger entries to the job that produced them
-- ------------------------------------------------------------------------------
alter table public.khata_transactions
  add column if not exists work_order_id uuid references public.work_orders(id) on delete set null;

-- ------------------------------------------------------------------------------
-- 8. Row level security - defence in depth
-- ------------------------------------------------------------------------------
alter table public.contractor_links enable row level security;
alter table public.work_orders      enable row level security;
alter table public.work_proofs      enable row level security;
alter table public.work_payments    enable row level security;

drop policy if exists "Parties can view their contractor links" on public.contractor_links;
create policy "Parties can view their contractor links" on public.contractor_links
  for select using (auth.uid() = landowner_id or auth.uid() = contractor_id);

drop policy if exists "Parties can view their work orders" on public.work_orders;
create policy "Parties can view their work orders" on public.work_orders
  for select using (auth.uid() = landowner_id or auth.uid() = contractor_id);

drop policy if exists "Parties can view proofs for their work" on public.work_proofs;
create policy "Parties can view proofs for their work" on public.work_proofs
  for select using (
    exists (select 1 from public.work_orders w
            where w.id = work_order_id
              and (auth.uid() = w.landowner_id or auth.uid() = w.contractor_id))
  );

drop policy if exists "Parties can view their payments" on public.work_payments;
create policy "Parties can view their payments" on public.work_payments
  for select using (auth.uid() = payer_id or auth.uid() = payee_id);

-- ------------------------------------------------------------------------------
-- 9. Private storage for work evidence
-- ------------------------------------------------------------------------------
insert into storage.buckets (id, name, public)
values ('work-proofs', 'work-proofs', false)
on conflict (id) do update set public = false;
