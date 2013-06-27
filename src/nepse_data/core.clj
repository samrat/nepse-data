(ns nepse-data.core
  (:require [me.raynes.laser :as l]
            [clj-http.client :as http]))

(def latest-share-url "http://nepalstock.com/datanepse/index.php")

(defn market-open? []
  (let [html (l/parse (:body (http/get latest-share-url)))
        marketcl (l/select html
                           (l/class= "marketcl"))]
    (if (empty? marketcl)
      true
      false)))

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

(defn parse-rupees
  "Returns the extracted numerical value from a string in the format Rs.50 or Rs. 50."
  [string]
  (-> (clojure.string/split string #"Rs. *")
      second
      (clojure.string/replace #"," "")
      read-string
      int))

(defn stock-details [stock-symbol]
  (let [base-url "http://nepalstock.com/companydetail.php?StockSymbol=%s"
        stock-page (l/parse (:body (http/get (format base-url stock-symbol))))
        first-table (-> stock-page
                        (l/select (l/class= "dataTable"))
                        first
                        l/zip
                        (l/select (l/class= "row1"))
                        first)
        first-table-vals (tr-values first-table)
        second-table (-> stock-page
                        (l/select (l/class= "dataTable"))
                        second
                        l/zip
                        (l/select (l/class= "row1"))
                        first)
        second-table-vals (tr-values second-table)]
    (prn second-table-vals)
    {:last-traded (first first-table-vals)
     :last-trade-price (Integer/parseInt (second first-table-vals))
     :net-change-in-rs (-> first-table-vals
                           (nth 2)
                           parse-rupees)
     :percent-change (-> first-table-vals
                         (nth 3)
                         read-string)
     :high (-> first-table-vals
               (nth 4)
               parse-rupees)
     :low (-> first-table-vals
              (nth 5)
              parse-rupees)
     :previous-close (-> first-table-vals
                         (nth 6)
                         parse-rupees)
     :symbol (last first-table-vals)
     :listed-shares (-> (first second-table-vals)
                        (clojure.string/replace #"," "")
                        read-string)
     :paid-up-value (-> (second second-table-vals)
                        parse-rupees)
     :total-paid-up-value (-> (nth second-table-vals 2)
                              parse-rupees)
     :closing-market-price (-> (nth second-table-vals 3)
                               parse-rupees)
     :market-capitalization (-> (nth second-table-vals 4)
                                parse-rupees)
     :market-capitalization-date (last second-table-vals)}))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
