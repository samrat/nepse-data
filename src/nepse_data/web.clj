(ns nepse-data.web
  (:use [compojure.core :only (defroutes GET)]
        ring.middleware.json
        ring.middleware.cors
        ring.util.response
        [clojure.tools.logging :only [info]]
        [org.httpkit.server :only [run-server]])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.reload :as reload]
            [nepse-data.scrape :as scrape]))

(defroutes routes
  (route/resources "/")
  (GET "/market-status" [] (response {:market-open (scrape/market-open?)}))
  (GET "/live-data" [] (response (scrape/live-data)))
  (GET "/market-info" [] (response (scrape/market-info)))
  (GET "/last-trading-day" [] (response (scrape/last-trading-day)))
  (GET "/stock-details/:symbol" [symbol] (response (scrape/stock-details
                                                    symbol)))
  (GET "/ninety-days-info/:symbol" [symbol] (response (scrape/ninety-days-info
                                                       symbol)))
  ;;(route/not-found (layout/four-oh-four))
  )

(defn- wrap-request-logging [handler]
  (fn [{:keys [request-method uri] :as req}]
    (let [resp (handler req)]
      (info (name request-method) (:status resp)
            (if-let [qs (:query-string req)]
              (str uri "?" qs) uri))
      resp)))

(def application (-> (handler/site routes)
                     wrap-json-response
                     reload/wrap-reload
                     wrap-request-logging
                     (wrap-cors
                      :access-control-allow-origin #".+")))

(defn -main [& args]
  (let [port (Integer/parseInt 
              (or (System/getenv "PORT") "8080"))]
    (scrape/update-html)
    (run-server application {:port port :join? false})
    (info "Server started. Listening on port " port)))