# Scraping NFC Sweetspot API

> Please note that these scripts were written ad-hoc to solve a concrete problem
> in an exploratory fashion, and were retrieved more or less as-is from my zsh
> history!

First, get the official list of known/supported Android devices

```sh
wget http://storage.googleapis.com/play_public/supported_devices.csv
```

This list is in utf-16le, so convert to utf-8:

```sh
iconv -f UTF-16LE -t UTF-8 supported_devices.csv -o supported_devices_utf8.csv
```

Get the model names from the list
```sh
cut -d, -f2 supported_devices_utf8.csv | sort | uniq >! device-list
```

Load the sweetspot data from all 

```zsh
function urlencode {
    setopt extendedglob
    echo "${${(j: :)@}//(#b)(?)/%${(l:2::0:):-$[[##16]##${match[1]}]}}"
}
for i in ${(f)"$(<device-list)"}; do
    [[ -e device-data/$i ]] && continue
    curl -s "http://sweetspot.nfcring.com/api/v1/sweetspot?model=$(urlencode $i)" > device-data/$i
    echo $i
done
```

Parsing these hotspot files requires json, we do this in python:
```python
import json
import sys

with open(sys.argv[1], "r") as f:
    data = f.read()

raw = json.loads(data)
raw = json.loads(raw)

xs = []
ys = []
for entry in raw:
    if "maxX" not in entry or "maxY" not in entry:
        continue
    x = float(entry["x"]) / entry["maxX"]
    y = float(entry["y"]) / entry["maxY"]

    xs.append(x)
    ys.append(y)

if len(xs) == 0:
    sys.stderr.write("no data for " + sys.argv[1] + "\n")
    sys.exit(1)

# calculate averages
avgx = sum(xs) / len(xs)
avgy = sum(ys) / len(ys)

# alternatively, calculate medians
# xs = sorted(xs)
# ys = sorted(ys)
# medianx = xs[len(xs)/2]
# mediany = ys[len(ys)/2]

# print '{}, {}, {}'.format(sys.argv[1], medianx, mediany)
print '{}, {}, {}, {}'.format(sys.argv[1], avgx, avgy, len(xs))
```

Call this script from zsh as calcavg.py on each downloaded model file (that's
longer than 4 bytes, which is an empty json object):

```zsh
for i in *(L+4); { echo $i >&2; python ../../calcavg.py $i } >! ../hotspots
```
