-- Follow-up to 20260612000000_shared_clips.sql.
--
-- The original migration created the shared_clips table and RLS policies but
-- left the storage bucket + object policies as comments to be applied via the
-- Supabase dashboard. The originally-suggested storage policy set included an
-- `anon_delete_objects` policy that let ANY anonymous client delete ANY clip
-- from the public bucket before its 24-hour expiry — a privacy/security bug
-- since the bucket is public-readable and therefore enumerable.
--
-- This migration:
--   1. Ensures the `shared-clips` storage bucket exists and is public.
--   2. Drops the unsafe `anon_delete_objects` policy if it was applied.
--   3. Idempotently creates safe storage object policies:
--        - anon can SELECT (public viewer downloads the clip)
--        - anon can INSERT (app uploads new shares)
--        - anon CANNOT delete. Storage deletes are performed only by the
--          ShareCleanupWorker, which authenticates with the service role key
--          and bypasses RLS entirely.
--
-- NOTE: We do NOT attempt `ALTER TABLE storage.buckets ENABLE ROW LEVEL
-- SECURITY` here — the Supabase CLI login role is not the owner of
-- storage.buckets (only supabase_admin is). Bucket-level RLS is not needed for
-- this security model; the object-level policies below are sufficient.

-- 1. Ensure bucket exists + is public. Safe to re-run.
insert into storage.buckets (id, name, public)
select 'shared-clips', 'shared-clips', true
where not exists (
  select 1 from storage.buckets where id = 'shared-clips'
);

-- 2. Drop the unsafe anon-delete policy if it exists from a prior manual apply.
drop policy if exists "anon_delete_objects" on storage.objects;

-- 3. Storage object policies (idempotent via drop+create).
drop policy if exists "public_read_objects" on storage.objects;
create policy "public_read_objects" on storage.objects
  for select to anon
  using (bucket_id = 'shared-clips');

drop policy if exists "anon_insert_objects" on storage.objects;
create policy "anon_insert_objects" on storage.objects
  for insert to anon
  with check (bucket_id = 'shared-clips');

-- No anon delete policy. Service role bypasses RLS and is the only principal
-- that can delete storage objects. See ShareCleanupWorker (uses
-- SUPABASE_SERVICE_ROLE_KEY).