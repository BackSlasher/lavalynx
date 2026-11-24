# Lavalynx instructions

This checkout is Lavalynx, a fork of Firefox for Android; see `lavalynx/README.md`
for what is changed and how it is built. These rules apply on top of the upstream
`AGENTS.md`.

- Before rebasing `main` onto upstream, push its current tip to a dated backup
  branch, `backup/main-<YYYY-MM-DD>`, and after the rebase add a row to the
  "Keeping up with upstream" table in `lavalynx/README.md`. Never rebase without
  the backup.
- Mark every change to upstream code with a `// lavalynx:` comment (or
  `<!-- lavalynx: -->` in XML), and keep lavalynx-only files under `lavalynx/`.
