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

-- Storage bucket and object policies (run via Supabase dashboard SQL editor)
-- The storage.buckets table is managed by Supabase; create the bucket via the dashboard
-- or with `supabase storage bucket create shared-clips --public`.
--
-- After creating the bucket, run these policies in the SQL editor:
--
-- create policy "public_read_objects" on storage.objects
--   for select to anon using (bucket_id = 'shared-clips');
--
-- create policy "anon_insert_objects" on storage.objects
--   for insert to anon with check (bucket_id = 'shared-clips');
--
-- create policy "anon_delete_objects" on storage.objects
--   for delete to anon using (bucket_id = 'shared-clips');
