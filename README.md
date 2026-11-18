# Mytheria Hoppers

Upgrade your hoppers instead of replacing them.

Mytheria Hoppers adds per-hopper speed upgrades, collection range upgrades,
item filters, and optional crop/mob/ore hopper types. Upgrades are bought from
an in-game menu with your server's Vault economy.

## Features

- Upgrade hopper **speed** and **collection range** independently.
- Set how often a hopper runs and how many items it moves per cycle.
- Unlock an **item filter** and choose exactly which materials a hopper accepts.
- Use typed hoppers for:
  - **Crop** items and farm drops
  - **Mob** drops and loot
  - **Ore** and common mining drops
  - **Universal** hoppers that accept everything
- Give upgraded hopper items to players; breaking a hopper keeps its upgrades.
- Save hopper upgrades, types, and filters across restarts.
- Import existing-world hoppers gradually from saved region files.
- Manage hoppers by world or type from an admin menu, with confirmation and
  undo for the latest removal.
- Toggle typed hoppers and the hopper crafting recipe without restarting.

## Requirements

- 1.16.5 or newer Paper server
- Java 8 or newer
- Vault and a Vault-compatible economy plugin for paid upgrades


## Installation

1. Install Vault and an economy plugin if you want paid upgrades.
2. Put `MytheriaHoppers.jar` in your server's `plugins` folder.
3. Start the server once.
4. Edit `plugins/MytheriaHoppers/config.yml` if needed.
5. Run `/mh reload`, or restart the server.

Without an economy provider, hoppers still collect and transfer items, but
players cannot buy upgrades or unlock filters.

## How to use

### Players

1. Place a normal hopper, or ask an admin for a typed hopper item.
2. **Right-click** the hopper to open its inventory.
3. **Sneak + left-click** the hopper to open its upgrade menu.
4. Buy speed or range upgrades from the menu.
5. Open **Item Filter Settings**, unlock the filter, and unlock slots as needed.
6. To add a material, hold it on your cursor and click an empty filter slot.
   Click a selected material to remove it.

Filters are allow-lists: once a filter is enabled, the hopper only picks up
the selected materials. Typed hoppers also reject items outside their type.

### Hopper types

Admins can create hopper items with:

```text
/mh give <player> <all|crop|mob|ore> [amount]
```

Typed hoppers start by searching their current chunk. Each range level adds one
chunk of radius. Universal hoppers use block-radius collection instead and
accept every item type.

## Commands

`/mytheriahoppers` is the full command name; `/mh` is its alias.

| Command | Permission | What it does |
| --- | --- | --- |
| `/mh` | None | Opens the help menu. |
| `/mh reload` | `mytheriahoppers.admin.reload` | Reloads `config.yml`. |
| `/mh admin` | `mytheriahoppers.admin` | Opens the administration menu. |
| `/mh give <player> <type> [amount]` | `mytheriahoppers.admin` | Gives a hopper item to an online player. |

The admin menu can:

- remove all tracked hoppers in a world;
- remove all tracked hoppers of one type;
- undo the latest removal when the blocks can be restored;
- enable or disable typed hoppers; and
- enable or disable the hopper crafting recipe.

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `mytheriahoppers.use` | Everyone | Open the hopper upgrade menu. |
| `mytheriahoppers.filter` | Everyone | Open and use hopper filters. |
| `mytheriahoppers.admin` | Operators | Use admin tools and `/mh give`. |
| `mytheriahoppers.admin.reload` | Operators | Use `/mh reload`. |
| `mytheriahoppers.admin.bypass` | Operators | Reserved for server integrations. |

## Configuration

The default configuration is generated at
`plugins/MytheriaHoppers/config.yml`.

| Setting | Default | Purpose |
| --- | ---: | --- |
| `settings.max-speed-level` | `4` | Maximum speed upgrade level. |
| `settings.max-range-level` | `4` | Maximum range upgrade level. |
| `settings.max-hoppers-per-run` | `25` | Processing limit per scheduler pass. |
| `filter.max-slots` | `18` | Maximum materials in one filter. |
| `crafting-recipes.enabled` | `true` | Enable the normal hopper recipe. |
| `conversion.enabled` | `false` | Scan saved region files for existing hoppers. |
| `conversion.chunks-per-second` | `2` | Pace of the conversion scan. |

Speed values are under `speed-upgrades.<level>`:

- `transfer-ticks` controls the interval between processing cycles.
- `transfer-amount` controls the number of items attempted per cycle.
- `cost` controls the Vault economy price.

Range prices are under `range-upgrades.<level>`. For typed hoppers, the level
is also the chunk radius. To import an existing world, enable
`conversion.enabled`, start the server, wait for the scan to finish, then
disable it again.

## Data and backups

Hopper data is stored in `plugins/MytheriaHoppers/data.yml`. The plugin writes
a temporary file before replacing the main file and keeps recent daily
backups in `plugins/MytheriaHoppers/backups/`.
