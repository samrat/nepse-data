(ns nepse-data.core
  (:require [me.raynes.laser :as l]
            [clj-http.client :as http]
            [clojure.string :as str])
  (:use nepse-data.utils))

(def latest-share-url "http://nepalstock.com/datanepse/index.php")

(defn market-open?
  "Returns true if the website says its open, false otherwise."
  []
  (let [html (l/parse (:body (http/get latest-share-url)))
        marketcl (l/select html
                           (l/class= "marketcl"))]
    (if (empty? marketcl)
      true
      false)))

(defn live-data []
  (let [html (l/parse (:body (http/get latest-share-url)))
        marquee (-> html
                    (l/select (l/element= "marquee"))
                    first)]
    (->> marquee
         :content
         (remove #(= (:tag %) :img))
         (map l/text)
         (partition 3)
         (map str/join)
         (map #(re-find #"(\S+) (\d+) \( +(\S+) \) \( +(\S+) \)" %))
         (map #(zipmap [:symbol :latest-trade-price
                        :total-share :net-change-in-rs]
                       (map parse-string (drop 1 %)))))))

(defn market-info
  []
  (let [html (l/parse (:body (http/get latest-share-url)))
        market-info-table (-> html
                              (l/select (l/class= "dataTable"))
                              (nth 2)
                              l/zip)
        [nepse sensitive] (-> market-info-table
                              (l/select (l/class= "row1"))
                              (l/zip)
                              (#(map tr->vec %)))
        index-data (fn [idx]
                     {:current (-> (second idx)
                                   parse-string)
                      :points-change (-> (nth idx 2)
                                         parse-string)
                      :percent-change (-> (nth idx 3)
                                          (str/replace #"%" "")
                                          parse-string)})]
    {:nepse (index-data nepse)
     :sensitive (index-data sensitive)}))

(defn all-traded []
  (let [html (l/parse (:body (http/get latest-share-url)))
        trades-table (-> html
                         (l/select (l/class= "dataTable"))
                         first
                         l/zip)
        stock-symbols (map #(-> %
                               :content
                               second
                               :content
                               first
                               :attrs
                               :href
                               (str/split #"=")
                               second)
                           (-> trades-table
                               (l/select (l/class= "row1"))))
        rows (-> trades-table
                (l/select (l/class= "row1"))
                (l/zip)
                (#(map tr->vec %))
                (#(map conj % stock-symbols)))]
    (map #(zipmap [:company          :number-transactions
                   :max-price        :min-price
                   :closing-price    :total-shares
                   :amount           :previous-closing
                   :difference-in-rs :stock-symbol]
                  (map parse-string (drop 1 %))) rows)))

(defn stock-details
  [stock-symbol]
  (let [base-url "http://nepalstock.com/companydetail.php?StockSymbol=%s"
        stock-page (l/parse (:body (http/get (format base-url stock-symbol))))
        first-table (-> stock-page
                        (l/select (l/class= "dataTable"))
                        first
                        l/zip)
        first-table-vals (-> first-table
                             (l/select (l/class= "row1"))
                             l/zip
                             (#(map tr->vec %)))
        second-table (-> stock-page
                        (l/select (l/class= "dataTable"))
                        second
                        l/zip)
        second-table-vals (-> second-table
                             (l/select (l/class= "row1"))
                             l/zip
                             (#(map tr->vec %)))
        all-vals (concat (first first-table-vals)
                         (first second-table-vals))]
    (zipmap [:last-traded-date :last-trade-price
             :net-change-in-rs :percent-change
             :high             :low
             :previous-close   :stock-symbol
             :listed-shares    :paid-up-value
             :total-paid-up-value :closing-market-price
             :market-capitalization :market-capitalization-date]
            (map parse-string all-vals))))
