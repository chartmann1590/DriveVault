-- Create table for shared clip metadata
create table if not exists shared_clips (
  id uuid primary key,
  storage_path text not null,
  file_size_bytes bigint,
  upload_time timestamptz default now(),
  expires_at timestamptz not null
);

alter table shared_clips enable row level security;

-- Anyone can read (viewer page needs to check expiry)
create policy "public_read" on shared_clips
  for select to anon using (true);

-- App uploads without requiring a user account
create policy "anon_insert" on shared_clips
  for insert to anon with check (true);

-- Cleanup worker can only delete already-expired records
create policy "anon_delete_expired" on shared_clips
  for delete to anon using (expires_at < now());

-- Storage bucket and object policies are created by the follow-up migration
-- 20260713000000_fix_shared_clips_storage_policies.sql, which also drops the
-- unsafe `anon_delete_objects` policy that was previously suggested here.
--
-- IMPORTANT: storage object deletes are performed ONLY by the ShareCleanupWorker
-- using the Supabase service role key (which bypasses RLS). Anon is never
-- granted delete permission on storage.objects.
