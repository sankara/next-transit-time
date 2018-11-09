(ns user
  (:require [next-transit.config :refer [env]]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [mount.core :as mount]
            [next-transit.core :refer [start-app]]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(defn start []
  (mount/start-without #'next-transit.core/repl-server))

(defn stop []
  (mount/stop-except #'next-transit.core/repl-server))

(defn restart []
  (stop)
  (start))

(start)
