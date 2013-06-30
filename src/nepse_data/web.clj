(ns nepse-data.web
  (:use [compojure.core :only (defroutes GET)]
        [ring.middleware.json]
        [ring.util.response]
        [org.httpkit.server :only [run-server]])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.reload :as reload]
            [nepse-data.api :as nepse]))

(defroutes routes
  (route/resources "/")
  (GET "/market-status" [] (response (nepse/market-status)))
  (GET "/live-data" [] (response (nepse/live-data)))
  (GET "/stock-details/:symbol" [symbol] (response (nepse/stock-details symbol)))
  ;;(route/not-found (layout/four-oh-four))
  )

(def application (-> (handler/site routes)
                     wrap-json-response
                     reload/wrap-reload))

(defn -main [& args]
  (let [port (Integer/parseInt 
               (or (System/getenv "PORT") "8080"))]
    (run-server application {:port port :join? false})))