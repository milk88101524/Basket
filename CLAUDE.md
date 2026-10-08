# Basket

A shared shopping list that works offline. It is a practice project: test-first development, Room, offline-first design, then CI and Firebase (Crashlytics).

## How this project is developed

- **Test first, and Han writes the code.** Red, green, refactor, one commit each. Claude explains and reviews; it does not generate a whole feature or a pile of tests at once. A test is only trusted once it has been seen failing.
- **Decisions are written down when they are made**, 3 to 5 lines each in `docs/decisions.md`: what was decided, why, and what was given up.
- Claude helps with the code, and commits made with its help carry `Co-Authored-By`. That is meant to be visible.

## Design decisions already made

- Room is the single source of truth; the UI only reads from it.
- Firestore is only the remote. Its own offline cache is not used, so the sync is written here.
- Conflicts: the item updated later wins. A deleted item stays as a tombstone so a delete is not undone by the other device.

## Rules

- Commit format: `<type>: <summary>` (the global default; `test` is a type). No Jira key: this is a personal project.
- The git identity of this repo is the GitHub noreply address. Do not change it, and do not change the global git config.
- Ask before every push. Never commit keys or config files with secrets (for example `google-services.json`): ask first.
- Do not put AI conversation content in the repo.
