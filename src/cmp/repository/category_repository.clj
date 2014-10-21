(ns cmp.repository.category-repository
  (:use korma.db)
  (:use korma.core)
  (:use cmp.models)
  (:require [clojure.string :as str]))


;; Get all usernames from mysql database
(defn get-all-categories
  []
(select itemcategory
        (fields :id :name)))

(defn get-all-categories-by-proc-plan-id
  [id]
(select procurementplanitem
        (with procurementplan)
        (with item)
        (join itemcategory (= :item.itemcategory_id :itemcategory.id))
        (where {:procurementplanitem.procurementplan_id id})
        (fields [:itemcategory.id :id] [:itemcategory.name :name])
        (modifier "DISTINCT")))

(defn get-all-categories-by-order-id
  [id]
(select facilityorderitem
        (with facilityorder)
        (with item)
        (join itemcategory (= :item.itemcategory_id :itemcategory.id))
        (where {:facilityorderitem.facilityorder_id id})
        (fields [:itemcategory.id :id] [:itemcategory.name :name])
        (modifier "DISTINCT")))

(defn get-all-categories-by-issue-id
  [id]
(select facilityissueitem
        (with facilityissue)
        (with item)
        (join itemcategory (= :item.itemcategory_id :itemcategory.id))
        (where {:facilityissueitem.facilityissue_id id})
        (fields [:itemcategory.id :id] [:itemcategory.name :name])
        (modifier "DISTINCT")))