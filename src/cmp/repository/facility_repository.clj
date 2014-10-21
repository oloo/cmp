(ns cmp.repository.facility-repository
  (:use korma.db)
  (:use korma.core)
  (:use cmp.models)
  (:require [clojure.string :as str]
            [cmp.util :as util]
            [cmp.sql-util :as sql-util]
            [cmp.repository.orderdeliveryschedule-repository :as orderdeliveryschedule-repository]))


;; Get all usernames from mysql database
(defn get-all-facilities-actual
  []
(select facility
        (with district)
        (with level)
        (fields :name :code [:district.name :districtname] 
                [:level.name :levelname])))

(defn get-facility-by-code
  "Modified to also check for those which had LC at the end but not ED"
  [code]
  (let [facilities (select facility
          (fields :id :code :name)
          (where (like :code code)))]
    (if (> (count facilities) 0)
      (first facilities)
      (if (.endsWith code "ED")
          (get-facility-by-code (str/replace code "ED" ""))
         (throw (Exception. (str "No facility with code: " code)))))))

;; Get all facilities from mysql database
(defn get-all-facilities
  []
(select facility
        (with level)
        (fields :id :name :code :zone :budgetceiling
                [:district_name :districtname]
                [:level.name :levelname])))

(defn get-number-of-facilities
  ([]
    (:number-of-facilities
      (first
        (select facility
          (aggregate (count :*) :number-of-facilities)))))
 
  ([levels]
  (:number-of-facilities
    (first
      (select facility
          (aggregate (count :*) :number-of-facilities)
          (where {:level_id [in levels]}))))))

;; Get all facilities by level id
(defn get-facilities-by-level-id
  [levelid]
(select facility
        (with level)
        (where {:level_id [= levelid]})
        (fields :id :name :code :zone :budgetceiling
                [:district_name :districtname]
                [:level.name :levelname])))

;;Get facility data with related number of plans, orders and issues
(defn get-facilities-with-proc-order-issue-number
  []
  (let [facilities (select facility
                           (with level)
                           (fields :id :name :zone :code [:district_name :districtname] 
                [:level.name :levelname]))        
        procplans (select procurementplan
                          (fields :id [:facility_id :facilityid]))        
        facilityorders (select facilityorder
                               (fields :id [:facility_id :facilityid]))        
        facilityissues (select facilityissue
                               (fields :id [:facility_id :facilityid]))]
    
    (into [] 
          (for [facility facilities]
            (hash-map :id (:id facility)
                      :name (:name facility)
                      :zone (:zone facility)
                      :code (:code facility)
                      :districtname (:districtname facility)
                      :levelname (:levelname facility)
                     :numberofplans (count (filter (fn [x] (= (:id facility)
                                                              (:facilityid x)))  procplans))
                     :numberoforders (count (filter (fn [x] (= (:id facility)
                                                              (:facilityid x)))  facilityorders))
                     :numberofissues (count (filter (fn [x] (= (:id facility)
                                                              (:facilityid x)))  facilityissues)))
      ))))

(defn get-facility-by-proc-plan-id 
  [proc-plan-id]
  (first (select procurementplan
          (with facility)
          (fields :id [:facility.name :facility-name] [:facility.code :facility-code])
          (where {:id proc-plan-id}))))

(defn get-all-facility-report-data
 ([]
  (let [financial-year (util/get-current-financial-year)]
    (select procurementplan
            (with facility)
            (join level (= :facility.level_id :level.id))
            (where { :year [= financial-year]})
            (fields [:facility.id :id] [:facility.name :name]
                    [:facility.code :code] [:level.name :levelname]
                    :totalcredit))))
  
  ([facilitylevel year]
    (select procurementplan
            (with facility)
            (join level (= :facility.level_id :level.id))
            (where (sql-util/where-wrapper {:year [= year]
                                            :facility.level_id [= (java.lang.Integer/parseInt facilitylevel)]}))
            (fields [:facility.id :id] [:facility.name :name]
                    [:facility.code :code] [:level.name :levelname]
                    :totalcredit))))
  
 (defn get-facility-report-data
    ([facility-code]
    (let [{facility-id :id name :name} (get-facility-by-code facility-code)]
     
      (hash-map :procurementplandata (select procurementplanitem 
              (with procurementplan)
              (join item (= :procurementplanitem.item_id :item.id))
              (where {:procurementplanitem.facility_id facility-id})
              (fields :id
                      [:procurementplanitem.item_id :item-id]
                      [:procurementplanitem.facility_id :facility-id])
              (aggregate (sum :plannedquantity) :numberplanned :procurementplanitem.item_id)
              (order :item.id))
               
               :orderdata (select facilityorderitem 
              (with facilityorder)
              (join item (= :facilityorderitem.item_id :item.id))
              (where {:facilityorderitem.facility_id facility-id})
              (fields :id 
                      [:facilityorderitem.item_id :item-id]
                      [:facilityorderitem.facility_id :facility-id])
              (aggregate (sum :orderquantity) :numberordered :facilityorderitem.item_id)
              (order :item.id))
               
               :issuedata (select facilityissueitem 
              (with facilityissue)
              (join item (= :facilityissueitem.item_id :item.id))
              (where {:facilityissueitem.facility_id facility-id})
              (fields :id 
                      [:facilityissueitem.item_id :item-id]
                      [:facilityissueitem.facility_id :facility-id])
              (aggregate (sum :issuequantity) :numberissued :facilityissueitem.item_id)
              (order :item.id)))))
    
   ([facility-code order-cycle financial-year]
    (let [{facility-id :id name :name} (get-facility-by-code facility-code)
          start-end-dates-financial-year (util/get-financial-year-start-end-dates financial-year)
          latest-order-delivery-schedule
          (if order-cycle
              (orderdeliveryschedule-repository/get-last-orderdeliveryschedule-by-cycle 
                 financial-year 5 order-cycle)             
              (orderdeliveryschedule-repository/get-last-orderdeliveryschedule-by-date-before 
                 financial-year 5 (util/get-sql-date)))     
          number-of-orders (if (nil? (:cycle latest-order-delivery-schedule))
                             0 (:cycle latest-order-delivery-schedule))]
     
      (hash-map :procurementplandata (select procurementplanitem 
              (with procurementplan)
              (join item (= :procurementplanitem.item_id :item.id))
              (where (sql-util/where-wrapper {:procurementplanitem.facility_id [= facility-id]
                                              :procurementplan.year [= financial-year]}))
              (fields :id
                      [:procurementplanitem.item_id :item-id]
                      [:procurementplanitem.facility_id :facility-id])
              (aggregate (sum :plannedquantity) :numberplanned :procurementplanitem.item_id)
              (order :item.id))
               
               :orderdata (select facilityorderitem 
              (with facility)
              (with facilityorder)
              (join item (= :facilityorderitem.item_id :item.id))
              (where (sql-util/where-wrapper {:facilityorderitem.facility_id [= facility-id]
                      :facilityorder.orderdate [between (:startdate start-end-dates-financial-year)
                                                        (:enddate start-end-dates-financial-year)]}))
              (fields :id 
                      [:facilityorderitem.item_id :item-id]
                      [:facilityorderitem.facility_id :facility-id])
              (aggregate (sum :orderquantity) :numberordered :facilityorderitem.item_id)
              (order :item.id))
               
               :issuedata (select facilityissueitem 
              (with facilityissue)
              (join item (= :facilityissueitem.item_id :item.id))
              (where (sql-util/where-wrapper {:facilityissueitem.facility_id [= facility-id]
                      :facilityissue.issuedate [between (:startdate start-end-dates-financial-year)
                                                        (:deliverydeadline latest-order-delivery-schedule)]}))
              (fields :id 
                      [:facilityissueitem.item_id :item-id]
                      [:facilityissueitem.facility_id :facility-id])
              (aggregate (sum :issuequantity) :numberissued :facilityissueitem.item_id)
              (order :item.id))))) )

(defn edit-facility
  [id name code levelid]
    (update facility
    (set-fields {:name name :code code :level_id levelid
           :datemodified (util/get-sql-datetime)})
    (where {:id id})))

(defn get-proc-order-issue-ids
  [code] 
  (let [{id :id name :name budget-ceiling :budgetceiling code :code} (first
                                         (select facility
                                                 (fields :id :name :code :budgetceiling)
                                                 (where {:code code})))
        {proc-plan-id :id} (first (select procurementplan
                             (fields :id)
                             (where {:facility_id id})
                             (order :id :DESC)))
        {order-id :id} (first (select facilityorder
                             (fields :id)
                             (where {:facility_id id})
                             (order :id :DESC)))
        {issue-id :id} (first (select facilityissue
                             (fields :id)
                             (where {:facility_id id})
                             (order :id :DESC)))]
   (hash-map :id id :name name :code code :budget-ceiling budget-ceiling :proc-plan-id proc-plan-id :order-id order-id :issue-id issue-id) ))

(defn get-data-import-report-data
  []
  (let [orderdeliveryschedules (orderdeliveryschedule-repository/get-orderdeliveryschedules)
        facilitys (get-all-facilities)
        procurementplans (select procurementplan
                                 (fields :id :facility_id :year))
        facilityorders (select facilityorder 
                               (fields :id :facility_id :cycle :orderdate))
        ;TODO delete this method after testing that it works without it
        map-import-basing-on-schedule (fn [schedules col facilityid default-value zone year cycle]                                      
                                        (let [matchedschedule (orderdeliveryschedule-repository/get-matched-orderdeliveryschedule
                                                                schedules year zone cycle)
                                              ]
                                          (util/filter-map-and-return-first 
                                            (fn [xs1] (and
                                                       (= facilityid (:facility_id xs1))
                                                       ;; check if order date is utmost 2 weeks beofre order deadline
                                                       (util/sql-date-is-after?  (:orderdate xs1) (:orderdeadline matchedschedule) 0 -14)
                                                       ;; Check if delivery deadline is after order deadline
                                                       (util/sql-date-is-after? (:deliverydeadline matchedschedule) (:orderdate xs1) 1 0)
                                                       ))
                                            col default-value)))
        
        map-import-basing-on-cycle (fn [col facilityid cycle]
                                     (util/filter-map-and-return-first 
                                            (fn [xs1] (and
                                                       (= facilityid (:facility_id xs1))
                                                       (= cycle (:cycle xs1))
                                                       ))
                                            col nil))
      ]
    
    (for [{code :code facilitylevel :levelname facility-id :id name :name zone :zone} facilitys]
      (hash-map :id facility-id
                :code code
                :name name
                :facilitylevel facilitylevel
                :zone zone
                ;; TODO: Remove hard coded year from here 2013/14
                ;; Mising data will hrow an error say, when year turns to 2014/15
                :hasprocplan (not (nil? (util/filter-map-and-return-first (fn [xs] (and (= "2013/14" (:year xs))
                                                                           (= facility-id (:facility_id xs))))
                                                               procurementplans nil)))
                
                :hasordercycle1 (not (nil? (map-import-basing-on-cycle facilityorders facility-id 1)))
                
                :hasordercycle2 (not (nil? (map-import-basing-on-cycle facilityorders facility-id 2)))
                
                :hasordercycle3 (not (nil? (map-import-basing-on-cycle facilityorders facility-id 3)))
                
                :hasordercycle4 (not (nil? (map-import-basing-on-cycle facilityorders facility-id 4)))
                
                :hasordercycle5 (not (nil? (map-import-basing-on-cycle facilityorders facility-id 5)))
                
                :hasordercycle6 (not (nil? (map-import-basing-on-cycle facilityorders facility-id 6)))))))


;; Add a new facility
;; Note the typo in ccrregion
(defn add-new-facility
  [name code ccregion zone levelid 
   districtname districtid]
    (insert facility
    (values {:name name :code code :ccrregion ccregion 
           :zone zone :level_id levelid
           :district_name districtname :district_id districtid
           :datecreated (util/get-sql-date)
           :datemodified (util/get-sql-datetime)})))

(defn get-facility-issues-and-plans
  []
  (select facility
          (join procurementplan (= :procurementplan.facility_id :id))
          (join facilityissue (= :facilityissue.facility_id :id))
          (fields :name [:procurementplan.totalcredit :totalcredit]
                  [:facilityissue.totalcost :totalcost])
          (modifier "DISTINCT")
          (where {:totalcredit [not= nil]
                 :totalcost [not= nil]})
          ))