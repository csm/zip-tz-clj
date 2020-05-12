(import 'com.almworks.sqlite4java.SQLiteConnection)

(require '[clojure.java.io :as io])
(def conn (SQLiteConnection. (io/file "zip-tz.db")))
(.open conn)

(def create-stmt (.prepare conn "CREATE TABLE ziptz (begin TEXT PRIMARY KEY, end TEXT, tz TEXT);"))
(.step create-stmt)

(require '[clojure.edn :as edn])
(import 'java.io.PushbackReader)

(def tz-data (edn/read (PushbackReader. (io/reader (io/resource "zip->tz.edn")))))

(doseq [{:keys [start end tz]} tz-data]
  (let [insert-stmt (.prepare conn "INSERT INTO ziptz (begin, end, tz) VALUES (?, ?, ?);")]
    (.bind insert-stmt 1 start)
    (.bind insert-stmt 2 end)
    (.bind insert-stmt 3 tz)
    (.step insert-stmt)))
