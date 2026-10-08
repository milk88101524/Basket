# Test list

The behaviours the merge has to have, written before the code. One at a time: pick one, write the failing test, make it pass, tidy up. New cases are added here when they are found.

## Merging two copies of the shopping list

- [x] 1. Same item on both sides: the one updated later wins (`3e1271d`, `8e6786a`)
- [ ] 2. Same item, same `updatedAt`, different content: a rule that gives the same answer on every device (see decisions)
- [x] 3. An item only the local side has is kept (`5443e023`, `e18751cb`)
- [x] 3b. The same item in both lists: merged with the rule of test 1 (`2e007d7`, `ad44b5f`, `be62b52`, `c9fb51a`)
- [x] 3c. The same id twice in the local list: only the later one is kept (`f267a2c`, `2eb8dc4`)
- [x] 4. An item only the remote side has is added (`5e093cf`,`c5d29cc`)
- [ ] 5. One side edited, the other deleted: the later one wins, and the user is told
- [ ] 6. Deleted on both sides: stays deleted
- [ ] 7. One side renamed, the other ticked the same item: the whole item is compared, so the earlier change is lost (documents the known limit of the rule)
- [ ] 8. `merge(a, b) == merge(b, a)` and `merge(a, a) == a`

## Later (not the merge)

- [ ] Repository: read from Room first, queue writes made offline, send them when online (with fakes for local and remote)
- [ ] Room DAO and migrations (in-memory database)
- [ ] ViewModel state
