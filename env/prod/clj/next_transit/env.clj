(ns next-transit.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[next-transit started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[next-transit has shut down successfully]=-"))
   :middleware identity})
