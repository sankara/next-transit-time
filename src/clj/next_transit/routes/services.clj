(ns next-transit.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [next-transit.next-ferry :as ferry]
            [next-transit.bart :as bart]
            [clojure.tools.logging :as log]))

(defn- slot-values [request slot-name]
  (->> request
       :request
       :intent
       :slots
       slot-name
       :resolutions
       :resolutionsPerAuthority
       (map :values)
       first
       (map :value)
       (map :id)
       (map keyword)))
;;(def request {:request {:intent {:slots {:from {:resolutions {:resolutionsPerAuthority [{:values [{:value {:name "Oakland" :id "oakj"}}]}]}}}}}})
;;(slot-values request :from)


(defn- parse-request [request]
  (into {} (map #(identity [% (first (slot-values request %))]) [:from :to])))

;;(parse-request request)

(defn- readable-text [kw from to result]
  (let [prefix (condp = kw
                 :next "The next ferry"
                 :later "The one after that")]
    (str prefix " leaves " (ferry/terminal-name from) " at " (get result from)
         " and reaches " (ferry/terminal-name to) " at " (get result to))))

(defn- to-alexa-response [output-text]
  {:version "1.0"
   :response {:outputSpeech {:type "PlainText"
                             :text output-text}}})

;;(to-alexa-response {:depart "22:00" :arrive "22:00"})

(defapi service-routes
  {:swagger {:ui "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "Next Transit Service"
                           :description "Just a way to get the next ferry - for now"}}}}

  (context "/api" []
    (context "/ferry" []
             :tags ["Next Ferry"]

      (GET "/next" []
           :query-params [{from :- s/Keyword :oakj} {to :- s/Keyword :sffb}]
           :summary      "Returns the next ferry between from and to. Defaults to Oakland Jack London to San Francisco Ferry Building"
           (let [result (ferry/next-transit-times from to)]
             (log/info "Processing request with params" {:from from :to to})
             (ok result)))

      (POST "/next" []
            :return {:version String, :response {:outputSpeech {:type String :text String}}}
            :body   [body s/Any]
            :summary "Returns the next ferry between from and to. Defaults to Oakland Jack London to San Francisco Ferry Building"

            (let [{:keys [from to] :or {from :oakj to :sffb}} (parse-request body)
                  [next-ferry later-ferry] (take 2 (ferry/next-transit-times from to))
                  t-next-ferry (readable-text :next from to next-ferry)
                  t-later-ferry (if (not (nil? later-ferry)) (readable-text :later from to later-ferry))]
              (log/info "Processing request with params" {:from from :to to})
              (ok (to-alexa-response (str t-next-ferry ". " t-later-ferry))))))

    (context "/bart" []
             :tags ["Next Bart"]

      (GET "/next" []
           :query-params [{from :- s/Keyword :lake} {to :- s/Keyword :embr}]
           :summary      "Returns the next bart between from and to. Defaults to Lake Merritt to Embarcadero"
           (let [result (bart/next-bart from to)]
             (log/info "Processing request for bart/next with params" {:from from :to to})
             (ok result))))))


;;(def from :oakj)
;;(def to :sffb)
