# Home Assistant Add-on for [Danfoss Icon Controller](https://www.danfoss.com/en-gb/products/dhs/smart-heating/smart-heating/danfoss-icon/): two-controller fork

## Summary

This add-on integrates Danfoss Icon floor heating with Home Assistant. This fork runs **two Icon controllers (App Modules) at once**, for example one per floor of the same house. Each controller runs as its own instance inside the same add-on container.

| | Controller 1A | Controller 2A |
| --- | --- | --- |
| Web UI / pairing / REST port | `9199` | `9200` |
| Pairing file | `/share/danfoss-icon/danfoss_config_1a.json` | `/share/danfoss-icon/danfoss_config_2a.json` |

The labels 1A and 2A only decide which port and pairing file are used. Sensor names come from each controller's name in the Danfoss Icon app (see [Home Assistant sensors](#home-assistant-sensors)).

## Pairing

Pair each controller separately, using the Danfoss Icon **Android** app. The iPhone app uses a different protocol that isn't supported, so borrow an Android phone for pairing; it isn't needed afterwards.

The Danfoss Icon app lists each controller as a separate "house", even when both are in the same building.

1. In the Danfoss Icon app, open the first controller (e.g. the 1st floor) and go to `Settings → Share house` to get a one-time code.
2. Open `http://<home-assistant-ip>:9199`, paste the code **without dashes**, enter any username and click **Start**.
3. Repeat for the second controller (e.g. the 2nd floor) on `http://<home-assistant-ip>:9200`.

When pairing succeeds, the add-on writes that controller's pairing file and starts reading it immediately. On later starts, controllers with an existing pairing file connect automatically.

> **Keep pairing data private.** The pairing files and the success page after pairing contain the private key used to talk to your controller. Don't share them, screenshot them or commit them anywhere.

### Upgrading from upstream (one controller)

On start, if `/share/danfoss-icon/danfoss_config.json` exists and `danfoss_config_1a.json` doesn't, the old file is copied to `danfoss_config_1a.json`. Your existing controller becomes controller 1A without pairing again. The old file is left in place and no longer used.

## Home Assistant sensors

Every 60 seconds by default, each instance pushes its controller's rooms to Home Assistant as sensors:

| Sensor | Example |
| --- | --- |
| `sensor.danfoss_<name>_<room_number>_temperature` | `sensor.danfoss_namas_1a_0_temperature` |
| `sensor.danfoss_<name>_master_controller_last_updated` | `sensor.danfoss_namas_1a_master_controller_last_updated` |

`<name>` is the controller's house name in the Danfoss Icon app, lowercased with everything except `a–z` and `0–9` replaced by `_`. For example, "Namas 1A" becomes `namas_1a`. Give the two controllers different names in the app, e.g. one per floor. **Renaming a controller in the app renames all its sensors**, which breaks automations that use them.

Room sensors carry the same attributes as upstream: `room_number`, `mode`, `preset`, `temperature_home`/`_away`/`_sleep`, `temperature_target`, `battery_level` and others.

Things to know:

- **The sensors are pushed through the HA REST API, not registered by an integration.** They don't appear under Devices & services, and they disappear when Home Assistant restarts until the add-on's next update, up to one update interval later. During the restart the add-on log shows `502 Bad Gateway` errors; that is expected.
- **`unknown_house` sensors.** Right after the add-on starts, the first update can run before the controller has reported its name, so a stray `sensor.danfoss_unknown_house_master_controller_last_updated` may appear. It's harmless and goes away on the next Home Assistant restart.

## Known limitations

- **MQTT is not split per controller.** MQTT discovery IDs and topics (`danfoss_icon_thermostat_room_<n>`, `danfoss/icon/<n>/state`, `danfoss/icon/<n>/set`) still use only the room number. With MQTT enabled, room *n* of controller 1A and room *n* of controller 2A share one climate entity, and a command for that room reaches both controllers. **Keep `mqttEnabled: false`** with two controllers until this is fixed.
- **Exactly two controllers.** Ports and file names are fixed in `run.sh`. If only one controller is paired, the other instance just waits for pairing.
- **`sensorNameFmt` and `port` options are ignored.** Names are built from the controller's name in the app and ports are fixed (see [DOCS.md](DOCS.md#configuration-parameters)).
- **The watchdog checks only port 9199.** If either instance exits, the add-on stops both, so Supervisor restarts the whole add-on.

## Documentation

Configuration options and setting up climate entities without MQTT: [DOCS.md](DOCS.md).
