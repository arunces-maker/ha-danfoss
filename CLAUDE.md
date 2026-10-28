# CLAUDE.md

Context for Claude Code in this repo.

## What this is

A fork of [soundvibe/ha-danfoss](https://github.com/soundvibe/ha-danfoss) (based on upstream v0.4.8): a Home Assistant add-on for Danfoss Icon floor heating. This fork runs **two Icon controllers (App Modules) at once**, e.g. one per floor.

- **Controller 1A**: port `9199`, pairing file `/share/danfoss-icon/danfoss_config_1a.json`
- **Controller 2A**: port `9200`, pairing file `/share/danfoss-icon/danfoss_config_2a.json`
- Sensor names include the controller's house name from the Danfoss Icon app: `sensor.danfoss_<name>_<room>_temperature`, `sensor.danfoss_<name>_master_controller_last_updated`. If the name isn't known yet, `unknown_house` is used.
- An old single-controller `danfoss_config.json` is copied to `_1a` on start if `_1a` doesn't exist.

**This repo is public.** Never commit personal info: IP addresses, house/controller names, pairing files, private keys, tokens, logs from the real system.

## Layout

- `repository.json`: HA add-on repository metadata
- `danfoss-addon/config.yaml`: add-on manifest; `version` here is the release version
- `danfoss-addon/run.sh`: starts two Java instances (`-DDANFOSS_CONFIG_FILE`, `-DDANFOSS_PORT`), migrates the old config, stops both if either exits
- `danfoss-addon/Dockerfile`: installs OpenJDK 21 and downloads `ha-danfoss-addon.jar` from **this fork's** GitHub release `v<version>` (falls back to latest). The JAR is not built inside Docker.
- `danfoss-addon/src/main/java/net/soundvibe/hasio/`: Java 21 code (`--enable-preview`, string templates)
  - `Application.java`: web UI/pairing page, REST, config file path and port from system properties
  - `Bootstrapper.java`: HA sensor updates, sensor name building (`unknown_house` fallback), MQTT
  - `danfoss/protocol/`: Danfoss SDG/DeviSmart protocol (from upstream, rarely touched)
  - `ha/HomeAssistantClient.java`: HA REST API client
- `danfoss-addon/src/test/`: JUnit tests (`IconMasterTest`, `IconRoomTest`)
- `.github/workflows/maven.yml`: build + tests on push/PR to `main`
- `.github/workflows/release.yml`: on a published GitHub release, builds the JAR and uploads it as `ha-danfoss-addon.jar`

Docs: `README.md` (overview, install), `danfoss-addon/README.md` (pairing, sensors, limitations), `danfoss-addon/DOCS.md` (options, REST commands), `danfoss-addon/CHANGELOG.md`, `danfoss-addon/RELEASE.md`.

## Releasing

1. Bump `version` in `danfoss-addon/config.yaml` (not in `pom.xml`, which stays `0.0.1`).
2. Add a section to `danfoss-addon/CHANGELOG.md`.
3. Commit and push to `main`.
4. Create a GitHub release with tag `v<version>` (e.g. `gh release create v0.5.1 --generate-notes`). CI uploads the JAR.
5. In HA, update the add-on; it downloads the JAR for that version when building the image.

The tag must match `config.yaml`, otherwise the Docker build falls back to the latest JAR. Current version: **0.5.0**.

## Known limitations

- MQTT discovery IDs/topics are not split per controller; keep `mqttEnabled: false` with two controllers.
- Exactly two controllers; ports and file names are fixed in `run.sh`.
- `sensorNameFmt` and `port` options are ignored.
- Watchdog checks only port 9199.
- Sensors are pushed via the HA REST API, so they vanish after an HA restart until the next update.

## Working with the user

- Follow the user's global instructions (language, plain explanations, one question at a time, ask before commit/push and before changing the live Home Assistant).
- Releases go live for anyone using this fork: confirm before creating a GitHub release.
- Keep `README.md`, `danfoss-addon/README.md`, `CHANGELOG.md` and this file up to date with each change.
