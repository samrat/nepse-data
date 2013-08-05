(ns nepse-data.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            ;;[clojure.java.jdbc.ddl :as ddl]
            )
  (:use [nepse-data.scrape :only [listed-companies
                                  ninety-days-info]]
        [clj-time.coerce :only [to-sql-date]]))

(def db "postgres://localhost:5432/mydb")

(defn historical-data-schema
  []
  (jdbc/with-connection db
    (try (jdbc/create-table "historical"
                            [:symbol :varchar "NOT NULL"]
                            [:date :date]
                            [:transactions :int]
                            [:volume :int]
                            [:traded_amount :int]
                            [:open_price :int]
                            [:max_price :int]
                            [:min_price :int]
                            [:close_price :int])
         (catch Exception e e))))

(defn listed-companies-schema
  []
  (jdbc/with-connection db
    (try (jdbc/create-table "companies"
                            [:symbol :varchar "NOT NULL"])
         (catch Exception e e))))

(defn insert-companies
  []
  (apply jdbc/insert! db
                :companies
                nil
                (map vector (listed-companies))))

(defn companies
  []
  (jdbc/query db
              (sql/select * :companies)))

(defn insert-history
  []
  (doseq [stock (map :symbol (companies))
          day (vals (apply dissoc (try (ninety-days-info stock)
                                       (catch Exception _))
                           [:company :stock-symbol]))]
    (when (empty?
           (jdbc/query db
                       (sql/select
                        * { :historical :h}
                        (sql/where {:h.symbol stock
                                    :h.date (to-sql-date (:date day))}))))
      (jdbc/insert! db
                    :historical
                    (-> (zipmap [:transactions :volume
                                 :traded_amount :open_price :max_price
                                 :min_price :close_price]
                                ((juxt :total-transactions :total-traded-shares
                                       :total-traded-amount :open-price
                                       :max-price :min-price :closing-price)
                                 day))
                        (assoc :symbol stock)
                        (assoc :date (to-sql-date (:date day))))))))