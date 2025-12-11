# VickyEs RELOOTER

This is a mod that adds loot back to chests in chunks that are loaded. It is good for battleroyale like modpacks to prevent going back to world backups and predictable loot.


# VERSION - 0.0.1
> This is the base version with nothing much going on. no changes whatsoever (base).

# VERSION - 0.0.2
> There was a MAJOR fix.
- Fixed loot tables not loading because of file mismatch (major fix)
- Added support for single lootables (loads once per chest) also in the gui creator (minor addition)
- Added reload to config (minor addition)

# VERSION - 0.0.3

> There was yet ANOTHER major fix

- Fixed the bug where because some items (lke TacZ guns) were nbt based they couldn't load because the relootables didn'
  t save nbt. They now do.

# VERSION - 0.0.4

> There were many improvements to the relooter system...

- Indication of chests that have been relooted by adding a tag to the name (can be disabled in config). (minor addition)
- Added the ability to mark a lootable in a loot-table as "sureSpawn" which means per chest assigned to that table that
  item will always be found. This also adds a grouping system so that sure spawns can compete for who actually spawns. (
  minor addition)
- Added a list to the config for containers that can be relooted (though in code only RandomizableContainerBlockEntity
  will be relooted). (minor addition)
- Added the config line to enable or disable the relooter from relooting player placed chests no matter the
  circumstance. (minor addition)
- Fixed a bug for the min and max amount of items a relooter table creates in the relooter gui. (minor bug fix)
- Improved the loot table creation gui tooltips for items. (minor improvement)
- Improved looting logic by making it an asynchronous task for faster and better larger chest count handling. (major
  improvement)