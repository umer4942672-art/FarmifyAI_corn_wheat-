-- ==============================================================================
-- FarmifyAI - Messages attached to a work order
-- ==============================================================================
-- Safe to re-run.
--
-- Not a general chat: every message belongs to one job, so the conversation sits
-- beside that job's photos, GPS evidence and money. When the two sides disagree
-- later, the whole exchange is in one place instead of scattered through a
-- messaging app where it can also be deleted.
--
-- System rows record what happened - accepted, submitted, reviewed, paid - so a
-- readable history builds itself without either party writing anything.
-- ==============================================================================

create table if not exists public.work_messages (
  id            uuid primary key default gen_random_uuid(),
  work_order_id uuid not null references public.work_orders(id) on delete cascade,
  -- Null for system entries, which belong to no one.
  sender_id     uuid references public.profiles(id) on delete set null,
  kind          text not null default 'text' check (kind in ('text', 'system')),
  body          text not null check (length(trim(body)) > 0),
  created_at    timestamptz not null default now(),
  -- Read marks are per side, so each party sees its own unread count.
  read_by_landowner_at  timestamptz,
  read_by_contractor_at timestamptz
);

create index if not exists work_messages_order_idx
  on public.work_messages (work_order_id, created_at);

alter table public.work_messages enable row level security;

drop policy if exists "Parties can view messages on their work" on public.work_messages;
create policy "Parties can view messages on their work" on public.work_messages
  for select using (
    exists (
      select 1 from public.work_orders w
      where w.id = work_order_id
        and (auth.uid() = w.landowner_id or auth.uid() = w.contractor_id)
    )
  );
