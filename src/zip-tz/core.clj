(ns zip-tz.core
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:import (java.io PushbackReader)))

(let [zip-data (-> (io/resource "zip->tz.edn")
                   (io/reader)
                   (PushbackReader.)
                   (edn/read))]
  (defn zip->tz
    "Find the TZDB timezone ID for zip code zip.

    Takes the first five digits from the input, stripping any whitespace,
    and looks up the timezone.

    Returns nil if no timezone is found, or if zip isn't valid."
    [zip]
    (let [[_ zip] (re-matches #"\s*([0-9]{5}).*" zip)]
      (when zip
        (loop [lo 0
               hi (dec (count zip-data))]
          (let [mid (quot (+ lo hi) 2)
                midval (nth zip-data mid)]
            (println lo hi mid midval)
            (cond (< hi lo)
                  nil

                  (and (>= 0 (compare (:start midval) zip))
                       (<= 0 (compare (:end midval) zip)))
                  (:tz midval)

                  (>= 0 (compare (:end midval) zip))
                  (recur (inc mid) hi)

                  (<= 0 (compare (:start midval) zip))
                  (recur lo (dec mid)))))))))
