(ns next-transit.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [next-transit.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[next-transit started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[next-transit has shut down successfully]=-"))
   :middleware wrap-dev})
