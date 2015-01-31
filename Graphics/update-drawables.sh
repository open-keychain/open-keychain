#!/bin/bash

APP_DIR=../OpenKeychain/src/main
DRAWABLE_DIR=$APP_DIR/res/drawable
MDPI_DIR=$APP_DIR/res/drawable-mdpi
HDPI_DIR=$APP_DIR/res/drawable-hdpi
XDPI_DIR=$APP_DIR/res/drawable-xhdpi
XXDPI_DIR=$APP_DIR/res/drawable-xxhdpi
XXXDPI_DIR=$APP_DIR/res/drawable-xxxhdpi
PLAY_DIR=./drawables/
SRC_DIR=./drawables/



# Launcher Icon:
# -----------------------
# mdpi: 48x48
# hdpi: 72x72
# xhdpi: 96x96
# xxhdpi: 144x144.
# xxxhdpi 192x192.
# google play: 512x512

# Adobe Illustrator (.ai) exports by Tha Phlash are way better than the Inkscape exports (.svg)

#NAME="ic_launcher"

#inkscape -w 48 -h 48 -e "$MDPI_DIR/$NAME.png" $NAME.svg
#inkscape -w 72 -h 72 -e "$HDPI_DIR/$NAME.png" $NAME.svg
#inkscape -w 96 -h 96 -e "$XDPI_DIR/$NAME.png" $NAME.svg
#inkscape -w 144 -h 144 -e "$XXDPI_DIR/$NAME.png" $NAME.svg
#inkscape -w 192 -h 192 -e "$XXXDPI_DIR/$NAME.png" $NAME.svg
#inkscape -w 512 -h 512 -e "$PLAY_DIR/$NAME.png" $NAME.svg

# Actionbar Icons
# -----------------------
# mdpi: 32x32
# hdpi: 48x48
# xhdpi: 64x64
# xxhdpi: 96x96

for NAME in "ic_action_nfc" "ic_action_qr_code" "ic_action_safeslinger" "ic_action_search_cloud" "ic_cloud_search_24px"
do
echo $NAME
inkscape -w 32 -h 32 -e "$MDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 48 -h 48 -e "$HDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 64 -h 64 -e "$XDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 96 -h 96 -e "$XXDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
done

for NAME in "status_lock_closed" "status_lock_error" "status_lock_open" "status_signature_expired_cutout" "status_signature_expired" "status_signature_invalid_cutout" "status_signature_invalid" "status_signature_revoked_cutout" "status_signature_revoked" "status_signature_unknown_cutout" "status_signature_unknown" "status_signature_unverified_cutout" "status_signature_unverified" "status_signature_verified_cutout" "status_signature_verified" "key_flag_authenticate" "key_flag_certify" "key_flag_encrypt" "key_flag_sign"
do
echo $NAME
inkscape -w 24 -h 24 -e "$MDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 32 -h 32 -e "$HDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 48 -h 48 -e "$XDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 64 -h 64 -e "$XXDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
done

for NAME in "create_key_robot"
do
echo $NAME
inkscape -w 48 -h 48 -e "$MDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 64 -h 64 -e "$HDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 96 -h 96 -e "$XDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 128 -h 128 -e "$XXDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
done

for NAME in "drawer_header"
do
echo $NAME
inkscape -w 512 -h 288 -e "$DRAWABLE_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
done