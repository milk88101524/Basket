# Decisions

Written when the decision is made: what was decided, why, and what was given up.

## 2026-10-08 Room is the source of truth, Firestore is only the remote

The UI reads only from Room. Firestore's own offline cache is not used, so the sync, the queue of offline writes and the conflict rules are written in this project. Given up: less code.

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

## Open

- One user on several devices, or a list shared by several people? Not decided; the merge is the same in both cases.
- Order of the merged list: now the items of the local list come first, then the ones only the remote has. No test depends on it. Decide when a screen needs a fixed order, and pin it with a test then.