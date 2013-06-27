(ns nepse-data.utils
  (:require [clojure.string :as str]))

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
        (re-find #"\d+" string) (-> string
                                    (str/replace #"," "")
                                    read-string)
        :else string))