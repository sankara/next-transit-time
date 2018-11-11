(ns next-transit.bart
  (:require [next-transit.config :refer [env]]
            [clj-http.client :as client]
            [java-time :as t]
            [java-time-literals.core]))


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

(defn- fetch-stations []
  (-> (call-bart-api {:api "stn" :cmd "stns"})
      :root
      :stations
      :station))

(def stations
  (into {}
        (map #(identity [(keyword (:abbr %)) %])
             (fetch-stations))))
;;stations

(comment
  (defn- format-station-csv [[_ station]]
    (clojure.string/join "," (map #(str "\"" % "\"") (vals (select-keys station [:name, :abbr])))))
  (format-station-csv (first stations))
  (map #(println (format-station-csv %)) stations))


(defn- parse-bart-time [time]
  (try (t/local-time "h:mm a" time)
       (catch java.time.format.DateTimeParseException e
         (t/local-time "hh:mm a" time))))
;;(parse-bart-time "11:59 PM")
;;(parse-bart-time "1:10 AM")

(defn- extract-time-values [keys names hash]
  (let [parsed-values (map #(parse-bart-time (get hash (keyword %))) names)]
    (apply assoc {} (interleave keys parsed-values))))
;;(extract-time-values [:arrive] ["@origTimeMin"] (assoc {} (keyword "@origTimeMin") "11:50 AM"))

(defn next-bart [from_k to_k]
  (let [from (name from_k)
        to (name to_k)
        response (call-bart-api {:api "sched"
                                 :cmd "depart"
                                 :orig from
                                 :dest to})]
    (->> response
         :root
         :schedule
         :request
         :trip
         (map #(extract-time-values [from to]
                                    ["@origTimeMin" "@destTimeMin"]
                                    %))
         (filter #(t/after? (get % from) (t/local-time))))))

;; (next-bart :24TH :ROCK)
;; (next-bart :LAKE :EMBR)
