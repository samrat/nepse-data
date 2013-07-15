(ns nepse-data.utils
  (:require [clojure.string :as str]
            [me.raynes.laser :as l]))

(defn parse-rupees
  "Returns the extracted numerical value from a string in the format
  Rs.50 or Rs. 50."
  [string]
  (try (-> (str/split string #"Rs. *")
           second
           (str/replace #"," "")
           read-string
           biginteger)
       (catch Exception _ "NA")))

(defn parse-percentage
  "Returns the extracted numerical value from a string in the format
  50% or 50 %"
  [string]
  (-> (str/split string #" *%")
      first
      read-string))

(defn parse-string [string]
  (cond (.startsWith string "Rs.") (parse-rupees string)
        (.startsWith string "-") (read-string string)
        (.endsWith string "%") (parse-percentage string)
        (re-find #"\-" string) string ;; dates
        (and (re-find #"\d+" string)
             (not (re-find #"[A-Za-z]+" string))) (-> string
                                                      (str/replace #"," "")
                                                      read-string)
        :else string))

(defn node-text
  "Returns the text value of a node and its contents."
  [node]
  (cond
   (string? node) node
   (and (map? node) (not= :comment (:type node))) (str/join (map node-text (:content node)))
   :else ""))

(defn nth-table
  "Returns the nth dataTable from parsed-html as a hickory node."
  [parsed-html idx]
  (-> parsed-html
      (l/select (l/class= "dataTable"))
      (nth idx)
      l/zip))

(defn tr->vec
  [row]
  (-> row
      (l/select (l/element= "td"))
      (#(map node-text %))
      (#(map str/trim %))
      vec))

(defn href
  "Returns link from an <a> tag."
  [a]
  (-> a
      :content
      second
      :content
      first
      :attrs
      :href))

(def stock-details-titles [["Last Traded Date"
                            "Last Trade Price"
                            "Net Chg."
                            "%Change"
                            "High"
                            "Low"
                            "Previous Close"
                            "Quote"]
                           ["Listed Shares"
                            "Paid Up Value"
                            "Total Paid Up Value"
                            "Closing Market Price"
                            "Market Capitalization"
                            "Market Capitalization Date"]])

(defmacro futures
  [n & exprs]
  (vec (for [_ (range n)
             expr exprs]
         `(future ~expr))))