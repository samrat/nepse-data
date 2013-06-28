(ns nepse-data.web
  (:use [compojure.core :only (defroutes GET)]
        [ring.adapter.jetty :as ring]
        [ring.middleware.json]
        [ring.util.response])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [nepse-data.api :as nepse]))

(defroutes routes
  (route/resources "/")
  (GET "/market-status" [] (response (nepse/market-status)))
  (GET "/live-data" [] (response (nepse/live-data)))
  (GET "/stock-details/:symbol" [symbol] (response (nepse/stock-details symbol)))
  ;;(route/not-found (layout/four-oh-four))
  )

(def application (-> (handler/site routes)
                     wrap-json-response))

(defn start [port]
  (run-jetty application {:port port :join? false}))

(defn -main []
  (let [port (Integer/parseInt 
               (or (System/getenv "PORT") "8080"))]
  (start port)))