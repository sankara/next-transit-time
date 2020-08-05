(ns next-transit.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [next-transit.next-ferry :as ferry]
            [next-transit.bart :as bart]
            [clojure.tools.logging :as log]
            [java-time :as t]))

(comment
  (def request
    {:request {:intent
               {:name  "ferry"
                :slots {:from_terminal {:resolutions {:resolutionsPerAuthority
                                                      [{:values [{:value {:name "Oakland" :id "oakj"}}]}]}}}}}}))

(defn- intent-name [request]
  (->> request
       :request
    :intent
       :name
    keyword))

;;(intent-name request)

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

;;(slot-values request :from)

(defn- parse-request [request]
  (let [type       (intent-name request)
        slot-names (case type
                     :ferry {:from :from_terminal
                             :to   :to_terminal}
                     :bart  {:from :from_station
                             :to   :to_station})]
    (as-> slot-names t
          (map (fn [[k v]] [k (first (slot-values request v))]) t)
          (into {} t)
          (assoc t :type type))))

;;(parse-request request)

(defn- readable-text [type kw from to result]
  (let [prefix                      (condp = kw
                                      :next  (str "The next " (name type))
                                      :later "The one after that")
        name-resolver               (case type
                                      :ferry ferry/terminal-name
                                      :bart  bart/station-name)
        [from-name to-name]         (map name-resolver [from to])
        [from-time to-time]         (map (partial get result) [from to])
        [from-time-min to-time-min] (map #(t/time-between (t/local-time) % :minutes) [from-time to-time])]
    (str prefix " leaves " from-name " in " from-time-min " minutes at " (get result from)
         " and reaches " to-name " at " (get result to))))

;;(readable-text :bart :next :lake :embr {:lake (t/local-time "11:30") :embr (t/local-time "12:30")})

;;(t/time-between (t/local-time) (t/local-time "15:45") :minutes)

(defn- to-alexa-response [output-text]
  {:version  "1.0"
   :response {:outputSpeech {:type "PlainText"
                             :text output-text}}})

;;(to-alexa-response {:depart "22:00" :arrive "22:00"})

(defn- get-transit-response [type from to]
  (let [[next-transit later-transit] (take 2
                                           (case type
                                             :ferry (ferry/next-transit-times from to)
                                             :bart  (bart/next-bart from to)))
        t-next-transit               (readable-text type :next from to next-transit)
        t-later-transit              (readable-text type :later from to later-transit)]
    (to-alexa-response (str t-next-transit ". " t-later-transit))))

(defapi service-routes
  {:swagger {:ui   "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version     "1.0.0"
                           :title       "Next Transit Service"
                           :description "Just a way to get the next ferry - for now"}}}}

  (context "/api" []
           (context "/next" []
                    (GET "/transit" []
                         :query-params
                                  [type :- (s/enum :bart :ferry), from :- s/Keyword, to :- s/Keyword]
                         :summary "Returns the next ferry or bart from a given dock/station to a given dock/station"

                         (let [result (case type
                                        :bart  (bart/next-bart from to)
                                        :ferry (ferry/next-transit-times from to))]
                           (log/info "Processing request with params" {:from from :to to})
                           (ok result)))


                    (GET "/ferry" []
                         :query-params [{from :- s/Keyword :oakj} {to :- s/Keyword :sffb}]
                         :summary      "Returns the next ferry between from and to. Defaults to Oakland Jack London to San Francisco Ferry Building"
                         (let [result (ferry/next-transit-times from to)]
                           (log/info "Processing request with params" {:from from :to to})
                           (ok result)))

                    (GET "/bart" []
                         :query-params [{from :- s/Keyword :lake} {to :- s/Keyword :embr}]
                         :summary      "Returns the next bart between from and to. Defaults to Lake Merritt to Embarcadero"
                         (let [result (bart/next-bart from to)]
                           (log/info "Processing request for bart/next with params" {:from from :to to})
                           (ok result)))

                    (POST "/transit" []
                          :return
                                   {:version String, :response {:outputSpeech {:type String :text String}}}
                          :body    [body s/Any]
                          :summary "Invocation from Alexa only. Returns the next bart/ferry"

                          (let [{:keys [type from to]} (parse-request body)]
                            (if (some nil? [type from to]) (log/info body))
                            (log/info "Processing request with params" {:type type :from from :to to})
                            (ok (get-transit-response type from to)))))))

;;(def from :lake)
;;(def to :embr)
