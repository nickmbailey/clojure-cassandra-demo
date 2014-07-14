(ns clojure-cassandra-demo.db
  (require [qbits.hayt :refer :all]
           [qbits.alia :as alia]
           [clj-time.core :as t]
           [clj-time.coerce :as c]
           [clj-time.format :as f])
  (:gen-class))

(def cluster (alia/cluster {:contact-points ["localhost"]}))
(def session (alia/connect cluster))

(def create-clojure-cassandra-demo
  (create-keyspace :clojure_cassandra_demo
    (if-exists false)
    (with {:replication
            {:class "SimpleStrategy"
             :replication_factor 3}})))

(def create-user-song-counts
  (create-table :user_song_counts
    (if-exists false)
    (column-definitions {:username :text
                         :year_and_month :text
                         :artist :text
                         :song :text
                         :count :counter
                         :primary-key [[:username :year_and_month] :artist :song]})))

(def create-artist-song-counts
  (create-table :artist_song_counts
    (if-exists false)
    (column-definitions {:artist :text
                         :year_and_week :text
                         :album :text
                         :song :text
                         :count :counter
                         :primary-key [[:artist :year_and_week] :album :song]})))

(def create-artist-history
  (create-table :artist_history
    (if-exists false)
    (column-definitions {:artist :text
                         :year_and_week :text
                         :played_at :timestamp
                         :username :text
                         :song :text
                         :album :text
                         :primary-key [[:artist :year_and_week] :played_at]})
    (with {:clustering-order [[:played_at :desc]]})))

(def create-song-history
  (create-table :song_history
    (if-exists false)
    (column-definitions {:song :text
                         :year_and_week :text
                         :played_at :timestamp
                         :username :text
                         :artist :text
                         :album :text
                         :primary-key [[:song :year_and_week] :played_at]})
    (with {:clustering-order [[:played_at :desc]]})))

(def create-user-history
  (create-table :user_history
    (if-exists false)
    (column-definitions {:username :text
                         :year_and_month :text
                         :played_at :timestamp
                         :song :text
                         :artist :text
                         :album :text
                         :primary-key [[:username :year_and_month] :played_at]})
    (with {:clustering-order [[:played_at :desc]]})))

(defn insert-user-history [username played-at song artist album year-and-month]
  (alia/execute-async session
    (insert :user_history
      (values
        :username username
        :year_and_month year-and-month
        :played_at played-at
        :song song
        :artist artist
        :album album))))

(defn insert-user-song-counts [username played-at song artist album year-and-month]
  (alia/execute-async session
    (update :user_song_counts
      (set-columns
        {:count [+ 1]})
      (where [[= :username username]
              [= :artist artist]
              [= :song song]
              [= :year_and_month year-and-month]]))))

(defn insert-artist-song-counts [username played-at song artist album year-and-week]
  (alia/execute-async session
    (update :artist_song_counts
      (set-columns
        {:count [+ 1]})
      (where [[= :artist artist]
              [= :album album]
              [= :song song]
              [= :year_and_week year-and-week]]))))

(defn insert-song-history [username played-at song artist album year-and-week]
  (alia/execute-async session
    (insert :song_history
      (values
        :song song
        :year_and_week year-and-week
        :played_at played-at
        :username username
        :artist artist
        :album album))))

(defn insert-artist-history [username played-at song artist album year-and-week]
  (alia/execute-async session
    (insert :artist_history
      (values
        :artist artist
        :year_and_week year-and-week
        :played_at played-at
        :username username
        :song song
        :album album))))

(defn track-played [username played-at song artist album]
  (alia/execute session (use-keyspace :clojure_cassandra_demo))
  (let [played-at-datetime (c/from-long played-at)
        year-and-month (f/unparse (f/formatters :year-month) played-at-datetime)
        year-and-week (f/unparse (f/formatters :weekyear-week) played-at-datetime)
        promises [(insert-user-history username played-at song artist album year-and-month)
                  (insert-artist-history username played-at song artist album year-and-week)
                  (insert-song-history username played-at song artist album year-and-week)
                  (insert-user-song-counts username played-at song artist album year-and-month)
                  (insert-artist-song-counts username played-at song artist album year-and-week)]]
    (doseq [p promises]
      @p)))

(defn get-artist-popular-songs [artist year-and-week]
  (alia/execute session (use-keyspace :clojure_cassandra_demo))
  (let [results (alia/execute
                  session
                  (select
                    :artist_song_counts
                    (where [[= :artist artist]
                            [= :year_and_week year-and-week]])))]
    (->> results
      (sort-by :count)
      (reverse)
      (take 10))))

(defn get-user-favorite-songs [username year month]
  (alia/execute session (use-keyspace :clojure_cassandra_demo))
  (let [dt (t/date-time year month)
        year-and-month (f/unparse (f/formatters :year-month) dt)
        results (alia/execute
                  session
                  (select
                    :user_song_counts
                    (where [[= :username username]
                            [= :year_and_month year-and-month]])))]
    (->> results
      (sort-by :count)
      (reverse)
      (take 10))))

(defn get-user-history [username year month]
   (let [dt (t/date-time year month)
         year-and-month (f/unparse (f/formatters :year-month) dt)]
    (alia/execute session (use-keyspace :clojure_cassandra_demo))
    (alia/execute session (select
                            :user_history
                            (where [[= :username username]
                                    [= :year_and_month year-and-month]])))))

(defn get-artist-history [artist year-and-week]
  (alia/execute session (use-keyspace :clojure_cassandra_demo))
  (alia/execute session (select
                          :artist_history
                          (where [[= :artist artist]
                                  [= :year_and_week year-and-week]]))))

(defn init-db []
  (alia/execute session create-clojure-cassandra-demo)
  (alia/execute session (use-keyspace :clojure_cassandra_demo))
  (alia/execute session create-user-song-counts)
  (alia/execute session create-artist-song-counts)
  (alia/execute session create-user-history)
  (alia/execute session create-song-history)
  (alia/execute session create-artist-history))
