#!/bin/bash

APP_DIR=../../OpenPGP-Keychain
LDPI_DIR=$APP_DIR/res/drawable-ldpi
MDPI_DIR=$APP_DIR/res/drawable-mdpi
HDPI_DIR=$APP_DIR/res/drawable-hdpi
XDPI_DIR=$APP_DIR/res/drawable-xhdpi
XXDPI_DIR=$APP_DIR/res/drawable-xxhdpi
XXXDPI_DIR=$APP_DIR/res/drawable-xxxhdpi
PLAY_DIR=./


# Launcher Icon:
# -----------------------
# ldpi: 36x36
# mdpi: 48x48
# hdpi: 72x72
# xhdpi: 96x96
# xxhdpi: 144x144.
# xxxhdpi 192x192.
# google play: 512x512

NAME="icon"

inkscape -w 36 -h 36 -e "$LDPI_DIR/$NAME.png" $NAME.svg
inkscape -w 48 -h 48 -e "$MDPI_DIR/$NAME.png" $NAME.svg
inkscape -w 72 -h 72 -e "$HDPI_DIR/$NAME.png" $NAME.svg
inkscape -w 96 -h 96 -e "$XDPI_DIR/$NAME.png" $NAME.svg
inkscape -w 144 -h 144 -e "$XXDPI_DIR/$NAME.png" $NAME.svg
inkscape -w 192 -h 192 -e "$XXXDPI_DIR/$NAME.png" $NAME.svg
inkscape -w 512 -h 512 -e "$PLAY_DIR/$NAME.png" $NAME.svg

