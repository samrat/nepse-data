(ns nepse-data.utils
  (:require [clojure.string :as str]
            [me.raynes.laser :as l]))

(defn parse-rupees
  "Returns the extracted numerical value from a string in the format
  Rs.50 or Rs. 50."
  [string]
  (-> (str/split string #"Rs. *")
      second
      (str/replace #"," "")
      read-string
      biginteger))

(defn parse-percentage
  "Returns the extracted numerical value from a string in the format
  50% or 50 %"
  [string]
  (-> (str/split string #" *%")
      second
      (str/replace #"," "")
      read-string
      float))

(defn parse-string [string]
  (cond (.startsWith string "Rs.") (parse-rupees string)
        (.endsWith string "%") (parse-percentage string)
        (re-find #"\-" string) string ;; dates
        (re-find #"\d+" string) (-> string
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

(defn tr->vec
  [row]
  (-> row
      (l/select (l/element= "td"))
      (#(map node-text %))
      (#(map str/trim %))
      vec))
