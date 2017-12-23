# goobox-sync-storj

[![Build Status](https://travis-ci.org/GooBox/goobox-sync-storj.svg?branch=master)](https://travis-ci.org/GooBox/goobox-sync-storj)

Sync app for Storj.

## Prerequisites

1. Install [Java](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html) 8. Make sure to install the 64-bit variant.
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

The app polls the Storj cloud and the local `Goobox` sync folder once per minute for any changes in the content. If file is created, deleted or modified in the local sync folder, the sync will be triggered within 5 seconds.

Basic sync scenarios should work: initial sync, downloading and uploading modified files. More care is required for more complex scenarios: conflicts, download/upload failures, etc.

The app uses an embedded Nitrine database for storing the current sync state of the files. The DB file can be found at the following location:
- `C:\Users\<user-name>\AppData\Local\Goobox` for Windows
- `~/.local/share/Goobox` for Linux
- `~/Library/Application Support/Goobox` for macOS

The `list-db.bat` script can be used to dump the content of the database. This might be useful for debugging.

### Overlay icons on Windows

Setting up the overlay icons on Windows required the following steps:

1. Make sure the [Microsoft Visual C++ 2010 Redistributable Package (x64)](https://www.microsoft.com/en-US/download/details.aspx?id=13523) is installed on the system
1. Open a Command Prompt as Administrator
1. Execute the `register-dlls.bat` file
1. Restart Windows
1. Run the app as usual
