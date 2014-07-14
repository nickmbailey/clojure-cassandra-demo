(ns clojure_cassandra_demo.core
  (:require-macros [shoreleave.remotes.macros :as shore-macros])
  (:require [domina :refer [by-id value by-class set-value! append! destroy!]]
            [domina.events :refer [listen! prevent-default]]
            [shoreleave.remotes.http-rpc :refer [remote-callback]]))

(defn load-user []
  (let [username (value (by-id "username-load"))]
   (remote-callback :load-tracks
                    [username]
                    #(js/alert (str "Done loading history for user " username)))))

(defn populate-user-history [history]
  (.log js/console history))

(defn load-user-history []
  (let [username (value (by-id "username-history"))]
   (remote-callback :load-user-history
                    [username nil nil]
                    populate-user-history)))

(defn init []
  (when (and js/document
             (aget js/document "getElementById"))
    (listen! (by-id "load-user") :click (fn [evt] (load-user)))
    (listen! (by-id "load-user-history") :click (fn [evt] (load-user-history)))
))

(init)
