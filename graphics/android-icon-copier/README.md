[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-android--icon--copier-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/1325)

What is this
============
A commandline tool to copy Android Material Design and FontAwesome icons to your
 project folders: `drawable-ldpi`, `drawable-mdpi`, and etc.

How it works
============
It downloads from these repos:
- Material design: https://github.com/google/material-design-icons
- FontAwesome and "Classic" Android: https://github.com/svenkapudija/Android-Action-Bar-Icons

Resolution supported
====================
        | l | m | h | xh | xxh | xxxh |
--------|---|---|---|----|-----|------|
FA      | Y | Y | Y | Y  |  Y  |   -  |
Classic | - | Y | Y | Y  |  Y  |   -  |
Material| - | Y | Y | Y  |  Y  |   Y  |

Sizes supported
===============
Material: 18, 24, 36, 48 dp.
FA and Classic: 32 dp only.


Usage
=====
<pre>
Usage:
Material      : ./copy {proj path} {category} {color} {icon name} [size]
Classic and FA: ./copy {proj path} {fa/classic} {color} {icon name}
</pre>

`[]` denotes optional args.
**Args are case sensitive!**

- `proj path`: Path to project folder relative to `base path`.
    - `base path` can be defined in options file (see below).
    - Auto-detects new or old project structure: `MyProject/src/main/res` or
      `MyProject/res`.
- `category`: Either "classic", "fa", or Material category.
- `color`: Color of icon: Either "white", "grey" or "black".
    - For Classic and FA, "white" refers to the Holo Dark theme (dark background).
      "Grey" refers to the Holo Light theme.
    - For Material, "grey" refers to grey600.
- `icon name`: Name of icon (must replace spaces and dashes with underscores).
    - Without any prefix. Examples: FontAwesome "thumbs_up", Classic "search".
- `size` (integer): for Material only, Size in dp, defaults to 24 which is the
    action bar icon size for material design.

Examples
--------
- `./copy MyProject maps white place`
    - Downloads to `BasePath/MyProject/{src/main}/res/drawable-{m,h,xh,xxh,xxh}dpi
- `./copy MyProject maps white place 48`
- `./copy Path/to/MyProject fa  grey thumbs_up`

Windows users need to use `python copy` instead (I think).

Filename mapping
================
The tool also supports filename mapping of destination png files. (see options)
Mapping vars:

- `cat`: category
- `name`: name as specified in commandline.
- `color`: color as specified: white, black, grey.
- `size`: integer only.
- `bg`: derived from color. black => bright, white => dark, grey => light.
- `bgSuffix`: "_dark" if bg is dark else empty string.

Options file
============

*In this project, if you need to fetch new icons, rename options.templ.json to options.json. And then run script get-material-icons.sh*

Named `options.json` in same dir. Sample:
```json
{
    "basePath": "~/Documents",
    "filenameMap": {
        "classic": "ic_action_{name}{bgSuffix}.png",
        "fa": "ic_action_fa_{name}{bgSuffix}.png",
        "material": "ic_{name}_{color}_{size}dp.png"
    }
}
```

~ is expanded to the user home dir.

`./copy Path/to/MyProject fa  white thumbs_up`  results in the
target filename of `ic_action_fa_thumbs_up_dark.png`.

Installation
============
- Python >= 2.7 (older or newer ver might work, you may try.)
- Python Requests package: `pip install requests`
- Git clone this repo or download the script.

Icon cheatsheet
===============
- Material: http://google.github.io/material-design-icons/
- FA: http://fortawesome.github.io/Font-Awesome/icons/ (icons in 4.2 not supported)
- Classic: coming soon.

License
=======
This project is under the MIT License. (see LICENSE)

Please refer to the respective icon library for its licensing info.
