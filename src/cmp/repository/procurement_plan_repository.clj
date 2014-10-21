(ns cmp.repository.procurement-plan-repository
  (:use korma.db)
  (:use korma.core)
  (:use cmp.models)
  (:require [clojure.string :as str]
            [cmp.util :as util]
            [cmp.repository.facility-repository :as facility-repository]
            [cmp.repository.item-repository :as item-repository]
            [taoensso.timbre :as logger]))

(defn find-match
  [x col key]
  (let [ x1 (if (number? x)
              (str (int x))
              x)
        matches (filter #(= x1 (% key)) col)]
    (if (empty? matches)
      (do
        (logger/info (str "No item with code: " x))
        (throw (Exception. (str "No item with code: " x))))
      (first matches))))

;; Get all procurement plans from mysql database
(defn get-all-procurement-plans
  []
 (select procurementplan
        (with facility)
        (join level (= :facility.level_id :level.id))
        (fields :id :year :totalcredit :totalcreditused
                [:facility.name :facilityname]
                [:level.name :facilitylevel])))

(defn get-plan-details
  [id]
  (select procurementplan
          (fields :id :year :totalcredit :totalcreditused :monthlycredit
                [:facility.name :facilityname])
          (with facility)
          (where {:id [= id]})))

(defn get-plan-details-by-code
  [code]
  (first (select procurementplan
          (fields :id :year :datecreated)
          (with facility)
          (where {:facility.code [= code]}))))

(defn get-procurement-plan-details
  [id]
  (select procurementplanitem
          (fields :id :unitsize :unitcost :plannedquantity
                  :totalcost
                  [:item.code :itemcode]
                  [:item.name :itemname]
                  [:procurementplan.totalcredit :totalcredit]
                  [:itemcategory.name :categoryname])
          (with procurementplan)
          (with item)
          (join itemcategory (= :item.itemcategory_id :itemcategory.id))
          (where {:procurementplan.id [= id]})
          (order :categoryname :ASC)))

(defn get-procurement-plan-details-by-facility-code
  [code]
  (select procurementplanitem
          (fields :id :unitsize :unitcost :plannedquantity
                  :totalcost
                  [:item.code :itemcode]
                  [:item.name :itemname]
                  [:procurementplan.totalcredit :totalcredit]
                  [:itemcategory.name :categoryname])
          (with procurementplan)
          (with item)
          (join itemcategory (= :item.itemcategory_id :itemcategory.id))
          (join facility (= :procurementplan.facility_id :facility.id))
          (where {:facility.code [= code]})
          (order :categoryname :ASC)))

(defn get-items-by-category
  [procplanid catid]
  (select procurementplanitem
          (fields  [:item.itemcategory_id :categoryid]
                   [:item.id :itemid]
                   [:item.code :code]
                   [:item.name :itemname]
                   [:unitsize :unit]
                   [:unitcost :price]
                   [:plannedquantity :quantity] 
                   [:totalcost :total])
          (with item)
          (with procurementplan)
          (where {:item.itemcategory_id [= catid]
                  :procurementplan.id [= procplanid]})))

(defn imported-before? 
  [facility-id year] 
    (select procurementplan
             (fields :id)
             (where {:year year
                     :facility_id facility-id})))

(defn delete-procurement-plan-with-details
  [id]
  (println id)
  (delete procurementplanitem
          (where {:procurementplan_id id}))
  (delete procurementplan
          (where {:id id})))

;; This removes the dulicate ones as well
(defn add-procurement-plan
  "Add procurement plan and return the Id of the inserted row"
  [facility-id year totalcredit]
  (doseq [proc-id (imported-before? facility-id year)]
    (delete-procurement-plan-with-details (:id proc-id)))
  
  (insert procurementplan (values {:facility_id facility-id :year year
                                   :totalcredit totalcredit
                                   :monthlycredit 0
                                   :totalcreditused 0
                                   :datecreated (util/get-sql-date)
                                   :datemodified (util/get-sql-datetime)})))

(defn add-procurement-plan-item
  [items facilityid procurementplanid itemcode averagequantity
   plannedquantity unitsize unitcost totalcost]
  (let [matcheditem (find-match itemcode items :code)]
    (insert procurementplanitem (values (hash-map :unitsize unitsize :unitcost unitcost :procurementplan_id procurementplanid
                                                :facility_id facilityid :averagequantity (cond (nil? averagequantity) 0 :else averagequantity)
                                                :plannedquantity (cond (nil? plannedquantity) 0 :else plannedquantity) 
                                                :totalcost totalcost :item_id (matcheditem :id)
                                                :datecreated (util/get-sql-date) :datemodified (util/get-sql-datetime))))))

(defn add-procurement-plan-with-details-raw 
  [facilitycode totalcredit year data] 
  (let [facilityid ((facility-repository/get-facility-by-code facilitycode) :id )
        procurementplanid (:GENERATED_KEY (add-procurement-plan facilityid year totalcredit))
        items (item-repository/get-all-items)]
    
    (doseq  [ {code :code description :description unitsize :unitsize
              unitcost :unitcost averagequantity :averagequantity 
              plannedquantity :plannedquantity totalcost :totalcost} data]
      
      (add-procurement-plan-item items facilityid procurementplanid code averagequantity
                                 plannedquantity unitsize unitcost totalcost))))

(defn add-procurement-plan-with-details
   [
    {facilitycode :facility-code
    totalcredit :totalcredit 
    year :year 
    data :data}]
   
   (transaction
     (try
       (add-procurement-plan-with-details-raw facilitycode totalcredit year data)
        (catch Exception e 
         (do 
           (logger/info (str "Error:" (.getMessage e)))
           (rollback)
           (throw (Exception. (.getMessage e))))))))


;; TODO: Delete this method
;; This works for all procurement plans
(defn add-procurement-plan-with-details1
  [{facility-code :facility-code
    totalcredit :totalcredit 
    year :year 
    data :data}]
  (let [facility-id ((facility-repository/get-facility-by-code facility-code) :id )
        procurement-plan-id (:GENERATED_KEY (add-procurement-plan facility-id year totalcredit ))
        ;; get all items
        items (item-repository/get-all-items)
        procurement-plan-items (hash-map)]
    
    (doseq [ {code :code description :description unitsize :unitsize
              unitcost :unitcost averagequantity :averagequantity 
              plannedquantity :plannedquantity totalcost :totalcost} data]
         (try (insert procurementplanitem (values (hash-map :unitsize unitsize :unitcost unitcost :procurementplan_id procurement-plan-id
                                                :facility_id facility-id :averagequantity (cond (nil? averagequantity) 0 :else averagequantity)
                                                :plannedquantity (cond (nil? plannedquantity) 0 :else plannedquantity) 
                                                :totalcost totalcost :item_id (:id (first (filter #(= (first (str/split code #"\.")) (% :code)) items)))
                                                :datecreated (util/get-sql-date) :datemodified (util/get-sql-datetime))))
           (catch Exception e (logger/info (str "Error inserting" code)))))))



