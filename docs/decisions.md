# Decisions

Written when the decision is made: what was decided, why, and what was given up.

## 2026-10-08 Room is the source of truth, Firestore is only the remote

The UI reads only from Room. Firestore's own offline cache is not used, so the sync, the queue of offline writes and the conflict rules are written in this project. Given up: less code.

## 2026-10-08 Conflicts: the whole item is compared, the one updated later wins

When two devices changed the same item while they could not see each other, the copy with the later `updatedAt` replaces the other one completely. Simple to reason about and to test. Given up: if one device renamed an item and the other ticked it, the earlier change is lost. Merging field by field is left for later.

## 2026-10-08 Edit against delete: the later one wins, and the user is told

A delete is not treated differently from an edit. A deleted item stays in the list as a tombstone (`deleted = true`) so the delete can be sent to the other devices and compared by time. When an edit beats a delete the item comes back with a notice. Given up: a delete can be undone by an edit made earlier on another device.

## Open

- Same `updatedAt`, different content: "the local copy wins" gives different answers on two devices, which then never agree. The rule has to be the same everywhere (test 2 and test 8 will show it).
- One user on several devices, or a list shared by several people? Not decided; the merge is the same in both cases.
