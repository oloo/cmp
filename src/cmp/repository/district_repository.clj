(ns cmp.repository.district-repository
  (:use korma.db)
  (:use korma.core)
  (:use cmp.models) 
  (:require [clojure.string :as str]
            [cmp.util :as util]))


;; Get all districts from mysql database
(defn get-all-districts
  []
(select district
        (fields :id :name :region)))

(defn get-district-by-id
  [id]
  (first (select district
          (fields :id :name :region)
          (where (= :id id)))))

(defn edit-district
  [id name region]
  (update district 
          (set-fields {:name name
                       :region region})
          (where  {:id id})))

;; Add a new district
(defn add-new-district
  [name region]
  (insert district
  (values {:name name :region region
           :datecreated (util/get-sql-date)
           :datemodified (util/get-sql-datetime)})))