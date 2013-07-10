(ns nepse-data.scrape-test
  (:use clojure.test
        [nepse-data.scrape])
  (:require [me.raynes.laser :as l]))

;;(future-cancel update-html)

(deftest market-status-closed
  (reset! html (l/parse (slurp "test/datanepse20130629.html")))
  (is (= (market-open?) false)))

(deftest market-status-open
  (reset! html (l/parse (slurp "test/datanepse20130630open.html")))
  (is (= (market-open?) true)))

(deftest test-live-data-closed
  (reset! html (l/parse (slurp "test/datanepse20130629.html")))
  (is (= (:market-open (live-data)) false)))

(deftest test-live-data-open
  (reset! html (l/parse (slurp "test/datanepse20130630open.html")))
  (is (= (:market-open (live-data)) true))
  (is (= (:datetime (live-data)) "2013-06-30T12:34:32+05:45"))
  (is (= (count (:transactions (live-data))) 34)))

(deftest test-market-info-closed
  (reset! html (l/parse (slurp "test/datanepse20130629.html")))
  (is (= (market-info) {:nepse {:current 495.54,
                                :points-change -2.5,
                                :percent-change -0.5},
                        :sensitive {:current 123.65,
                                    :points-change -0.83,
                                    :percent-change -0.66},
                        :as-of "2013-06-27"
                        :market-open false})))

(deftest test-last-trading-day
  (reset! html (l/parse (slurp "test/datanepse20130629.html")))
  (is (= (count (:transactions (last-trading-day))) 88))
  (is (= (:date (last-trading-day)) "2013-06-27"))
  (is (= (first (:transactions (last-trading-day))) {:stock-symbol "ACEDBL",
                                               :previous-closing 106,
                                               :min-price 108,
                                               :closing-price 108,
                                               :difference-in-rs 2,
                                               :number-transactions 2,
                                               :amount 133272,
                                               :shares-traded 1234,
                                               :percent-change 1.89M,
                                               :max-price 108,
                                               :company "Ace Development Bank Limited"}))
  (is (= (map :stock-symbol (filter #(> (:percent-change %) 5)
                                    (:transactions (last-trading-day))))
         ["ILFC" "SIFC" "UFIL" "ZFL"])))