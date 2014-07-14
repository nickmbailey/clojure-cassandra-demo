(ns clojure-cassandra-demo.util
  (require [clojure-cassandra-demo.db :refer :all]
           [me.raynes.least :as least]
           [clojure.tools.logging :as log]
           [clj-time.core :as t]
           [clj-time.coerce :as c]
           [clj-time.format :as f])
  (:gen-class))

(def api-key (System/getenv "LASTFM_API_KEY"))

(defn to-long [x]
  (Long/parseLong x))

(defn fetch-from-lastfm [username page]
  (let [result (least/read
                 "user.getRecentTracks"
                 api-key
                 {:limit 200 :user username :extended 1 :page page})]
    (if (contains? result :error)
      (throw (Exception. (:message result)))
      result)))

(defn load-tracks [username]
  (log/info "Loading tracks for " username)
  (try
    (loop [page 1]
      (let [result (fetch-from-lastfm username page)
            attr (:attr (:recenttracks result))
            current-page (to-long (:page attr))
            total-pages (to-long (:totalPages attr))]
        (log/info "Page" current-page "out of" total-pages)
        (doseq [track (:track (:recenttracks result))]
          (when (not (nil? (:uts (:date track)))) ; if a song is currently playing, skip it
            (let [
                  played-at (* 1000 (to-long (:uts (:date track)))) ; convert to milliseconds
                  song (:name track)
                  artist (:name (:artist track))
                  album (:text (:album track))]
              (track-played username played-at song artist album))))
        (when-not (or (= page total-pages) (= page 10))
          (recur (inc page)))))
    (catch Exception e
      (log/error e "Problem loading user history for" username))))
