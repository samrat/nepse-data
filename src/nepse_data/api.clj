(ns nepse-data.api
  (:require [cheshire.core :refer :all]
            [nepse-data.scrape :as scrape]))

(defn all-traded
  []
  (scrape/all-traded))

(defn market-status
  []
  {:market-open (scrape/market-open?)})

(defn live-data
  []
  (scrape/live-data))

(defn stock-details
  [symbol]
  (scrape/stock-details symbol))

(defn ninety-days-info
  [sym]
  (scrape/ninety-days-info sym))