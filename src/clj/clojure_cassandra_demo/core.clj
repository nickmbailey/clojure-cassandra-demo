(ns clojure-cassandra-demo.core
  (require [clojure-cassandra-demo.db :refer :all]
           [clojure-cassandra-demo.util :refer [load-tracks to-long]]
           [compojure.core :refer [defroutes ANY GET POST]]
           [compojure.handler :refer [site]]
           [compojure.route :refer [resources]]
           [me.raynes.least :as least]
           [ring.adapter.jetty :as jetty]
           [ring.middleware.reload :refer [wrap-reload]]
           [ring.util.response :refer [redirect]]
           [clojure.tools.logging :as log]
           [hiccup.core :refer :all]
           [hiccup.form :as form]
           [clj-time.core :as t]
           [clj-time.coerce :as c]
           [clj-time.format :as f])
  (:gen-class))

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
    [:h3 "Welcome"]
    [:a {:href "/load"} "Load User History from Last.fm"] [:br] [:br]
    [:a {:href "/user/nickmbailey"} "View User: nickmbailey"] [:br] [:br]
    [:a {:href "/artist/Tokyo Police Club"} "View Artist: Tokyo Police Club"]))

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
     [:tr [:td [:u "Song"]] [:td [:u "Album"]] [:td [:u "Play Count"]]]
     (map popular-track-content (get-artist-popular-songs artist year-and-week))]
    [:h4 "Users Listening"]
    [:table {:id "artist-song-list"}
     [:tr [:td [:u "User"]] [:td [:u "Song"]] [:td [:u "Album"]] [:td [:u "Played"]]]
     (map artist-track-list-content (get-artist-history artist year-and-week))]))

(defn user-content [username year month]
  (layout
    [:h3 (str "User: " username)]
    [:h4 "Favorite Songs"]
    [:table {:id "favorite-song-list"}
     [:tr [:td [:u "Artist"]] [:td [:u "Song"]] [:td [:u "Play Count"]]]
     (map favorite-track-content (get-user-favorite-songs username year month))]
    [:h4 "Listened Songs"]
    [:table {:id "played-song-list"}
     [:tr [:td [:u "Artist"]] [:td [:u "Song"]] [:td [:u "Album"]] [:td [:u "Played"]]]
     (map user-track-list-content (get-user-history username year month))]))

(defroutes app-routes
  (POST "/api/load-tracks" request
    (load-tracks (:username (:params request)))
    (redirect "/"))

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
