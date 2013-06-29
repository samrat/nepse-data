(ns nepse-data.scrape
  (:require [me.raynes.laser :as l]
            [clj-http.client :as http]
            [clojure.string :as str])
  (:use nepse-data.utils))

(def latest-share-url "http://nepalstock.com/datanepse/index.php")

(defn get-html
  "Pulls and parses html"
  []
  (-> (http/get latest-share-url)
      :body
      l/parse))

(def html
  ^{:doc "Caches parsed HTML of latest-share-url. A thread updates this(see
  update-html."}
  (atom (get-html)))

(def market-close
  ^{:doc "Stores the market status in order to estimate how frequently the
  @html atom needs to get updated."}
  (atom false))

(def update-html
  ^{:doc "Updates html in a separate thread. Whether the
  market was open or closed in the last check determines when to make
  the next check."}
  (future (loop []
            (reset! html (get-html))
            (if @market-close
              (Thread/sleep (* 60000 60 21))
              (Thread/sleep 60000))
            (recur))))

(defn market-open?
  "Returns true if the website says its open, false otherwise."
  []
  (let [marketcl (l/select @html
                           (l/class= "marketcl"))]
    (if (empty? marketcl)
      true
      false)))

(defn live-data
  "If market is open, returns live data. Otherwise, the data returned
  is the same as that of all-traded. To see if market is open, use
  market-open?"
  []
  (let [marquee (-> @html
                    (l/select (l/element= "marquee"))
                    first)
        datetime (->> marquee
                      node-text
                      (#(str/replace % "\u00a0" ""))
                      (re-seq
                       #"As of ((\d{4})-(\d{2})-(\d{2}) *(\d{2}):(\d{2}):(\d{2}))")
                      first
                      (drop 2)
                      (apply format "%s-%s-%sT%s:%s:%s+05:45")
                      )]
    (->> marquee
         :content
         (remove #(= (:tag %) :img))
         (map l/text)
         (partition 3)
         (map str/join)
         (map #(re-find #"(\S+) (\S+) \( +(\S+) \) \( +(\S+) \)" %))
         (map #(zipmap [:stock-symbol :latest-trade-price
                        :total-share :net-change-in-rs]
                       (map parse-string (drop 1 %))))
         (map #(assoc % :as-of datetime))
         (map #(assoc % :percent-change
                      (try (* 100.
                              (/ (get % :net-change-in-rs)
                                 (- (get % :latest-trade-price)
                                    (get % :net-change-in-rs)
                                    )))
                           (catch Exception _ 0)))))))

(defn market-info
  []
  (let [market-info-table (-> @html
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
                                          parse-string)})
        date (->> @html
                  first
                  node-text
                  (re-seq #"As of ((\d{4})-(\d{2})-(\d{2}))")
                  (#(nth % 2)) ;; third appearance
                  second)]
    {:nepse (index-data nepse)
     :sensitive (index-data sensitive)
     :as-of date}))

(defn all-traded
  "Returns trading data from the last trading day. To obtain live data
  when market is open, see live-data."
  []
  (let [trades-table (-> @html
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
                (#(map conj % stock-symbols)))
        date (->> @html
                  first
                  node-text
                  (re-seq #"As of ((\d{4})-(\d{2})-(\d{2}))")
                  second ;; the first appearance is at inside the <marquee>
                  second)]
    (->> (map #(zipmap [:company          :number-transactions
                        :max-price        :min-price
                        :closing-price    :total-shares
                        :amount           :previous-closing
                        :difference-in-rs :stock-symbol]
                      (map parse-string (drop 1 %)))
             rows)
        (map #(assoc % :as-of date)))))

(defn stock-details
  "Returns details for a given stock symbol."
  [stock-symbol]
  (prn stock-symbol)
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
             :previous-closing   :stock-symbol
             :listed-shares    :paid-up-value
             :total-paid-up-value :closing-market-price
             :market-capitalization :market-capitalization-date]
            (map parse-string all-vals))))
