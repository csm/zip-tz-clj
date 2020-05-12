(import '[org.geotools.data DataStore DataStoreFinder])
(require '[clojure.java.io :as io])

; zip code info from https://www.census.gov/geographies/mapping-files/time-series/geo/carto-boundary-file.html

(def zips-url (io/resource "zips/cb_2018_us_zcta510_500k.shp"))
(def zips (DataStoreFinder/getDataStore {"url" (str zips-url)}))

(defn ^java.util.Iterator wrap-feature-iterator
  [^org.geotools.feature.FeatureIterator fiter]
  (reify java.util.Iterator
    (hasNext [_] (.hasNext fiter))
    (next [_] (.next fiter))))

(def feature-source (.getFeatureSource zips (first (.getTypeNames zips))))
(def feature-collection (.getFeatures feature-source))
(def features (iterator-seq (wrap-feature-iterator (.features feature-collection))))

; time zone info from https://github.com/evansiroky/timezone-boundary-builder/releases

(def tzs-url (io/resource "tzs/combined-shapefile.shp"))
(def tzs (DataStoreFinder/getDataStore {"url" (str tzs-url)}))

(def tz-feature-source (.getFeatureSource tzs (first (.getTypeNames tzs))))
(def tz-feature-collection (.getFeatures tz-feature-source))
(def tz-features (iterator-seq (wrap-feature-iterator (.features tz-feature-collection))))

(import 'org.geotools.referencing.CRS)
(def crs-xform (CRS/findMathTransform (-> tz-features first (.. getDescriptor getType getCoordinateReferenceSystem))
                                      (-> features first (.. getDescriptor getType getCoordinateReferenceSystem))
                                      true))

; I think this is what we need to do, since the two data sets are in different
; projections: convert the time zone values from their current projection to
; the projection in the zip code data set.

(import 'org.geotools.geometry.jts.JTS)
(def xformed-tz-features
  (map (fn [tz] [tz (JTS/transform (.. tz getDefaultGeometryProperty getValue) crs-xform)])
       tz-features))

(def zip->tz
  (filter some?
    (map (fn [zip]
           (print "mapping zip" (.. zip (getProperty "GEOID10") (getValue)) "... ")
           (.flush *out*)
           (let [tz (->> xformed-tz-features
                         ; first see if the zip code polygon intersects the time zone polygon...
                         (filter #(try (.intersects (second %)
                                                    (.. zip getDefaultGeometryProperty getValue))
                                       (catch Exception x
                                         (println "exception testing intersection:" x))))
                         ; ... then find the largest intersection, since zip codes near the
                         ; borders of time zones (or if there are zip codes in multiple time
                         ; zones at once, for real) will intersect too.
                         (reduce (fn [cur-max e]
                                   (let [a1 (when cur-max
                                              (.getArea (.intersection (second cur-max)
                                                                       (.. zip getDefaultGeometryProperty getValue))))
                                         a2 (.getArea (.intersection (second e)
                                                                     (.. zip getDefaultGeometryProperty getValue)))]
                                     (if (or (nil? a1) (< a1 a2))
                                       e
                                       cur-max)))
                                 nil)
                         (first))]
             (println (some-> tz (..  (getProperty "tzid") (getValue))))
             [(.. zip (getProperty "GEOID10") (getValue))
              (or (some-> tz (.. (getProperty "tzid") (getValue))) "unknown")]))
         features)))

; compress the list, so contiguous zip codes can map to the same time zone
; this could make up zip codes out of thin air, if there are gaps. But we don't
; care that much.

(def zip->tz2 (loop [items []
                     prev nil
                     vals (sort-by first zip->tz)]
                (if-let [[val & vals] vals]
                  (if (and (some? prev)
                           (= (:tz prev) (second val)))
                    (recur items (assoc prev :end (first val)) vals)
                    (recur (cond-> items (some? prev) (conj prev))
                           {:start (first val) :end (first val) :tz (second val)}
                           vals))
                  (cond-> items (some? prev) (conj prev)))))

(with-open [out (io/writer "zip->tz.edn")]
  (binding [*out* out]
    (prn zip->tz2)))
