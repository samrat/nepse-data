(ns nepse-data.scrape-test
  (:use clojure.test
        [nepse-data.scrape :exclude [html get-html update-html]])
  (:require [me.raynes.laser :as l]))

(def html (atom (l/parse (slurp "test/datanepse20130629.html"))))

(deftest market-status
  (is (= (market-open?) false)))

(deftest test-live-data
  (is (= (count (live-data)) 88))
  (is (= (apply = (map :as-of (live-data))) true))
  (is (= (first (live-data)) {:percent-change 1.886792452830189,
                              :as-of "2013-06-27T14:58:50+05:45",
                              :net-change-in-rs 2,
                              :total-share 1234,
                              :latest-trade-price 108,
                              :stock-symbol "ACEDBL"}))
  (is (= (filter #(> (:percent-change %) 5) (live-data))
         [{:percent-change 9.48905109489051, :as-of "2013-06-27T14:58:50+05:45", :net-change-in-rs 13, :total-share 56748, :latest-trade-price 150, :stock-symbol "ILFC"}
          {:percent-change 5.555555555555556, :as-of "2013-06-27T14:58:50+05:45", :net-change-in-rs 9, :total-share 120, :latest-trade-price 171, :stock-symbol "SIFC"}
          {:percent-change 5.128205128205128, :as-of "2013-06-27T14:58:50+05:45", :net-change-in-rs 4, :total-share 211, :latest-trade-price 82, :stock-symbol "UFIL"}
          {:percent-change 9.89010989010989, :as-of "2013-06-27T14:58:50+05:45", :net-change-in-rs 9, :total-share 742, :latest-trade-price 100, :stock-symbol "ZFL"}])))

(deftest test-market-info
  (is (= (market-info) {:nepse {:current 495.54,
                                :points-change -2.5,
                                :percent-change -0.5},
                        :sensitive {:current 123.65,
                                    :points-change -0.83,
                                    :percent-change -0.66},
                        :as-of "2013-06-27"})))

;; same as test-live-data since market is closed
(deftest test-all-traded
  (is (= (count (all-traded)) 88))
  (is (= (apply = (map :as-of (all-traded))) true))
  (is (= (first (all-traded)) {:percent-change 1.886792452830189,
                              :as-of "2013-06-27T14:58:50+05:45",
                              :net-change-in-rs 2,
                              :total-share 1234,
                              :latest-trade-price 108,
                              :stock-symbol "ACEDBL"}))
  (is (= (filter #(> (:percent-change %) 5) (all-traded))
         [{:percent-change 9.48905109489051, :as-of "2013-06-27T14:58:50+05:45", :net-change-in-rs 13, :total-share 56748, :latest-trade-price 150, :stock-symbol "ILFC"}
          {:percent-change 5.555555555555556, :as-of "2013-06-27T14:58:50+05:45", :net-change-in-rs 9, :total-share 120, :latest-trade-price 171, :stock-symbol "SIFC"}
          {:percent-change 5.128205128205128, :as-of "2013-06-27T14:58:50+05:45", :net-change-in-rs 4, :total-share 211, :latest-trade-price 82, :stock-symbol "UFIL"}
          {:percent-change 9.89010989010989, :as-of "2013-06-27T14:58:50+05:45", :net-change-in-rs 9, :total-share 742, :latest-trade-price 100, :stock-symbol "ZFL"}])))