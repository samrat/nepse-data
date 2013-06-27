(ns nepse-data.core
  (:require [me.raynes.laser :as l]
            [clj-http.client :as http]
            [clojure.string :as str]))

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
         (map #(zipmap [:symbol :latest-trade-price :total-share :net-change-in-rs]
                       (map parse-string (drop 1 %)))))))

(defn market-info
  []
  (let [html (l/parse (:body (http/get latest-share-url)))
        market-info-table (-> html
                              (l/select (l/class= "dataTable"))
                              (nth 2)
                              l/zip)
        tr->vec (fn [idx]
                  (-> idx
                      (l/select (l/element= "td"))
                      (#(map l/text %))
                      (#(map str/trim %))))
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
        tr->vec (fn [row]
                  (-> row
                      (l/select (l/element= "td"))
                      (#(map l/text %))
                      (#(map str/trim %))
                      vec))
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
    {:last-traded (first first-table-vals)
     :last-trade-price (parse-string (second first-table-vals))
     :net-change-in-rs (-> first-table-vals
                           (nth 2)
                           parse-string)
     :percent-change (-> first-table-vals
                         (nth 3)
                         parse-string)
     :high (-> first-table-vals
               (nth 4)
               parse-string)
     :low (-> first-table-vals
              (nth 5)
              parse-string)
     :previous-close (-> first-table-vals
                         (nth 6)
                         parse-string)
     :symbol (last first-table-vals)
     :listed-shares (-> (first second-table-vals)
                        parse-string)
     :paid-up-value (-> (second second-table-vals)
                        parse-string)
     :total-paid-up-value (-> (nth second-table-vals 2)
                              parse-string)
     :closing-market-price (-> (nth second-table-vals 3)
                               parse-string)
     :market-capitalization (-> (nth second-table-vals 4)
                                parse-string)
     :market-capitalization-date (last second-table-vals)}))
