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
- [ ] 6. Several items in the list, only some of them need uploading: only those are returned
- [ ] 7. The local copy is deleted (a tombstone) and newer than the remote copy: uploaded (a delete is treated like an edit)

## Later (not the merge)

- [ ] Repository: read from Room first, queue writes made offline, send them when online (with fakes for local and remote)
- [ ] Room DAO and migrations (in-memory database)
- [ ] ViewModel state
