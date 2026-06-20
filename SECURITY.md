# Security Notes for DriveVault Contributors

## Secrets

DriveVault requires several API keys and tokens that must **never** be committed to Git:

- `local.properties` (already in `.gitignore`)
- GitHub personal access token (`github.api.token`)
- Firebase project/app credentials
- Supabase URL and anon key

A template is provided at `local.properties.example`. Copy it to `local.properties` and fill in your own values.

## If You Think a Secret Was Committed

1. **Rotate the exposed credential immediately** in the relevant service dashboard.
2. Remove it from Git history with a tool such as [BFG Repo-Cleaner](https://rtyley.github.io/bfg-repo-cleaner/) or `git filter-repo`.
3. Force-push the cleaned history only if you are sure no collaborators have unpushed work.
4. Treat the secret as compromised even if the commit was reverted.

## Supabase Anon Key in BuildConfig

The Supabase anon key is currently compiled into `BuildConfig` and is therefore extractable from the APK. This is acceptable for the anonymous clip-sharing flow because:

- Row-Level Security (RLS) is enabled on the `shared_clips` table.
- Clips expire after 24 hours and are cleaned up by `ShareCleanupWorker`.
- The key cannot be used to read arbitrary user data.

For stronger protection, consider moving to a server-signed upload URL flow in the future.

## Reporting Security Issues

If you discover a vulnerability in DriveVault, please open a private GitHub issue or email the maintainers directly. Do not disclose security issues publicly until they are resolved.
