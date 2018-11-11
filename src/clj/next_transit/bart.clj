(ns next-transit.bart
  (:require [next-transit.config :refer [env]]
            [clj-http.client :as client]
            [java-time :as t]
            [java-time-literals.core]
            [clojure.tools.logging :as log]))

(def api-key
  (-> env :bart :api-key))

;;api-key

(defn- call-bart-api [request & {:keys [client-params]}]
  (log/info "Calling bart with " request)
  (let [endpoint (str "https://api.bart.gov/api/" (:api request) ".aspx")
        query (merge request {:json "y" :key api-key})
        params (merge {:query-params query :as :json} client-params)
        response (client/get endpoint params)]
    (log/info "Received response: " response)
    (:body response)))

;;(def response (call-bart-api {:api "stn" :cmd "stns"} :client-params {:debug true}))

(def stations
  (memoize (fn []
             (->> (call-bart-api {:api "stn" :cmd "stns"})
                  :root
                  :stations
                  :station
                  (map (fn [stn] [(keyword (:abbr stn)) stn]))
                  (into {})))))
;;(stations)

(comment
  (defn- format-station-csv [[_ station]]
    (clojure.string/join "," (map #(str "\"" % "\"") (vals (select-keys station [:name, :abbr])))))
  (format-station-csv (first stations))
  (map #(println (format-station-csv %)) (stations)))

(defn- call-schedule-api [from_k to_k]
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
         :trip)))
;;(call-schedule-api :24TH :ROCK)
;;(def sched-hash (first (call-schedule-api :24TH :ROCK)))



(defn- parse-bart-time [time]
  (try (t/local-time "h:mm a" time)
       (catch java.time.format.DateTimeParseException e
         (t/local-time "hh:mm a" time))))
;;(parse-bart-time "11:59 PM")
;;(parse-bart-time "1:10 AM")

(defn- extract-value [hash s]
  (let [k (keyword (str "@" s))
        v (get hash k)]
    (if (re-find #"Time" s)
      (parse-bart-time v)
      (keyword v))))
;;(extract-value sched-hash "origin")
;;(extract-value sched-hash "origTimeMin")

(defn- extract-schedule [sched-hash]
  (apply assoc {}
         (map (partial extract-value sched-hash)
              ["origin" "origTimeMin" "destination" "destTimeMin"])))

;;(extract-schedule sched-hash)


(defn next-bart [from to]
  (->> (call-schedule-api from to)
       (map extract-schedule)
       (filter #(t/after? (get % from) (t/local-time)))))

;; (next-bart :24TH :ROCK)
;; (next-bart :LAKE :EMBR)
