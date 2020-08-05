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
        query    (merge request {:json "y" :key api-key})
        params   (merge {:query-params query :as :json} client-params)
        response (client/get endpoint params)]
    (log/info "Received response: " response)
    (:body response)))

;;(def response (call-bart-api {:api "stn" :cmd "stns"} :client-params {:debug true}))

(defn- format-station-abbr [str]
  (keyword (clojure.string/lower-case str)))

;;FIXME: Occasionally caches empty {} and fails miserably
(def stations
  (memoize
    (fn []
      (->> (call-bart-api {:api "stn" :cmd "stns"})
           :root
        :stations
           :station
        (map (fn [stn] [(format-station-abbr (:abbr stn)) stn]))
           (into {})))))

;;(stations)

(defn station-name [station-code]
  (-> (stations)
      (get station-code)
      (get :name)))

;;(station-name :lake)

(comment
  (defn- format-station-csv [[key station]]
    (clojure.string/join "," (map #(str "\"" % "\"") [(:name station) (name key)])))
  (format-station-csv (first (stations)))
  (map #(println (format-station-csv %)) (stations)))

(defn- call-schedule-api [from_k to_k]
  (let [from     (name from_k)
        to       (name to_k)
        response (call-bart-api
                   {:api  "sched"
                    :cmd  "depart"
                    :orig from
                    :dest to})]
    (->> response
         :root
      :schedule
      :request
      :trip)))

;;(call-schedule-api :24th :rock)
;;(def sched-hash (first (call-schedule-api :24th :rock)))


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
      (format-station-abbr v))))

;;(extract-value sched-hash "origin")
;;(extract-value sched-hash "origTimeMin")

(defn- extract-schedule [sched-hash]
  (apply assoc {}
         (map (partial extract-value sched-hash)
              ["origin" "origTimeMin" "destination" "destTimeMin"])))

;;(extract-schedule sched-hash)


;;(def from :24th)
;;(def to :rock)
(defn next-bart [from to]
  (->> (call-schedule-api from to)
       (map extract-schedule)
       (filter #(t/after? (get % from) (t/local-time)))))

;;(map extract-schedule (call-schedule-api from to))
;; (next-bart :24th :rock)
;; (next-bart :lake :embr)
