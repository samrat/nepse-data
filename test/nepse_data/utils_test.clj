(ns nepse-data.utils-test
  (:use clojure.test
        nepse-data.utils))

(deftest parse-string-test
  (is (= (parse-string "Rs. 50") 50))
  (is (= (parse-string "24.5%") 24.5))
  (is (= (parse-string "-123") -123))
  (is (= (parse-string "123,456,789") 123456789))
  (is (= (parse-string "ACEDBL") "ACEDBL"))
  (is (= (parse-string "2013-05-16") "2013-05-16")))