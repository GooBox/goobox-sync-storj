# goobox-sync-storj

[![Build Status](https://travis-ci.org/GooBox/goobox-sync-storj.svg?branch=master)](https://travis-ci.org/GooBox/goobox-sync-storj)

Sync app for Storj.

## Prerequisites

1. Install [Java](https://java.com/download/) 8 or later. Make sure to install the 64-bit variant.
1. Install [libstorj](https://github.com/Storj/libstorj)
1. Import your keys using the libstorj CLI. Don't use a passcode. This is not supported yet by the sync app.

## Installation

1. Download the ZIP package of the [latest release](https://github.com/GooBox/goobox-sync-storj/releases/latest)
1. Extract the ZIP on the local file system

## Usage

1. Open a Command Prompt / Terminal
1. Navigate to the extracted `goobox-sync-storj` folder
1. Run the batch file, i.e. `goobox-sync-storj.bat`

The app uses the auth file in the `<user.home>/.storj` folder created by the libstorj CLI for the authentication with the Storj bridge.

Currently the app supports basic two-way sync between the Storj cloud to the local file system. The app will sync the content of the bucket with name `Goobox` to the local folder with name `Goobox`, which is a subfolder of the user home folder. If either the bucket or the local folder do not exist, the app will automatically create empty ones.

The app will poll the Storj cloud and the local `Goobox` sync folder once per minute for any changes in the content. Basic scenarios should work: initial sync, downloading and uploading modified files. More care is required for more complex scenarios: conflicts, download/upload failures, etc.

The app uses an embedded Nitrine database for storing the current sync state of the files. The DB file can be found at the following location:
- `C:\Users\<user-name>\AppData\Local\Goobox` for Windows
- `~/.local/share/Goobox` for Linux
- `~/Library/Application Support/Goobox` for macOS

The `list-db.bat` script can be used to dump the content of the database. This might be useful for debugging.
