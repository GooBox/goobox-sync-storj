# goobox-sync-storj

Sync app for Storj.

## Prerequisites

1. Install [Java](https://java.com/download/) 7 or later
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
