(ns next-transit.next-ferry
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [java-time :as t]))

(def ferry-schedule
  (let [data-file (java.io.PushbackReader.
                   (io/reader (io/resource "data/sfbay-ferry.edn")))]
    (edn/read {:readers *data-readers*} data-file)))


(defn- find-route [from to]
  (let [routes (filter #(and (= (:from %) from)
                             (= (:to %) to))
                       (:routes ferry-schedule))]
    (first routes)))
;;(find-route :oak :sffb)

;;(t/after? (t/local-time "06:30") (t/local-time "03:30"))
(defn next-transit-time [from to]
  (let [ct (t/local-time)
        day  (t/day-of-week)
        route (find-route from to)
        departure (get (:departures route) day)
        departure-times (:times
                        (first (filter #(= (:id %) departure)
                                       (:departure-times route))))
        next-departure (first
                        (filter #(t/after? (first %) ct)
                                departure-times))
        [depart arrive] next-departure]

    {:depart depart :arrive arrive}))

;;(def from :oak)
;;(def to :sffb)
;; (next-transit-time :oak :sffb)
