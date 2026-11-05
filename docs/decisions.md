# Decisions

Written when the decision is made: what was decided, why, and what was given up.

## 2026-10-08 Room is the source of truth, Firestore is only the remote

The UI reads only from Room. Firestore's own offline cache is not used, so the sync, the queue of offline writes and the conflict rules are written in this project. Given up: less code. (The queue of offline writes was dropped on 2026-11-02, see below.)

## 2026-10-08 Conflicts: the whole item is compared, the one updated later wins

When two devices changed the same item while they could not see each other, the copy with the later `updatedAt` replaces the other one completely. Simple to reason about and to test. Given up: if one device renamed an item and the other ticked it, the earlier change is lost. Merging field by field is left for later.

## 2026-10-08 Edit against delete: the later one wins, and the user is told

A delete is not treated differently from an edit. A deleted item stays in the list as a tombstone (`deleted = true`) so the delete can be sent to the other devices and compared by time. When an edit beats a delete the item comes back with a notice. Given up: a delete can be undone by an edit made earlier on another device.

## 2026-10-08 Same updatedAt: compare deleted, then checked, then name

"The local copy wins" gives different answers on two devices, which then never agree, because each device calls its own copy the local one. The answer must not depend on which side is local. When `updatedAt` is equal, the fields are compared in this order and the first difference decides: `deleted = false` is kept, then `checked = false`, then the name that sorts first. Why: a wrongly kept item can be deleted again, a wrongly deleted one cannot come back; an item not yet bought is worse to forget than to buy twice. Given up: the name rule has no meaning, it only has to be the same everywhere.

## 2026-10-12 通知: 用 changedByRemote, 不放進 merge
(1) 決定了什麼: 合併後,如果某個項目跟這台裝置原本的不一樣(被其他裝置的版本取代),就通知這台的使用者。用 changedByRemote(local, remote) 找出這些項目
(2) 為什麼: merge 是對稱的,看不出哪邊是 local;通知需要知道哪邊是 local,所以放在收兩份清單的函式裡
(3) 放棄了什麼: 只知道這項被別的裝置改了,不知道使用者自己是否也改過、自己的編輯被覆蓋了什麼。要做到要加欄位

## 2026-11-02 No separate queue of offline writes: what has to be uploaded is what differs from the remote

Writes made offline are only saved in Room. At every sync `toUpload(local, remote)` finds the items that differ from the remote, so an upload that failed is found again by the next sync. Why: a queue is a second copy of the same information and has to be kept in step with Room. It works for edits and deletes because a delete stays in the list as a tombstone. Given up: knowing which write failed or how often it was tried, and the order the writes were made in.

## 2026-11-02 A failed upload: the local list stays saved, the exception goes up and carries the items to report

`sync()` saves the merged list locally first, then uploads. If the upload fails the local list stays as it is (the screen reads Room and must show the newest list), and `sync()` throws `UploadFailed`, with the original error as its cause and the items to tell the user about in `changed`. Why: swallowing the error hides it from the caller (WorkManager must know the sync failed to retry it); without `changed`, the next sync finds nothing to report because the local list is already new. Given up: telling the user before the upload; to be decided again when the ViewModel exists.

## 2026-11-03 Offline when reading the remote: the IOException is passed on as it is

If `remote.read()` fails, `sync()` stops there: nothing is merged, nothing is saved, nothing is uploaded, and the `IOException` reaches the caller unchanged. Why: the local list has not changed, so there is nothing to report and nothing to lose; `UploadFailed` exists only to carry `changed`, and here there is nothing to carry. Given up: the caller cannot tell a failed read from a failed upload by the type alone (`UploadFailed` means the local list was already saved). Decide again if the ViewModel needs to tell them apart.

## 2026-11-03 The local list cannot be saved: the sync stops, nothing is uploaded

If `local.save` fails, the exception goes up as it is and the merged list is not uploaded. Why: it can only be a problem of the local database (with no network, `remote.read()` fails first); the local list is unchanged, so the next `sync()` works out the same `changedByRemote` and `toUpload` again and nothing is lost; uploading would not repair the local database and needs more code. Given up: the other devices get the local changes only after the local database works again.

## Open

- One user on several devices, or a list shared by several people? Not decided; the merge is the same in both cases.
- Order of the merged list: now the items of the local list come first, then the ones only the remote has. No test depends on it. Decide when a screen needs a fixed order, and pin it with a test then.

## 2026-11-05 Show the local list first and sync in the background; the Repository reads the local list with `getItems()`

On start the screen shows what is in Room and a sync runs in the background; the list is read again when the sync ends. Why: the app is offline-first, so the list must show with no network and must not wait for one. `sync()` cannot be used for the read because it returns `changed`, not the merged list. The name stays `getItems()`, not `getLocalItems()`: that the screen reads only the local store is a rule of the app (see 2026-10-08), not something the caller should have to know, and the name would be wrong if the source changed. Given up: the name does not tell a reader that no network is used (this note does). Expected to change: when the Room DAO exists the read becomes a Flow of the local list, so that every writer (sync and the user's own edits) refreshes the screen without a manual re-read.

