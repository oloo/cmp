(ns cmp.repository.facility-order-repository
  (:use korma.db)
  (:use korma.core)
  (:use cmp.models)
  (:require [clojure.string :as str]
            [cmp.util :as util]
            [cmp.repository.facility-repository :as facility-repository]
             [cmp.repository.item-repository :as item-repository]))

(defn find-match
  [x col key]
  (let [ x1 (if (number? x)
              (str (int x))
              x)
        
        matches (filter #(= (str/trim x1) (% key)) col)]
    (if (empty? matches)
      (do
        (taoensso.timbre/info (str "No item with code: " x))
        (throw (Exception. (str "No item with code: " x))))
      (first matches))))

;; Get all facility orders
(defn get-all-facility-orders
  []
(select facilityorder
        (with facility)
        (join level (= :facility.level_id :level.id))
        (fields :id :orderdate :totalcost :cycle
                [:level.name :facilitylevel]  [:facility.name :facilityname])))

(defn get-single-facility-order
  [id]
  (select facilityorder
          (fields :id :orderdate :totalcost [:facility.name :facilityname])
          (with facility)
          (where {:id [= id]})))

(defn get-single-facility-order-dates
  [code]
  (select facilityorder
          (fields :id :orderdate)
          (with facility)
          (where {:facility.code [= code]})))

(defn get-facility-order-details
  [id]
  (select facilityorderitem
          (fields :id :unitsize :unitcost :orderquantity
                  :ordercost
                  [:item.code :itemcode]
                  [:item.name :itemname]
                  [:facilityorder.totalcost :totalcost]
                  [:itemcategory.name :categoryname])
          (with facilityorder)
          (with item)
          (join itemcategory (= :item.itemcategory_id :itemcategory.id))
          (where {:facilityorder.id [= id]})
          (order :categoryname :ASC)))

(defn get-number-of-orders
  [code]
  (count (select facilityorder
          (fields :id)
          (with facility)
          (where {:facility.code [= code]}))))

(defn get-facility-order-details-by-facility
  "This returns a cumulative version for all orders for that
   facility"
  [facilityid]
  (select facilityorderitem
          (fields :id :unitsize :unitcost :orderquantity
                  :ordercost
                  [:item.code :itemcode]
                  [:item.name :itemname]
                  [:facilityorder.totalcost :totalcost]
                  [:itemcategory.name :categoryname])
          (with facilityorder)
          (with item)
          (join itemcategory (= :item.itemcategory_id :itemcategory.id))
          (where {:facilityorder.id [= facilityid]})
          (order :categoryname :ASC)))

(defn get-facility-order-details-by-facility-code
  [code]
  (select facilityorderitem
          (fields :id :unitsize :unitcost :orderquantity
                  :ordercost
                  [:item.code :itemcode]
                  [:item.name :itemname]
                  [:facilityorder.totalcost :totalcost]
                  [:itemcategory.name :categoryname])
          (with facilityorder)
          (with item)
          (join itemcategory (= :item.itemcategory_id :itemcategory.id))
          (join facility (= :facilityorder.facility_id :facility.id))
          (where {:facility.code [= code]})
          (order :categoryname :ASC)))

(defn get-number-of-orders-grouped-by-cycle
  []
  (select facilityorder
          (fields [:cycle :order-cycle])
           (aggregate (count :id) :number-of-orders :cycle)))

(defn get-facility-order-details-by-facility-code-grouped
  [code]
  (select facilityorderitem
          (fields 
                  :ordercost
                  [:item.code :itemcode]
                  [:item.name :itemname]
                  [:facilityorder.totalcost :totalcost]
                  [:itemcategory.name :categoryname])
                  (aggregate (sum :orderquantity) :orderquantity :item_id)
                  ;(aggregate (sum :facilityorder.totalcost) :facilitytotalcost :facilityorder_id)
          (with facilityorder)
          (with item)
          (join itemcategory (= :item.itemcategory_id :itemcategory.id))
          (join facility (= :facilityorder.facility_id :facility.id))
          (where {:facility.code [= code]})
          (order :categoryname :ASC)))

(defn get-items-by-category
  [orderid catid]
  (select facilityorderitem
          (fields  [:item.itemcategory_id :categoryid]
                   [:item.id :itemid]
                   [:item.code :code]
                   [:item.name :itemname]
                   [:unitsize :unit]
                   [:unitcost :price]
                   [:orderquantity :quantity] 
                   [:ordercost :total])
          (with item)
          (with facilityorder)
          (where {:item.itemcategory_id [= catid]
                  :facilityorder.id [= orderid]})))

(defn check-if-order-imported
  [facility-id cycle]
  (let [orders (select facilityorder
          (fields :id)
          (where 
            {:facility_id [= facility-id]
             :cycle [= cycle]}))]
    (not (empty? orders))))

(defn add-facility-order
  "Add facility order and return the Id of the inserted row"
  [facility-id cycle order-date expense]
  (if (not (check-if-order-imported facility-id cycle))
    (insert facilityorder (values {:facility_id facility-id
                                 :cycle cycle
                                   :totalcost expense
                                  ; :orderdate (cmp.util/reformat-date order-date)
                                   :orderdate order-date
                                   :datecreated (util/get-sql-date)
                                   :datemodified (util/get-sql-datetime)}))
    (throw (Exception. "Facility order already imported" ))))

(defn add-facility-order-item
  [items facilityid facilityorderid itemcode ordercost orderquantity cycle unitsize unitcost]
  (let [matcheditem (find-match itemcode items :code)]
    (insert facilityorderitem (values (hash-map :unitsize (if (nil? unitsize) 0 unitsize)
                                                :unitcost unitcost :facilityorder_id facilityorderid
                                                :facility_id facilityid :orderquantity (cond (nil? orderquantity) 0 :else orderquantity)                                                 
                                                :ordercost ordercost :item_id (matcheditem :id)
                                                :datecreated (util/get-sql-date) :datemodified (util/get-sql-datetime))))))

(defn add-facility-order-with-details-raw 
  [facility-code cycle order-date expense data] 
  (let [facilityid ((facility-repository/get-facility-by-code facility-code) :id )
        facilityorderid (:GENERATED_KEY (add-facility-order facilityid cycle order-date expense))
        items (item-repository/get-all-items)]
    
    (doseq  [ {code :code description :description unitsize :unitsize
              unitcost :unitcost orderquantity :orderquantity
              cycle :cycle ordercost :ordercost} data]
      
      (add-facility-order-item items facilityid facilityorderid code ordercost orderquantity cycle unitsize unitcost))))

(defn add-facility-order-with-details
   [
    {facility-code :facility-code
     cycle :cycle
    order-date :order-date
    expense :expense
    data :data}]
   
   (transaction
     (try
       ;TODO Remove teh hard coded 2!!
       (add-facility-order-with-details-raw facility-code 2 order-date expense data)
        (catch Exception e 
         (do 
           (taoensso.timbre/info (str "Error:" (.getMessage e)))
           (rollback)
           (throw (Exception. (.getMessage e))))))))

;; This will work for all facilities
(defn add-facility-order-with-details1
  [{facility-code :facility-code
    order-date :order-date
    expense :expense
    data :data}]
  (let [facility-id ((facility-repository/get-facility-by-code facility-code) :id )
        facility-order-id (:GENERATED_KEY (add-facility-order facility-id order-date expense))
        ;; get all items
        items (item-repository/get-all-items)
        facility-order-items (hash-map)]
    
   (if (nil? facility-id)
     (throw (Exception. (str "Facility Code " facility-code " does not exist in the system")))
     (doseq [ {code :code description :description unitsize :unitsize
              unitcost :unitcost orderquantity :orderquantity
              ordercost :ordercost} data]
         (try (insert facilityorderitem (values (hash-map :unitsize unitsize :unitcost unitcost :facilityorder_id facility-order-id
                                                :facility_id facility-id :orderquantity (cond (nil? orderquantity) 0 :else orderquantity)                                                 
                                                :ordercost ordercost :item_id (:id (first (filter #(= (first (clojure.string/split code #"\.")) (% :code)) items)))
                                                :datecreated (util/get-sql-date) :datemodified (util/get-sql-datetime))))
           (catch Exception e (taoensso.timbre/info (str "Error inserting" code))))))))

(defn delete-facility-order
  [orderid]
  (delete facilityorderitem
          (where {:facilityorder_id [= orderid]}))
  (delete facilityorder
          (where {:id [= orderid]})))
