<div align="center">
<img src="https://github.com/codingcat2468/ServerChangelogs/blob/master/logo.png?raw=true" alt="Plugin Logo" width="350" height="350">

# ServerChangelogs

[![Downloads](https://img.shields.io/modrinth/dt/server-changelogs?style=for-the-badge&logo=modrinth)](https://modrinth.com/project/server-changelogs)
[![Github Repo](https://img.shields.io/badge/github-repo-blue?logo=github&style=for-the-badge)](https://github.com/codingcat2468/ServerChangelogs)
[![License](https://img.shields.io/github/license/codingcat2468/ServerChangelogs?style=for-the-badge)](https://github.com/codingcat2468/ServerChangelogs/blob/master/LICENSE)


Keep players up-to-date on server changes using intuitive, dialog-based UIs!
</div>

<hr>

This plugin allows you to show changelogs to your players in a very accessible form, using minecraft's new-ish **dialog UIs**! Compared to similar solutions in chat / using books, this approach can't be overseen or be hard to read, and allows players to review earlier changes by just scrolling down!

## Features
Currently, the plugin supports the following features:
- Showing **new changelogs** to players upon **joining**
- **Configuration-phase** dialogs that will show **before** players even **join the world**
- Tracking **whether** a player **has read** a certain changelog
- **Indicators** for **unread changelogs** in the UI
- **Viewing** all changelogs using **commands**
- Very **user-friendly** creation of changelogs using **dialog UIs** through a dialog-based editor
- Saving **editor sessions** for later, so you can **exit** and **come back** to an in-progress changelog
- [**MiniMessage**](https://docs.papermc.io/adventure/minimessage/format/) support for **creating changelogs**
- A **completely customizable** changelog screen
- Customizable **translations** based on the player's **client language**

## Supported Server Software & Versions
This is a **Fabric** port of [ServerChangelogs](https://github.com/codingcat2468/ServerChangelogs). It is a **server-side only** mod, so vanilla clients can join without installing anything.
It is built against **Minecraft 26.2** and relies on the vanilla dialog system, which does not exist before `1.21.6`.

## Installation
Requires [Fabric API](https://modrinth.com/mod/fabric-api). Drop the mod jar into your `mods` folder and restart your server.
The mod generates its config files in `config/ServerChangelogs`.
Permissions are checked through [fabric-permissions-api](https://github.com/lucko/fabric-permissions-api) (works with e.g. LuckPerms); alternatively set `use_native_fallback_permissions: true` to use vanilla operators.

## Commands & Permissions
The plugin provides a few commands, to which access can be controlled using the following permissions:
<table>
  <tr>
    <th>Plugin Command</th>
    <th>Permission</th>
    <th>Description</th>
  </tr>
  <tr>
    <td><code>/scl info</code></td>
    <td>server_changelogs.command.info</td>
    <td>Provides version information about the plugin & platform</td>
  </tr>
  <tr>
    <td><code>/scl create</code></td>
    <td>server_changelogs.command.create</td>
    <td>Opens the UI to publish a new changelog</td>
  </tr>
  <tr>
    <td><code>/scl view</code></td>
    <td>server_changelogs.command.view</td>
    <td>Opens the player-facing changelog UI</td>
  </tr>
  <tr>
    <td><code>/scl reload</code></td>
    <td>server_changelogs.command.reload</td>
    <td>Reloads the plugin configuration and all languages</td>
  </tr>
  <tr>
    <td><code>/changelog</code></td>
    <td>server_changelogs.dedicated_command</td>
    <td>Does the same as <code>/scl view</code>, but using a dedicated command; this can be more intuitive for players! Note that this is <b>NOT</b> <code>/changelogs</code> (no S at the end).</td>
  </tr>
  <tr>
    <td>-</td>
    <td>server_changelogs.manage</td>
    <td>Allows the user to edit/delete existing changelogs through buttons in the changelog UI</td>
  </tr>
</table>

Note that `/server_changelogs`, `/changelogs` and `/scl` are just **aliases** of the
**same command** (they do the same thing and are controlled by the same permissions)!

## Configuration & Language
The plugin configuration can be found in `config.yml` and includes a few basic options for storage, formatting and UI layout.

Most of the actual UI customization can be done in the **language files** (found in the `lang` directory), which are formatted using [MiniMessage](https://docs.papermc.io/adventure/minimessage/format). If you're not sure what part of the file to edit, just search for the text you're trying to change in-game in the file!

Out of the box, the plugin currently only supports **American English** (`en_US`), but more language files can be added using the same country code format (e.g. `de_DE`, `fr_FR`, `en_GB`).

### Custom MiniMessage Tags & Sub-Translations
The plugin registers a few custom **MiniMessage Tags** you can use in any of the translations, including:
- `<prefix>`: The plugin prefix defined in the `prefix` translation
- `<plugin:name>`: The (internally defined) name of the plugin
- `<plugin:description>`: The (internally defined) plugin description
- `<plugin:version>`: The currently running version of the plugin
- `<plugin:authors>`: A comma-separated list of plugin authors
- `<platform:name>`: The (colored/formatted) name of the current platform *(e.g. Paper)*
- `<platform:version>`: The current version that the platform the plugin runs on is reporting
- `<translate:your.key.here>`: Allows you to include values of other translations in another translations. Unlike MM's `<lang>` tag, this is specifically made to work with the plugin's translations! Note that this **does not** support arguments as of now.

Throughout the language files, you'll also come across tags looking something like this: `<arg:0>`.

Those are **argument tags**, which contain specific text defined by the plugin (such as the actual changelog, the author, and more). Those arguments are **specific** to a **given translation**, meaning re-using them in another translation either won't work or give potentially unwanted results!

If multiple arguments are present in a certain translation, they can be identified by the number after the `:`.

## Roadmap
ServerChangelogs was originally made for a **smaller minecraft server** I'm a part of, to make players aware of changes without having to rely on e.g. **Discord**. Because we wanted this feature to be available pretty fast, the plugin initially won't have as many features as I'd like it to have.

The roadmap below contains some of the features that I'm **planning to add** in the future:
- [ ] **JDBC (relational) database support**: Currently, the plugin stores all changelog data (including who has already seen what changelogs) in a plain YAML file. The reason for this mostly comes down to  the stuff above (we wanted a solution fast), so we didn't prioritize storage. This does start to cause problems over time, since the amount of data that has to be (de-)serialized for each startup/write operation increases by a lot! A proper implementation would also have to consider things like auto-migration from the existing YAML files.
- [ ] **Pre-defined authors**: Rather than having to enter the name of the changelog's author every time (possibly including formatting), there could be a list of pre-defined authors in the plugin config that a user could choose from.
- [ ] **Customizable changelog dialog layout**: Currently, the changelog dialog always shows all previous changelogs, with unread ones marked red. In the future, it'd be nice to have an option for this, e.g. so players on join could also only see the most recent changelog.
- [ ] **Paged changelogs**: At the moment, all existing changelogs are just added to the dialog without any additional checks. While this is fine with just a few of them, it could quickly become an issue with a long history of changelogs. So adding a page system with a configurable amount of changelogs per page would be great!
- [x] **Player first-join storage**: Right now, changelogs are just displayed to players upon joining whenever they haven't read them (except for the **first time** a player joins a server). This can be very confusing to new-ish players, since they weren't there for all the previous changes to begin with! A possible (while not 100% perfect) solution to this would be storing when the player initially joined the server / was first seen by the plugin, to then only display relevant changelogs to them.
- [ ] **Other UI types**: The changes in this lower section are planned for a not-so-close future, the stuff above is more relevant for now! Anyway, even though other UI types (like chat, books, etc.) may not be as intuitive, some server owners might still prefer them. While there are plugins out there that can already do this, it would be nice for those to be available along with the additional features this plugin provides.
- [ ] **A poll/voting system?**: A bit of a far-fetched idea, but since changelogs include a lot of, well, *change*, they can sometimes be controversial along players. I feel like a voting system might fit this plugin kind of well, so players can immediately share their opinion on a specific change right when they see it!

> Contributions over on [GitHub](https://github.com/codingcat2468/ServerChangelogs) are always welcome, so if you do want to help out, it'd be nice to see if you are interested in implementing/helping with some of the above ideas!