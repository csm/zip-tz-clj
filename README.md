# zip-tz-clj

[![Clojars Project](https://img.shields.io/clojars/v/com.github.csm/zip-tz.svg)](https://clojars.org/com.github.csm/zip-tz)

Hackery to convert US zip code to time zone.

This uses zip code [data from the US Census Bureau](https://www.census.gov/geographies/mapping-files/time-series/geo/carto-boundary-file.html)
and timezone [data from timezone-boundary-builder](https://github.com/evansiroky/timezone-boundary-builder/releases).

This code may not actually do what I intend it to, but it was just a stab at the problem.

YMMV.

<hr>

```clojure
(require 'zip-tz.core)

(zip-tz.core/zip->tz "95060")
; => "America/Los_Angeles"
```

<hr>

```
$ sqlite3 zip-tz.db
sqlite> select tz from ziptz where begin < '95060' AND end > '95060';
America/Los_Angeles
```
