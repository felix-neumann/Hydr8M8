# Security Policy

## Reporting a vulnerability

Please **do not open a public issue** for security problems. Report them privately via
[GitHub private vulnerability reporting](https://github.com/felix-neumann/Hydr8M8/security/advisories/new).
You will get an answer as soon as possible; fixes are released as a new version on the
[Releases](https://github.com/felix-neumann/Hydr8M8/releases) page.

## Supported versions

Only the latest release receives fixes.

## How Hydr8M8 protects you

- All data stays on the device (app sandbox, no account, no server, no tracking). Backups go to
  your own Google account via Android Auto Backup.
- Network access is HTTPS only (GitHub for update checks, Open-Meteo for the weather bonus with
  location rounded to about 1 km).
- Updates: the app downloads only release assets of this repository and verifies that the file is
  signed with the Hydr8M8 key and is a newer version before offering it for installation. The app
  itself holds no permission to install packages; Android's system installer does that.
- Install releases only from this repository's Releases page.
