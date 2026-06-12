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

-- Storage bucket (run via dashboard or Storage API)
-- Bucket name: shared-clips, public: true
-- Policies on storage.objects:
--   SELECT: allow for anon (public reads)
--   INSERT: allow for anon
--   DELETE: allow for anon (cleanup worker)
insert into storage.buckets (id, name, public)
values ('shared-clips', 'shared-clips', true)
on conflict (id) do nothing;

create policy "public_read_objects" on storage.objects
  for select to anon using (bucket_id = 'shared-clips');

create policy "anon_insert_objects" on storage.objects
  for insert to anon with check (bucket_id = 'shared-clips');

create policy "anon_delete_objects" on storage.objects
  for delete to anon using (bucket_id = 'shared-clips');
