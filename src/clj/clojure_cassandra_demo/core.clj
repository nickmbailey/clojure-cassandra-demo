(ns clojure-cassandra-demo.core
  (require [clojure-cassandra-demo.db :refer :all]
           [compojure.core :refer [defroutes ANY GET POST]]
           [compojure.handler :refer [site]]
           [compojure.route :refer [resources]]
           [me.raynes.least :as least]
           [ring.adapter.jetty :as jetty]
           [ring.middleware.json :refer [wrap-json-response]]
           [ring.middleware.reload :refer [wrap-reload]]
           [ring.util.response :refer [redirect]]
           [clojure.tools.logging :as log]
           [hiccup.core :refer :all]
           [hiccup.form :as form]
           [clj-time.core :as t]
           [clj-time.coerce :as c]
           [clj-time.format :as f])
  (:gen-class))

(def api-key (atom "7098d67eb72a0bebbb470b07a6eb4493"))

(defn to-long [x]
  (Long/parseLong x))

(defn fetch-from-lastfm [username page]
  (let [result (least/read
                 "user.getRecentTracks"
                 @api-key
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
          (let [played-at (* 1000 (to-long (:uts (:date track)))) ; convert to milliseconds
                song (:name track)
                artist (:name (:artist track))
                album (:text (:album track))]
            (track-played username played-at song artist album)))
        (when-not (or (= page total-pages) (= page 10))
          (recur (inc page)))))
    (catch Exception e
      (log/error e "Problem loading user history for" username))))

(defn layout [& content]
  (html
      [:head
           [:meta {:http-equiv "Content-type"
                        :content "text/html; charset=utf-8"}]
           [:title "Clojure Cassandra Demo"]]
      [:body
        [:center
          [:h2 "Clojure Cassandra Demo"]
          content]]))

(defn home-content[]
  (layout
    [:h3 "Welcome"]))

(defn load-lastfm-content []
  (layout
    [:h3 "Load history from last.fm"]
    [:div {:id "username-form"}
     (form/form-to [:post "/api/load-tracks"]
       (form/label "username" "Username:")
       (form/text-field "username")
       (form/submit-button "Submit"))]))

(defn user-track-list-content [track]
  [:tr
   [:td (:artist track)]
   [:td (:song track)]
   [:td (:album track)]
   [:td (:played_at track)]])

(defn artist-track-list-content [track]
  [:tr
   [:td (:username track)]
   [:td (:song track)]
   [:td (:album track)]
   [:td (:played_at track)]])

(defn favorite-track-content [track]
  [:tr
   [:td (:artist track)]
   [:td (:song track)]
   [:td (:count track)]])

(defn popular-track-content [track]
  [:tr
   [:td (:song track)]
   [:td (:album track)]
   [:td (:count track)]])

(defn artist-content [artist year-and-week]
  (layout
    [:h3 (str "Artist: " artist)]
    [:h4 "Popular Songs"]
    [:table {:id "popular-song-list"}
     [:tr [:td "Song"] [:td "Album"] [:td "Play Count"]]
     (map popular-track-content (get-artist-popular-songs artist year-and-week))]
    [:h4 "Users Listening"]
    [:table {:id "artist-song-list"}
     [:tr [:td "User"] [:td "Song"] [:td "Album"] [:td "Played"]]
     (map artist-track-list-content (get-artist-history artist year-and-week))]))

(defn user-content [username year month]
  (layout
    [:h3 (str "User: " username)]
    [:h4 "Favorite Songs"]
    [:table {:id "favorite-song-list"}
     [:tr [:td "Artist"] [:td "Song"] [:td "Play Count"]]
     (map favorite-track-content (get-user-favorite-songs username year month))]
    [:h4 "Listened Songs"]
    [:table {:id "played-song-list"}
     [:tr [:td "Artist"] [:td "Song"] [:td "Album"] [:td "Played"]]
     (map user-track-list-content (get-user-history username year month))]))

(defroutes app-routes
  (POST "/api/load-tracks" request
    {:status 200
     :body (load-tracks (:username (:params request)))})

  (GET "/load" [username]
    (load-lastfm-content))

  (GET "/user/:username" [username]
    (let [dt (t/now)
          year (t/year dt)
          month (t/month dt)]
      (redirect (str "/user/" username "/" year "/" month))))

  (GET "/user/:username/:year/:month" [username year month]
    (user-content username (to-long year) (to-long month)))

  (GET "/artist/:artist" [artist]
    (let [dt (t/now)
          year-and-week (f/unparse (f/formatters :weekyear-week) dt)]
      (redirect (str "/artist/" artist "/" year-and-week))))

  (GET "/artist/:artist/:year-and-week" [artist year-and-week]
    (artist-content artist year-and-week))

  (GET "/" []
    (home-content))

  (resources "/")

  (ANY "/*" [path]
    {:status 404}))

(def handler
  (site app-routes))

(def app (site (var handler)))

(defn startup []
    (init-db))
