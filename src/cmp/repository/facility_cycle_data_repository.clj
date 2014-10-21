(ns cmp.repository.facility-cycle-data-repository
  (:use korma.db)
  (:use korma.core)
  (:use cmp.models) 
  (:require [clojure.string :as str]
            [cmp.util :as util]
            [cmp.repository.facility-repository :as facility-repository]))

(defn get-facility-cycle-item-data
  [facility-cycle-data-id]
  (select facilitycycleitemdata
          (with facilitycycledata)
          (with item)
          (fields :id [:item.id :itemid] [:item.code :itemcode])
          (where {:facilitycycledata facility-cycle-data-id})
          (order :item.id)))

(defn add-facility-cycle-data
  [year cycle facility-id]
  (insert facilitycycledata
          (values {:year year :facility_id facility-id :cycle cycle
           :datecreated (util/get-sql-date)
           :datemodified (util/get-sql-datetime)})))

(defn add-facility-cycle-item-data-proc
  [facility-cycle-data-id facility-id items data]
  (doseq [ {code :code description :description unitsize :unitsize
              unitcost :unitcost averagequantity :averagequantity 
              plannedquantity :plannedquantity totalcost :totalcost} data]  
    (try
      (if-let [facility-cycle-item-data (get-facility-cycle-item-data facility-cycle-data-id)
               ] nil )
      (catch Exception e (taoensso.timbre/info (str "Error inserting report data " code))))))