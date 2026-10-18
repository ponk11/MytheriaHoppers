# Mytheria Hoppers

Mytheria Hoppers lets players upgrade placed hoppers through an in-game menu. Upgrades can increase transfer speed and item pickup range. Players can also unlock a filter and choose which item types a hopper will pick up.

## Features

- Upgrade hopper transfer speed, up to level 4 by default.
- Increase the range at which a hopper picks up dropped items, up to level 4 by default.
- Unlock an item filter for each hopper, then add allowed item types to its slots.
- Set upgrade prices, filter prices, slot limits, and hopper transfer settings in `plugins/MytheriaHoppers/config.yml`.
- Save hopper upgrades and filter settings when the server shuts down.
- Convert existing hoppers gradually after installation to limit the work done during startup.

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
