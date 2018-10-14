(ns next-transit.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [next-transit.next-ferry :as ferry]))

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
                  (ok result)))))

;;(def from :oak)
;;(def to :sffb)
