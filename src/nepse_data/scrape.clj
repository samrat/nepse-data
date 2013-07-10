(ns nepse-data.scrape
  (:require [me.raynes.laser :as l]
            [clj-http.client :as http]
            [clojure.string :as str]
            [clojure.core.memoize :as memo]
            [clj-time.core :as t])
  (:use nepse-data.utils
        [clojure.tools.logging :only [info]]))

(def datanepse-url "http://nepalstock.com/datanepse/index.php")

(defn get-html
  "Pulls and parses html"
  []
  (-> (http/get datanepse-url)
      :body
      l/parse))

(def html
  ^{:doc "Caches parsed HTML of datanepse-url. A thread updates this(see
  update-html."}
  (atom (get-html)))

(defn- ->npt
  "Converts any time to Nepal Time(NPT)."
  [time]
  (t/to-time-zone time
                  (t/time-zone-for-offset 5 45)))

(defn- npt
  "Tell clj-time that the datetime passed is in Nepal Time(NPT)."
  [datetime]
  (t/from-time-zone datetime
                    (t/time-zone-for-offset 5 45)))

(defn update-html
  "Updates html in a separate thread. Decides when to update based on
  the current time in Nepal(NPT). Updates every 30 seconds between
  12:15 and 15:10. The fifteen minute lag at the beginning is due to
  the absence of live-data when the trading floor has just opened."
  []
  (future (loop []
            (let [current-time (->npt (t/now))
                  [y m d] ((juxt t/year t/month t/day) current-time)
                  market-open (t/interval (npt (t/date-time y m d 12 15))
                                          (npt (t/date-time y m d 15 10)))]
              (when (t/within? market-open current-time)
                (reset! html (get-html))
                (info "Fetched HTML from /datanepse/index.php")))
            (Thread/sleep 30000)
            (recur))))

(defn market-open?
  "Returns true if the website says its open, false otherwise."
  []
  (let [marketcl (l/select @html
                           (l/class= "marketcl"))]
    (if (empty? marketcl)
      true
      false)))

(defn market-info
  []
  (let [market-info-table (-> @html
                              (nth-table 2))
        [nepse sensitive] (-> market-info-table
                              (l/select (l/class= "row1"))
                              (l/zip)
                              (#(map tr->vec %)))
        index-data (fn [idx]
                     (zipmap [:current :points-change :percent-change]
                             (map parse-string (drop 1 idx))))
        date (->> @html
                  first
                  node-text
                  (re-seq #"As of ((\d{4})-(\d{2})-(\d{2}))")
                  (#(nth % 2)) ;; third appearance
                  second)]
    {:nepse (index-data nepse)
     :sensitive (index-data sensitive)
     :as-of date
     :market-open (market-open?)}))

(defn live-data
  "If market is open, returns a map containing live data. Otherwise
  returns {:market-open false}"
  []
  (if (market-open?)
    (let [marquee (-> @html
                      (l/select (l/element= "marquee"))
                      first)
          datetime (->> marquee
                        node-text
                        (#(str/replace % "\u00a0" "")) ;; nbsp characters
                        (re-seq
                         #"As of ((\d{4})-(\d{2})-(\d{2}) *(\d{2}):(\d{2}):(\d{2}))")
                        first
                        (drop 2)
                        (t/unparse (t/formatters :date-time-no-ms))
                        (apply format "%s-%s-%sT%s:%s:%s+05:45"))]
      {:market-open true
       :datetime datetime
       :transactions (->> marquee
                          :content
                          (remove #(= (:tag %) :img))
                          (map l/text)
                          (partition 3)
                          (map str/join)
                          (map #(re-find #"(\S+) (\S+) \( +(\S+) \) \( +(\S+) \)" %))
                          (map #(zipmap [:stock-symbol  :latest-trade-price
                                         :shares-traded :net-change-in-rs]
                                        (map parse-string (drop 1 %))))
                          (map #(assoc % :percent-change
                                       (try
                                         (with-precision 3
                                           (* 100M
                                              (/ (get % :net-change-in-rs)
                                                 (- (get % :latest-trade-price)
                                                    (get % :net-change-in-rs)))))
                                         (catch Exception _ 0)))))})
    {:market-open false}))

(defn last-trading-day
  "Returns trading data from the last trading day. To obtain live data
  when market is open, see live-data."
  []
  (let [trades-table (-> @html
                         (nth-table 0))
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
                  second ;; the first appearance is inside the <marquee>
                  second)]
    {:date date
     :transactions (->> (map #(zipmap [:company          :number-transactions
                                       :max-price        :min-price
                                       :closing-price    :shares-traded
                                       :amount           :previous-closing
                                       :difference-in-rs :stock-symbol]
                                      (map parse-string (drop 1 %)))
                             rows)
                        (map #(assoc % :percent-change
                                     (try
                                       (with-precision 3
                                         (* 100M (/ (get % :difference-in-rs)
                                                    (get % :previous-closing))))
                                          (catch Exception _ 0)))))}))

(defn stock-details
  "Returns details for a given stock symbol."
  [stock-symbol]
  (let [base-url "http://nepalstock.com/companydetail.php?StockSymbol=%s"
        stock-page (l/parse (:body (http/get (format base-url stock-symbol))))
        table-row-title (fn [table]
                          (-> table
                              (l/select (l/class= "rowtitle1"))
                              first
                              l/zip
                              tr->vec))
        ;; there are two tables in the stock details page.
        first-table (-> stock-page
                        (nth-table 0))
        first-table-row-title (table-row-title first-table)
        first-table-vals (-> first-table
                             (l/select (l/class= "row1"))
                             l/zip
                             (#(map tr->vec %)))
        second-table (-> stock-page
                         (nth-table 1))
        second-table-row-title (table-row-title second-table)
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
            
            (if (and (= first-table-row-title ["Last Traded Date"
                                               "Last Trade Price"
                                               "Net Chg."
                                               "%Change"
                                               "High"
                                               "Low"
                                               "Previous Close"
                                               "Quote"])
                     (= second-table-row-title ["Listed Shares"
                                                "Paid Up Value"
                                                "Total Paid Up Value"
                                                "Closing Market Price"
                                                "Market Capitalization"
                                                "Market Capitalization Date"] ))
              (map parse-string all-vals)
              (repeat "NA")))))

(def ninety-days-info
  ^{:doc "Show trading details for stock-symbol in the last 90 days."}
  (memo/ttl
   (fn [stock-symbol]
     (let [base-url "http://nepalstock.com/datanepse/stockWisePrices.php"
           soup (http/post base-url {:form-params {"StockSymbol" stock-symbol
                                                   "Submit" "Submit"}})
           parsed (l/parse (:body soup))
           trading-info-table (-> parsed
                                  (nth-table 1))
           trading-info-table-titles (-> trading-info-table
                                         (l/select (l/class= "rowtitle1"))
                                         second
                                         l/zip
                                         tr->vec)
           company (-> trading-info-table
                       (l/select (l/class= "rowtitle1"))
                       first
                       node-text
                       (.trim)
                       (str/split #"\(")
                       first
                       (.trim))
           rows (-> trading-info-table
                    (l/select (l/class= "row1"))
                    l/zip
                    (#(map tr->vec %)))]
       (-> (reduce (fn [m row]
                     (assoc m (first row)
                            (zipmap [:date                :total-transactions
                                     :total-traded-shares :total-traded-amount
                                     :open-price          :max-price
                                     :min-price           :closing-price]
                                    (map parse-string (rest row)))))
                   {}
                   rows)
           (assoc :company company)
           (assoc :stock-symbol stock-symbol))))
   :ttl/threshold (if (market-open?)
                    (* 1000 60 5)
                    (* 1000 60 60 3))))
