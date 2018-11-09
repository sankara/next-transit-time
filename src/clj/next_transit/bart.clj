(ns next-transit.bart
  (:require [next-transit.config :refer [env]]
            [clj-http.client :as client]))


(def api-key
  (-> env :bart :api-key))

;;api-key

(defn- call-bart-api [request & {:keys [client-params]}]
  (println client-params)
  (let [endpoint (str "https://api.bart.gov/api/" (:api request) ".aspx")
        query (merge request {:json "y" :key api-key})
        params (merge {:query-params query :as :json} client-params)
        response (client/get endpoint params)]
    (:body response)))

;;(def response (call-bart-api {:api "stn" :cmd "stns"} :client-params {:debug true}))

(defn stations []
  (-> (call-bart-api {:api "stn" :cmd "stns"})
      :root
      :stations
      :station))

;;(def station (first (stations)))
(comment
  (defn- format-station-csv [station]
    (clojure.string/join "," (map #(str "\"" % "\"") (vals (select-keys station [:name, :abbr])))))
  (map #(println (format-station-csv %)) (stations)))
