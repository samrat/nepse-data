(ns nepse-data.core
  (:require [me.raynes.laser :as l]
            [clj-http.client :as http]))

(def latest-share-url "http://nepalstock.com/datanepse/index.php")

(defn all-traded []
  (let [html (l/parse (:body (http/get latest-share-url)))
        title-row (-> html
                      (l/select (l/class= "dataTable"))
                      first
                      l/zip
                      (l/select (l/class= "rowtitle1")))
        title-row-vals (tr-values title-row)
        transactions (-> html
                         (l/select (l/class= "dataTable"))
                         first
                         l/zip
                         (l/select (l/class= "row1")))
        figures (fn [company posn]
                  (-> company
                      :content
                      (nth posn)
                      :content
                      first
                      (clojure.string/replace #"," "")
                      (Integer/parseInt)))
        date (-> html)]
    (map (fn [company]
           {:company (-> company
                         :content
                         second
                         :content
                         first
                         :content
                         first)
            :stock-symbol (-> company
                              :content
                              second
                              :content
                              first
                              :attrs
                              :href
                              (clojure.string/split #"=")
                              second)
            :number-transactions (figures company 2)
            :max-price (figures company 3)
            :min-price (figures company 4)
            :closing-price (figures company 5)
            :total-share (figures company 6)
            :amount (figures company 7)
            :previous-closing (figures company 8)
            :difference-in-rs (figures company 9)})
         transactions)))

(defn tr-values
  "Returns the values in a HTML table row."
  [tr]
  (-> tr
      :content
      (#(filter map? %))
      (#(map :content %))
      (#(map last %))))

(defn stock-details [stock-symbol]
  (let [base-url "http://nepalstock.com/companydetail.php?StockSymbol=%s"
        stock-page (l/parse (:body (http/get (format base-url stock-symbol))))
        first-table (-> stock-page
                        (l/select (l/class= "dataTable"))
                        first
                        l/zip
                        (l/select (l/class= "row1")))
        first-table-vals (tr-values first-table)]
    {:last-traded (first first-table-vals)
     :last-trade-price (-> (second first-table-vals)
                           (clojure.string/split #"Rs. ")
                           second)
     :net-change-in-rs (-> (nth first-))}
    ))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
