(ns next-transit.next-ferry
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [java-time :as t]
            [java-time-literals.core]))

(def ferry-schedule
  (let [data-file (java.io.PushbackReader.
                   (io/reader (io/resource "data/sfbay-ferry.edn")))]
    (edn/read {:readers *data-readers*} data-file)))

(defn terminal-name [terminal-id]
  (get (:terminals ferry-schedule) terminal-id))

;;(terminal-name :oakj)

;;(def t-route (first (:routes ferry-schedule)))

(defn- get-departure-times-for-day
  ([route]
   (get-departure-times-for-day route (t/day-of-week)))
  ([route day]
   (let [departure-set (get (:departures route) day)]
     (get (:departure-times route) departure-set))))

;;(get-departure-times-for-day t-route)
;;(def t-times (get-departure-times-for-day t-route))

;;Route path is a single list of nodes
;;That a path exists and the departure time at the starting terminal
;;is in the next two hours.
(defn- viable-route-path? [src dest route-path]
  (let [src-time (get route-path src)
        dest-time (get route-path dest)]
    (and (not (nil? src-time))
         (not (nil? dest-time))
         (t/after? dest-time src-time))))
         ;;(t/after? src-time (t/local-time)))))
         ;;(t/before? src-time (t/adjust (t/local-time) t/plus (t/hours 12))))))

;;(viable-route-path? :sffb :oakj (last t-times))
;;(viable-route-path? :oakj :sffb (last t-times))

;;(filter (partial viable-route-path? :oakj :sffb) t-times)
;;(filter (partial viable-route-path? :sffb :oakj) t-times)

(defn- viable-route-paths [src dest route]
  (filter (partial viable-route-path? src dest)
          (get-departure-times-for-day route)))

(defn next-transit-times [src dest]
  (->> (:routes ferry-schedule)
       (map (partial viable-route-paths src dest))
       (flatten)
       (sort-by #(get % src))))


;;(next-transit-times :oakj :sffb)
