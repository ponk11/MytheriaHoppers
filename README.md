# Mytheria Hoppers

Mytheria Hoppers lets players upgrade placed hoppers through an in-game menu. Upgrades can increase transfer speed and item pickup range. Players can also unlock a filter and choose which item types a hopper will pick up.

## Features

- Upgrade hopper transfer speed, up to level 4 by default.
- Increase the range at which a hopper picks up dropped items, up to level 4 by default.
- Unlock an item filter for each hopper, then add allowed item types to its slots.
- Set upgrade prices, filter prices, slot limits, and hopper transfer settings in `plugins/MytheriaHoppers/config.yml`.
- Save hopper upgrades and filter settings when the server shuts down.
- Convert existing hoppers gradually after installation to limit the work done during startup.
- Support worlds loaded by Multiverse-Core, Iris, and other world-management plugins.

## Install

1. Install Vault and an economy plugin that provides a Vault economy.
2. Put `MytheriaHoppers.jar` in the server's `plugins` folder.
3. Restart the server. The plugin creates its config file on first startup.

Players need a working Vault economy to buy upgrades or unlock filters. Without one, the plugin logs a warning and purchases are unavailable.

## For players

Right-click a placed hopper to open its upgrade menu. The menu shows the current speed and range levels, the cost of the next upgrade, and the filter settings. In the filter menu, players can unlock the filter, buy more slots, and add or remove item types. When a filter is active, that hopper picks up only the selected item types.

By default, speed and range each have four upgrade levels. The filter starts locked, costs $5,000,000 to unlock, and has up to 18 slots. Each additional slot costs $250,000.

## Admin commands

| Command | Description |
| --- | --- |
| `/mh` | Show the plugin command hint. |
| `/mh reload` | Reload the plugin configuration. |
| `/mh admin` | Open the administrator hopper world-count and removal menu. |

`/mytheriahoppers` is also available as the full command name.

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `mytheriahoppers.use` | Everyone | Use hopper upgrades. |
| `mytheriahoppers.filter` | Everyone | Unlock and use item filters. |
| `mytheriahoppers.admin` | Operators | General admin permission. |
| `mytheriahoppers.admin.reload` | Operators | Reload the configuration. |
| `mytheriahoppers.admin.bypass` | Operators | Bypass hopper restrictions. |

## Configuration

The main settings are in `plugins/MytheriaHoppers/config.yml`. The default speed upgrades use transfer intervals of 8, 6, 4, and 2 ticks. The default range levels are 2, 4, 6, and 8 blocks. Prices and these values can be changed in the matching `speed-upgrades` and `range-upgrades` sections.

`conversion.chunks-per-second` controls how quickly saved chunks are checked for existing hoppers after installation. The default is 2 chunks per second. Use `/mh reload` after changing settings.

Worlds loaded after Mytheria Hoppers starts are detected automatically. Saved hopper data waits for those worlds to load, and their loaded and saved chunks are scanned for existing hoppers. The admin menu is available with `/mh admin` for players with `mytheriahoppers.admin`.
# Mytheria Hoppers

Mytheria Hoppers is a configurable Paper 1.16.5 hopper upgrade plugin.

## Hopper types

When `hopper-types.enabled` is enabled, administrators can create:

- **Crop** hoppers for farm output
- **Mob** hoppers for mob drops
- **Ore** hoppers for mined resources
- **Universal** hoppers for the original unrestricted behavior

Typed hoppers start with a one-chunk collection radius. Their range upgrades increase
the chunk radius, while universal hoppers retain block-radius behavior. Use
`/mh give <player> <all|crop|mob|ore> [amount]` to create hopper items.

## Administration

`/mh admin` opens the administration panel. It includes per-world removal with
two-step confirmation, undo for the last removal, and a settings panel where
administrators can toggle typed hoppers or reload the configuration.
