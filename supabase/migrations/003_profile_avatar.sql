-- ==============================================================================
-- FarmifyAI - Profile photos in the cloud
-- ==============================================================================
-- Safe to re-run.
--
-- Profile photos were stored only in the app's private files directory, so they
-- were lost on reinstall and did not follow a farmer to a new phone, unlike
-- every other record. This puts the image in Supabase Storage and the path on
-- the profile row.
-- ==============================================================================

alter table public.profiles
  add column if not exists avatar_path text;

-- Private, like the disease images. Photos are served through short-lived
-- signed URLs after the backend has confirmed ownership.
insert into storage.buckets (id, name, public)
values ('avatars', 'avatars', false)
on conflict (id) do update set public = false;
