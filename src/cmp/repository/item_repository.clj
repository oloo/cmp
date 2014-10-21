(ns cmp.repository.item-repository
  (:use korma.db)
  (:use korma.core)
  (:use cmp.models)
  (:require [clojure.string :as str]
            [cmp.util :as util]
            [cmp.sql-util :as sql-util]
            [cmp.repository.orderdeliveryschedule-repository :as orderdeliveryschedule-repository]))


;; Get all usernames from mysql database
(defn get-all-items
  ([]
  (select item
        (with itemcategory)
        (with itemtype)
        (fields :id :code :name [:itemcategory.name :categoryname] 
                [:itemtype.name :itemtypename]
                :packsize :unitcost)
        (order :id)))
  
  ; Order by option
   ([order-by-option]
     (select item
        (with itemcategory)
        (with itemtype)
        (fields :id :code :name [:itemcategory.name :categoryname] 
                [:itemtype.name :itemtypename]
                :packsize :unitcost)
        (order order-by-option))))


(defn get-item-by-code
  [code]
  (first (select item
        (fields :id :code :name
                :packsize :unitcost)
        (where {:code code}))))

;; Add a new item
;; TODO fix the item-category link, for now it links to first category, 2
(defn add-new-item
  [name code pack-size unitcost category type order-type item-source item-classification item-class item-account]
    (insert item
    (values {:name name :code code :packsize pack-size 
           :unitcost unitcost :itemcategory_id category :itemtype_id type
           :itemordertype_id order-type :itemsource_id item-source 
           :itemclassification_id item-classification
           :itemclass_id item-class :itemaccount_id item-account
           :datecreated (util/get-sql-date)
           :datemodified (util/get-sql-datetime)})))

;; Gets all item reports data for now
;;TODO Change this to allow filters, for now hard wired to the year 2013/2014
(defn get-item-report-data
  ([]    
    (let [items (get-all-items)          
          latest-order-delivery-schedule 
          (orderdeliveryschedule-repository/get-last-orderdeliveryschedule-by-date-before 
            (util/get-current-financial-year)
            5
            (util/get-sql-date))
          number-of-orders (:cycle latest-order-delivery-schedule)
          procplanitems (select procurementplanitem
                                (fields :item_id)
                                (aggregate (sum :plannedquantity) :numberplanned :item_id)
                                (order :item_id))
          facilityorderitems (select facilityorderitem
                                     (fields :item_id)
                                     (aggregate (sum :orderquantity) :numberordered :item_id)
                                     (order :item_id))
          facilityissueitems (select facilityissueitem
                                 (with facilityissue)
                                 (fields :item_id)
                                 (where {:facilityissue.issuedate [< (:deliverydeadline latest-order-delivery-schedule)]})
                                 (aggregate (sum :issuequantity) :numberissued :item_id)
                                 (order :item_id))]
      (into [] (for [{id :id code :code name :name} items]
                 (hash-map :id id :code code :name name 
                           :numberplanned (* number-of-orders (util/filter-map-and-return-first (fn [xs] (= (:item_id xs) id)) procplanitems :numberplanned 0))
                           :numberordered (util/filter-map-and-return-first (fn [xs] (= (:item_id xs) id)) facilityorderitems :numberordered 0)
                           :numberissued (util/filter-map-and-return-first (fn [xs] (= (:item_id xs) id)) facilityissueitems :numberissued 0)
                           )))))
  
  ([{year :year ordercycle :ordercycle
     level :level zone :zone}]
    
    (let [items (get-all-items)
          financial-year (if (nil? year)(util/get-current-financial-year) year)
          start-end-dates-financial-year (util/get-financial-year-start-end-dates financial-year)
          latest-order-delivery-schedule
          (if ordercycle
              (orderdeliveryschedule-repository/get-last-orderdeliveryschedule-by-cycle 
                 financial-year 5 ordercycle)             
              (orderdeliveryschedule-repository/get-last-orderdeliveryschedule-by-date-before 
                 financial-year 5 (util/get-sql-date)))     
          number-of-orders (if (nil? (:cycle latest-order-delivery-schedule))
                             0 (:cycle latest-order-delivery-schedule))
          procplanitems (select procurementplanitem
                                (fields :item_id)
                                (with procurementplan)
                                (aggregate (sum :plannedquantity) :numberplanned :item_id)
                                (where (sql-util/where-wrapper {:procurementplan.year [= financial-year]}))
                                (order :item_id))
          facilityorderitems (select facilityorderitem
                                     (with facility)
                                     (with facilityorder)
                                     (fields :item_id)
                                     (where (sql-util/where-wrapper {:facilityorder.cycle [= ordercycle]
                                                     :facility.level_id [= level]
                                                     :facilityorder.orderdate [between (:startdate start-end-dates-financial-year)
                                                                                       (:enddate start-end-dates-financial-year)]}))
                                     (aggregate (sum :orderquantity) :numberordered :item_id)
                                     (order :item_id))
          facilityissueitems (select facilityissueitem
                                 (with facilityissue)
                                 (with facility)
                                 (fields :item_id)
                                 (where 
                                   (sql-util/where-wrapper {:facility.level_id [= level]
                                                            :facilityissue.issuedate [between (:startdate start-end-dates-financial-year)
                                                                     (:deliverydeadline latest-order-delivery-schedule)]}))
                                 (aggregate (sum :issuequantity) :numberissued :item_id)
                                 (order :item_id))]
      (into [] (for [{id :id code :code name :name} items]
                 (hash-map :id id :code code :name name 
                           :numberplanned (* number-of-orders (util/filter-map-and-return-first (fn [xs] (= (:item_id xs) id)) procplanitems :numberplanned 0))
                           :numberordered (util/filter-map-and-return-first (fn [xs] (= (:item_id xs) id)) facilityorderitems :numberordered 0)
                           :numberissued (util/filter-map-and-return-first (fn [xs] (= (:item_id xs) id)) facilityissueitems :numberissued 0)
                           ))))))

(defn get-single-item-report-data
   ([item-code]
    (let [latest-order-delivery-schedule 
          (orderdeliveryschedule-repository/get-last-orderdeliveryschedule-by-date-before 
            (util/get-current-financial-year)
            5
            (util/get-sql-date))
          {item-id :id name :name} (get-item-by-code item-code)]
     
      (hash-map :procurementplandata (select procurementplanitem 
              (with procurementplan)
              (join facility (= :procurementplan.facility_id :facility.id))
              (where {:procurementplanitem.item_id item-id})
              (fields :id 
                      [:procurementplanitem.item_id :item-id]
                      [:facility.id :facility-id])
              (aggregate (sum :plannedquantity) :numberplanned :procurementplan.facility_id)
              (order :facility.id))
               
               :orderdata (select facilityorderitem 
              (with facilityorder)
              (join facility (= :facilityorder.facility_id :facility.id))
              (where {:facilityorderitem.item_id item-id})
              (fields :id 
                      [:facilityorderitem.item_id :item-id]
                      [:facility.id :facility-id])
              (aggregate (sum :orderquantity) :numberordered :facilityorder.facility_id)
              (order :facility.id))
               
               :issuedata (select facilityissueitem 
              (with facilityissue)
              (join facility (= :facilityissue.facility_id :facility.id))
              (where {:facilityissueitem.item_id item-id
                      :facilityissue.issuedate [< (:deliverydeadline latest-order-delivery-schedule)]})
              (fields :id 
                      [:facilityissueitem.item_id :item-id]
                      [:facility.id :facility-id])
              (aggregate (sum :issuequantity) :numberissued :facilityissue.facility_id)
              (order :facility.id)))))
  
   ([item-code ordercycle level year]     
     (let [financial-year (if (nil? year)(util/get-current-financial-year) year)
          start-end-dates-financial-year (util/get-financial-year-start-end-dates financial-year)
          latest-order-delivery-schedule
          (if ordercycle
              (orderdeliveryschedule-repository/get-last-orderdeliveryschedule-by-cycle 
                 financial-year 5 ordercycle)             
              (orderdeliveryschedule-repository/get-last-orderdeliveryschedule-by-date-before 
                 financial-year 5 (util/get-sql-date)))
          {item-id :id name :name} (get-item-by-code item-code)]
     
      (hash-map :procurementplandata (select procurementplanitem 
              (with procurementplan)
              (join facility (= :procurementplan.facility_id :facility.id))
              (where (sql-util/where-wrapper {:procurementplanitem.item_id [= item-id]
                                              :procurementplan.year [= financial-year]}))
              (fields :id 
                      [:procurementplanitem.item_id :item-id]
                      [:facility.id :facility-id])
              (aggregate (sum :plannedquantity) :numberplanned :procurementplan.facility_id)
              (order :facility.id))
               
               :orderdata (select facilityorderitem 
              (with facilityorder)
              (join facility (= :facilityorder.facility_id :facility.id))
              (where (sql-util/where-wrapper {:facilityorderitem.item_id [= item-id]
                                              :facilityorder.cycle [= ordercycle]
                                              :facility.level_id [= level]
                                              :facilityorder.orderdate 
                                                  [between (:startdate start-end-dates-financial-year)
                                                           (:enddate start-end-dates-financial-year)]}))
              (fields :id 
                      [:facilityorderitem.item_id :item-id]
                      [:facility.id :facility-id])
              (aggregate (sum :orderquantity) :numberordered :facilityorder.facility_id)
              (order :facility.id))
               
               :issuedata (select facilityissueitem 
              (with facilityissue)
              (join facility (= :facilityissue.facility_id :facility.id))
              (where (sql-util/where-wrapper {:facilityissueitem.item_id [= item-id]
                      :facility.level_id [= level]
                      :facilityissue.issuedate [between (:startdate start-end-dates-financial-year)
                                                        (:deliverydeadline latest-order-delivery-schedule)]}))
              (fields :id 
                      [:facilityissueitem.item_id :item-id]
                      [:facility.id :facility-id])
              (aggregate (sum :issuequantity) :numberissued :facilityissue.facility_id)
              (order :facility.id))))))
