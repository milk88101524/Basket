# Test list

The behaviours the merge has to have, written before the code. One at a time: pick one, write the failing test, make it pass, tidy up. New cases are added here when they are found.

## Merging two copies of the shopping list

- [x] 1. Same item on both sides: the one updated later wins (`3e1271d`, `8e6786a`)
- [x] 2. Same item, same `updatedAt`, different content: a rule that gives the same answer on every device (see decisions)
  - [x] 2a. Only `deleted` differs: the one that is not deleted is kept (`582c543`, `7424fb2`)
  - [x] 2b. Only `checked` differs: the one that is not checked is kept (`d83d2e1`, `c8ce6ab`)
  - [x] 2c. `deleted` and `checked` point to different sides: `deleted` decides (`2694462`, `db2bdf4`)
  - [x] 2d. Only the name differs: the same one is kept whichever side is given first (`8e4ae22`, `4e479d9`)
  - [x] 2e. Both deleted, only `checked` differs: the one that is not checked is kept (`2ea07b8`, `661da46`, `62d43c3`)
- [x] 3. An item only the local side has is kept (`5443e023`, `e18751cb`)
- [x] 3b. The same item in both lists: merged with the rule of test 1 (`2e007d7`, `ad44b5f`, `be62b52`, `c9fb51a`)
- [x] 3c. The same id twice in the local list: only the later one is kept (`f267a2c`, `2eb8dc4`)
- [x] 4. An item only the remote side has is added (`5e093cf`,`c5d29cc`)
- [x] 5. One side edited, the other deleted: the later one wins, and the user is told
  - [x] 5a. The later one wins, whichever side it is on (the rule is already there; this pins it) (`ab60a99`)
  - [x] 5b. The user is told: `changedByRemote(local, remote)` returns the merged items that differ from the local copy (see decisions). Not done: telling the user that their own edit was overwritten, it needs a field that says what was edited here
    - [x] 5b-1. local 輸了且內容不同: 通知被修改了;一份清單中有多個項目但只有一個被修改時,只回傳被修改的,其餘不回傳 (`5d05917`, `2189431`)
    - [x] 5b-2. local 贏了: 不通知(代表本機是最新的,要做上傳才對) (`c366589`)
    - [x] 5b-3. 兩邊一樣: 不通知(兩邊一樣代表剛做過 sync 或是雙方根本沒動) (`199e6e5`)
    - [x] 5b-4. 只有 local 有: 不通知(同 local 贏了的處理) (`94ba4c9`)
    - [x] 5b-5. 只有 remote 有: 不通知(local 沒有這個項目,是新增,沒有東西被改掉) (`3ae089b`, `0f085f9`)
    - [x] 5b-6. remote 把項目刪了: 通知被刪除了,回傳的項目 `deleted = true` (`8bc02b9`)
- [x] 6. Deleted on both sides: stays deleted (`ce5346c`)
- [x] 7. One side renamed, the other ticked the same item: the whole item is compared, so the earlier change is lost (documents the known limit of the rule) (`776438e`)
- [x] 8. `merge(a, b) == merge(b, a)` and `merge(a, a) == a` (`f23b25b`, `b07ce79`). The sample items all have the same `updatedAt`, so only the tie rule is covered; add other `updatedAt` values later

## Upload: which items go to the remote

`toUpload(local, remote)` returns the merged items that differ from the remote copy: those are the ones the remote has to be told about. It is the mirror of `changedByRemote` (merged ≠ local, so the user is told). Sync (the remote is newer, the local copy is replaced) is not upload.

- [x] 1. An item only the local side has is uploaded (added while offline) (`1e9b0f5`, `b6c42a2`)
- [x] 2. The same item on both sides, identical: not uploaded (the remote is already up to date; a write costs money and wakes the other devices) (`53d559e`, `f9802b6`)
- [x] 3. Both sides have the item and the remote is newer: not uploaded (that is sync) (`fe1874c`; green from the start, shown to be able to fail by a mutation that compares the local list with the remote)
- [x] 4. Both sides have the item and the local copy is newer: uploaded (`6394c67`; green from the start, shown to be able to fail by comparing ids only)
- [x] 5. An item only the remote side has: not uploaded (`c0e536c`; green from the start, shown to be able to fail by also uploading ids the local list does not have)
- [x] 6. Several items in the list, only some of them need uploading: only those are returned (`3cb3ea8`; green from the start, shown to be able to fail by returning only the first item. The order of the result is not a rule, so the test compares sets)
- [x] 7. The local copy is deleted (a tombstone) and newer than the remote copy: uploaded (a delete is treated like an edit) (`19301db`; green once the local and remote sides were the right way round, shown to be able to fail by skipping deleted items)

## Repository: sync

Sync reads the local and the remote list, works out `toUpload` and `changedByRemote` from those two copies, then saves the merged list locally, uploads, and hands back the items to tell the user about. The notification list is worked out before the local save: after the save the local list equals the merged one and nothing would differ. There is no separate queue of offline writes: what still has to be uploaded is whatever differs from the remote, so a failed upload is found again by the next `toUpload`. Tested with a fake local store and a fake remote store.

- [x] 1. The remote has a newer `milk 2L` and a new `egg`, the local has the older `milk`: the local list becomes `milk 2L` and `egg`, nothing is uploaded, and `milk 2L` is reported as changed (`egg` is new, not reported) (`48db04a`, `5b1b1c6`; the "nothing is uploaded" check was green from the start, shown to be able to fail by always calling `write`, which records `[[]]`)
  - [x] 1b. The local list has a `bread` added offline, the remote has `milk` only: the remote is sent `bread` in one `write`, the local list keeps `milk` and `bread`, nothing is reported (found because test 1 left `toUpload` unused in `sync()`) (`27aa9d4`, `87691fb`)
- [x] 2. The upload fails (offline): the merged list is still saved locally, and the exception reaches the caller (WorkManager needs to know the sync failed to retry it; swallowing it would hide why) (`7fb2b42`; green from the start, shown to be able to fail by saving after the upload for the local check and by catching the exception for the other. The first run used a local list equal to the merged one, so the mutation stayed green: the data was changed so that saved and not saved differ)
  - [x] 2b. The upload fails while the remote also changed an item: the exception the caller gets (`UploadFailed`) carries the items to tell the user about (found while deciding 2: the local list is saved before the upload, so without this the next `changedByRemote` finds nothing and the notification is lost; telling the user before the upload is left for when the ViewModel exists) (`d56884c`, `4f27954`; the test of 2 now checks the `cause`, since the exception is wrapped)

More cases of `sync()`, added 2026-11-03. Not listed on purpose: nothing differs on either side (`upload.isNotEmpty()` is already covered by test 1, it would be green from the start) and `write` throwing something other than `IOException` (it only pins what the code does today).

- [x] 3. The first upload fails, the next `sync()` with the same data succeeds: the remote receives `bread` then (proves that no queue of offline writes is needed, see decisions) (`5d12597`; green from the start, shown to be able to fail by a mutation that stops uploading after the first failure: the receipts stay empty)
- [x] 4. The remote cannot be read (offline): the local list is left as it is, `write` is not called, the exception reaches the caller. The `IOException` is passed on as it is (see decisions) (`1abe3de`; green from the start, each of the three checks shown to be able to fail by a mutation of the `remote.read()` line: catching the error and going on with an empty remote list, writing the local list before rethrowing, emptying the local list before rethrowing)
- [x] 5. One `sync()` with both directions: the local list has a `bread` added offline and the remote has a newer `milk 2L`. `bread` is uploaded, `milk 2L` is reported, the local list keeps both (`232348c`; green from the start, shown to be able to fail by uploading the whole merged list instead of `toUpload`: the receipts then hold `milk 2L` as well)
- [x] 6. The local list cannot be saved (a problem of the local database, not of the network: with no network `remote.read()` would have failed first): the sync stops, nothing is uploaded, the exception reaches the caller (`0e534d8`; green from the start, shown to be able to fail by swallowing the exception and by uploading inside the `catch` before rethrowing. A check that the local list is unchanged was dropped: the fake `save` throws before it changes anything, so no change in `sync()` could make that check fail)

## Repository: getItems

The screen shows the local list first and syncs in the background (see decisions, 2026-11-05), so the Repository needs a read that touches only the local store. `sync()` cannot serve it: it returns `changed` (what to tell the user), not the merged list. Written down after the first test, not before; the list should come first from the next one on.

- [x] 1. The local list has `milk`, the remote has `egg`: `getItems()` returns `milk` only (the remote is ignored, and so is any merge of the two sides) (red `62784a5`, green `cb33f2c`; shown to be able to fail by two mutations of the `getItems()` line: reading `remote.read()` instead gives `[egg]`, and `remote.read() + local.read()` gives `[egg, milk]`. That is why the remote holds an item the local list does not: with an empty remote the second mutation would have stayed green)

## ViewModel

`ShoppingViewModel(repo, scope)` is a plain Kotlin class in the BLoC shape: an event goes in with `add(event)`, the state comes out as `state`. The state is the list plus a sync status (`ShoppingState(items, syncState, changed)`), because the list must show while a sync is running or has failed. Started with the data flow, then the details. The tests look only at the field they are about (`state.items` or `state.syncState`), so a change of the other field does not break them.

- [x] 1a. After `Started` the list is the local one (red `0455082`, green `bfede5f`; the remote holds the same item, so the test does not depend on the sync)
- [x] 1b. After `Started` the sync runs and the list is read again: local `milk`, remote `egg`, the list becomes `milk` and `egg` (red `9f578ca`, green `a42a96c`)
  - The state first was `Initial / Loading / Loaded / Error`. It could not show the list and a failed sync together, so it became the list plus a sync status (`dfb1111`, `e4c9a09`, `945bf46` fixes the second test that `e4c9a09` left on `Loaded`). See decisions, 2026-11-06.
- [x] 3. The sync fails (the remote cannot be read): the list stays the local one and the status is `Failed` (red `945bf46`, green `89b42b1`; red from the first run: the placeholder `catch` emptied the list. The first version of the test asserted `assertNotNull(syncState)`, which cannot fail because the type is not nullable, so it was replaced by an `is Failed` check)
- [x] 2. The sync ends: the status is `Done` (red `c2d9a01`, green `3822384`)
- [ ] 3b. The upload fails (`UploadFailed`: the merged list was already saved locally, see decisions 2026-11-02): the list shown is the merged one, not the one read before the sync, and the status is `Failed`. Found while reading test 3: the `catch` keeps the list read before the sync, so after an upload failure the screen would show the old list while the local one is already new. Only the read failure (test 3) is covered today
- [ ] 3c. The first read of the local list fails: what the state says is still to be decided (nothing was shown yet, so there is no list to keep)
- [x] 2b. The items the sync reports as changed (`sync()` returns them) reach the user: after `Started` the state's `changed` holds them (red `5f2fe89`, green `f33501d`; a field of the state, not a separate output, see decisions, 2026-11-06. Clearing it once the user has seen it (a `NoticeDismissed` event) is left for when a test needs it)
- [x] `Syncing` while the sync runs: the status is `Syncing` from the moment the local list is shown until the sync ends (red `4408fb8`, green `6c5850a`; the test holds the sync in the middle with a `readDelay` on the fake remote and `runCurrent()`, the delay is 0 by default so the other tests do not wait. The local read has no delay in this test: with one the test would stop at the first read, before the sync starts)
- [ ] Later: whether and when to tell the user before the upload (left open in decisions, 2026-11-02), `Failed` after an `UploadFailed` also carrying its `changed`, a second sync request while one is running, retry after a failure
- [ ] Later: replace the one-off read by a Flow of the local list (`LocalDataSource.observe()`), so every writer (sync and the user's own edits) refreshes the screen without a manual re-read

## Later (not the merge)

- [ ] Room DAO and migrations (in-memory database)
