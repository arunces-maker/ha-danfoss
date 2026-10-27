<div align="center">
<h1>Danfoss Icon Controller Add-on: two-controller fork</h1>
</div>

This is a fork of [soundvibe/ha-danfoss](https://github.com/soundvibe/ha-danfoss) that runs **two Danfoss Icon controllers (App Modules) from one add-on**. Typical use: a two-floor house with one Icon system per floor.

Upstream supports a single Icon controller and names sensors by room number only (`sensor.danfoss_0_temperature`), so with two controllers each one's room 0 writes to the same sensor. This fork:

- runs two instances side by side: **controller 1A** on port `9199` and **controller 2A** on port `9200`
- keeps a separate pairing file for each controller
- names the Home Assistant sensors after the name each controller has in the Danfoss Icon app, e.g. `sensor.danfoss_namas_1a_0_temperature`

Everything else (pairing flow, sensor attributes, REST commands) works as in upstream v0.4.8, which this fork is based on.

## Installation

1. Back up Home Assistant.
2. Go to **Settings → Add-ons → Add-on Store → ⋮ → Repositories** and add:

       https://github.com/arunces-maker/ha-danfoss

3. Install **Danfoss Icon** from this repository. It uses the same slug (`danfoss`) as upstream, so remove the upstream add-on first.
4. Start the add-on, then pair each controller (see the [add-on README](danfoss-addon/README.md#pairing)).

If you already ran upstream with one controller, its pairing is moved to controller 1A automatically, so only controller 2A needs pairing.

## Documentation

- [Add-on README](danfoss-addon/README.md): pairing, files, sensor names, known limitations
- [Configuration and REST commands](danfoss-addon/DOCS.md)
- [Changelog](danfoss-addon/CHANGELOG.md)
- [Release process](danfoss-addon/RELEASE.md)

## Credits

All protocol and add-on work is by [soundvibe](https://github.com/soundvibe). If upstream was useful to you, consider supporting the author:

[![paypal](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://paypal.me/soundvibe)
