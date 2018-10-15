(ns next-transit.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [next-transit.next-ferry :as ferry]))

(defn- to-alexa-response [{depart :depart, arrive :arrive}]
  (let [output-text (str "The next ferry is at " depart ", and will arrive at " arrive)]
    {:version "1.0"
     :response {:outputSpeech {:type "PlainText"
                               :text output-text}}}))

;;(to-alexa-response {:depart "22:00" :arrive "22:00"})

(defapi service-routes
  {:swagger {:ui "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "Next Transit Service"
                           :description "Just a way to get the next ferry - for now"}}}}

  (context "/api" []
    :tags ["Next Ferry"]

    (GET "/ferry/next" []
      :return {:depart java.time.LocalTime, :arrive java.time.LocalTime}
      :query-params [{from :- String :oak} {to :- String :sffb}]
      :summary      "Returns the next ferry between from and to. Defaults to Oakland Jack London to San Francisco Ferry Building"
      (let [result (ferry/next-transit-time from to)]
        (ok result)))

    (POST "/ferry/next" []
      :return {:version String, :response {:outputSpeech {:type String :text String}}}
      :query-params [{from :- String :oak} {to :- String :sffb}]
      :summary      "Returns the next ferry between from and to. Defaults to Oakland Jack London to San Francisco Ferry Building"
      (let [result (ferry/next-transit-time from to)]
        (ok (to-alexa-response

             result))))))


;;(def from :oak)
;;(def to :sffb)
