<div align="center">
<img src="https://github.com/codingcat2468/ServerChangelogs/blob/master/logo.png?raw=true" alt="Plugin Logo" width="350" height="350">

# ServerChangelogs

[![Downloads](https://img.shields.io/modrinth/dt/server-changelogs?style=for-the-badge&logo=modrinth)](https://modrinth.com/project/server-changelogs)
[![Github Repo](https://img.shields.io/badge/github-repo-blue?logo=github&style=for-the-badge)](https://github.com/codingcat2468/ServerChangelogs)
[![License](https://img.shields.io/github/license/codingcat2468/ServerChangelogs?style=for-the-badge)](https://github.com/codingcat2468/ServerChangelogs/blob/master/LICENSE)


A simple plugin used to show server changes to players using dialogs!
</div>

<hr>

This plugin allows you to show changelogs to your players in a very accessible form, using minecraft's new-ish **dialog UIs**! Compared to similar solutions in chat / using books, this approach can't be overseen or be hard to read, and allows players to review earlier changes by just scrolling down!

## Features
Currently, the plugin supports the following features:
- Showing **new changelogs** to players upon **joining**
- Tracking **whether** a player **has read** a certain changelog
- **Indicators** for **unread changelogs** in the UI
- **Viewing** all changelogs using **commands**
- More **user-friendly** creation of changelogs using **dialog UIs**
- [**MiniMessage**](https://docs.papermc.io/adventure/minimessage/format/) support for **creating changelogs**
- A **completely customizable** changelog screen
- Customizable **translations** based on the player's **client language**

## Supported Server Software & Versions
ServerChangelogs is a **Paper plugin**, meaning it will only run on **Paper** servers, including forks like **Purpur** etc.

The initial version of the plugin is built against **26.1.2**, which doens't always imply it won't work on lower versions. The backwards-compatibility of the Paper API can sometimes work pretty well, but that's not always the case.

In case something does break on earlier versions, let me know and [create an issue](https://github.com/codingcat2468/ServerChangelogs/issues/new) *(as long as you're not trying to run it on 1.8)*, and I **might** be able to take a look at it! Also, keep in mind that I won't keep the version number above updated very frequently, and that updates aren't always neccessary to support a new version, so just give it a try!

## Installation
Installing this plugin should be as simple as **downloading** the appropriate version, and **copying the file** into your `plugins` folder!
After that, **restart your server** and the plugin should **generate** all of the config files **automatically** (you can find them in the `plugins/ServerChangelogs` directory!)

## Commands & Permissions
The plugin provides a few commands, to which access can be controlled using the following permissions:
<table>
  <tr>
    <th>Command</th>
    <th>Permission</th>
    <th>Description</th>
  </tr>
  <tr>
    <td><code>/server_changelogs</code></td>
    <td>server_changelogs.command.root</td>
    <td>Provides version information about the plugin</td>
  </tr>
  <tr>
    <td><code>/scl create</code></td>
    <td>server_changelogs.command.create</td>
    <td>Opens the UI to publish a new changelog/td>
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
</table>

Note that `/server_changelogs`, `/changelogs` and `/scl` are just **aliases** of the
**same command** (they do the same thing and are controlled by the same permission)!

## Configuration & Language
The plugin configuration can be found in `config.yml` and includes a few basic options for storage, formatting and UI layout.

Most of the actual UI customization can be done in the **language files** (found in the `lang` directory), which are formatted using [MiniMessage](https://docs.papermc.io/adventure/minimessage/format). If you're not sure what part of the file to edit, just search for the text you're trying to change in-game in the file!

Out of the box, the plugin currently only supports **american english** (`en_US`), but more language files can be added using the same country code format (e.g. `de_DE`, `fr_FR`, `en_GB`).

### Custom MiniMessage Tags & Sub-Translations
The plugin registers a few custom **MiniMessage Tags** you can use in any of the translations, including:
- `<prefix>`: The plugin prefix defined in the `prefix` translation
- `<plugin:name>`: The (internally defined) name of the plugin
- `<plugin:description>`: The (internally defiend) plugin description
- `<plugin:version>`: The currently running version of the plugin
- `<plugin:authors>`: A comma-separated list of plugin authors
- `<translate:your.key.here>`: Allows you to include values of other translations in another translations. Unlike MM's `<lang>` tag, this is specifically made to work with the plugin's translations! Note that this **does not** support arguments as of now.

Throughout the language files, you'll also come across tags looking something like this: `<arg:0>`.

Those are **argument tags**, which contain specific text defined by the plugin (such as the actual changelog, the author, and more). Those arguments are **specific** to a **given translation**, meaning re-using them in another translation either won't work or give potentially unwanted results!

If multiple arguments are present in a certain translation, they can be identified by the number after the `:`.

## Roadmap
ServerChangelogs was originally made for a **smaller minecraft server** I'm a part of, to make players aware of changes without having to rely on e.g. **Discord**. Because we wanted this feature to be available pretty fast, the plugin initially won't have as many features as I'd like it to have.

The roadmap below contains some of the features that I'm **planning to add** in the future:
- [ ] **Removing/Editing changelogs** *(this is technically possible right now through editing the `_data.yml` file, but an actual UI would be nice to have, especially with other types of data storage!)*
- [ ] **Pre-defined authors**: Rather than having to enter the name of the changelog's author every time (possibly including formatting), there could be a list of pre-defined authors in the plugin config that a user could choose from.
- [ ] **Customizable changelog dialog layout**: Currently, the changelog dialog always shows all previous changelogs, with unread ones marked red. In the future, it'd be nice to have an option for this, e.g. so players on join could also only see the most recent changelog.
- [ ] **Paged changelogs**: At the moment, all existing changelogs are just added to the dialog without any additional checks. While this is fine with just a few of them, it could quickly become an issue with a long history of changelogs. So adding a page system with a configurable amount of changelogs per page would be great!
- [ ] **Player first-join storage**: Right now, changelogs are just displayed to players upon joining whenever they haven't read them (except for the **first time** a player joins a server). This can be very confusing to new-ish players, since they weren't there for all of the previous changes to begin with! A possible (while not 100% perfect) solution to this would be storing when the player initially joined the server / was first seen by the plugin, to then only display relevant changelogs to them.
- [ ] **Other UI types**: The changes in this lower section are planned for a not-so-close future, the stuff above is more relevant for now! Anyways, even though other UI types (like chat, books, etc.) may not be as intuitive, some server owners might still prefer them. While there are plugins out there that can already do this, it would be nice for those to be available along with the additional features this plugin provides.
- [ ] **A poll/voting system?**: A bit of a far-fetched idea, but since changelogs include a lot of, well, *change*, they can sometimes be controversial along players. I feel like a voting system might fit this plugin kind of well, so players can immediately share their opinion on a specific change right when they see it!

> Contributions over on [GitHub](https://github.com/codingcat2468/ServerChangelogs) are always welcome, so if you do want to help out, it'd be nice to see if you are interested in implementing/helping with some of the above ideas!