(ns cmp.repository.facility-issue-repository
  (:use korma.db)
  (:use korma.core)
  (:use cmp.models)
  (:require [clojure.string :as str]
            [cmp.util :as util]
            [cmp.repository.facility-repository :as facility-repository]
             [cmp.repository.item-repository :as item-repository]))

(defn find-match
  [col x key]
  (let [matches (filter #(= x (% key)) col)]
    (if (empty? matches)
      (do
        (taoensso.timbre/info (str "No item with code: " x))
        (throw (Exception. (str "No item with code: " x))))
      (first matches))))


(defn compute-issue-total  
  [data]
   (reduce #'+ (for  [{code :itemcode quantity :quantity unitsize :unitsize
              unitcost :unitcost issuequantity :issuequantity
              issuecost :issuecost} data] 
                 (java.lang.Double/parseDouble issuecost))))

;; Get all facility orders
(defn get-all-facility-issues
  []
(select facilityissue
        (with facility)
        (join level (= :facility.level_id :level.id))
        (fields :id :issuedate :totalcost [:facility.name :facilityname]
                [:level.name :facilitylevel])))

(defn get-single-facility-issue
  [id]
  (select facilityissue
          (fields :id :issuedate :totalcost [:facility.name :facilityname])
          (with facility)
          (where {:id [= id]})))

(defn get-last-issue
  [code]
  (first (select facilityissue
          (fields :id :issuedate)
          (with facility)
          (where {:facility.code [= code]})
          (order :issuedate :DESC))))

(defn get-facility-issue-details
  [id]
  (select facilityissueitem
          (fields :id :unitsize :unitcost :issuequantity
                  :issuecost
                  [:item.code :itemcode]
                  [:item.name :itemname]
                  [:facilityissue.totalcost :totalcost]
                  [:itemcategory.name :categoryname])
          (with facilityissue)
          (with item)
           (join itemcategory (= :item.itemcategory_id :itemcategory.id))
          (where {:facilityissue.id [= id]})
          (order :categoryname :ASC)))

(defn get-facility-issue-details-by-facility-code
  [code]
  (select facilityissueitem
          (fields :id :unitsize :unitcost 
                  :issuequantity
                  :issuecost
                  [:item.code :itemcode]
                  [:item.name :itemname]
                  [:facilityissue.totalcost :totalcost]
                  [:itemcategory.name :categoryname])
          (with facilityissue)
          (with item)
           (join itemcategory (= :item.itemcategory_id :itemcategory.id))
           (join facility (= :facilityissue.facility_id :facility.id))
          (where {:facility.code [= code]})
          (order :categoryname :ASC)))

(defn get-facility-issue-details-by-facility-code-grouped
  [code]
  (select facilityissueitem
          (fields :id :unitsize :unitcost 
                  ;:issuequantity
                  :issuecost
                  [:item.code :itemcode]
                  [:item.name :itemname]
                ;  [:facilityissue.totalcost :totalcost]
                  [:itemcategory.name :categoryname])
               (aggregate (sum :issuequantity) :issuequantity :item_id)
                  (aggregate (sum :facilityissue.totalcost) :totalcost)
          (with facilityissue)
          (with item)
           (join itemcategory (= :item.itemcategory_id :itemcategory.id))
           (join facility (= :facilityissue.facility_id :facility.id))
          (where {:facility.code [= code]})
          (order :categoryname :ASC)))

(defn get-items-by-category
  [issueid catid]
  (select facilityissueitem
          (fields  [:item.itemcategory_id :categoryid]
                   [:item.id :itemid]
                   [:item.code :code]
                   [:item.name :itemname]
                   [:unitsize :unit]
                   [:unitcost :price]
                   [:issuequantity :quantity] 
                   [:issuecost :total])
          (with item)
          (with facilityissue)
          (where {:item.itemcategory_id [= catid]
                  :facilityissue.id [= issueid]})))

(defn entered-today?
  [issue]
  (if (or (nil? issue)
          (empty? issue))
    false
    (if (= (util/get-sql-date) 
             (.toString (:datecreated issue)))
      true
      false)))

(defn check-if-issues-exist
  [facility-id issue-date]
  
  (let [issues (select facilityissue
                       (with facility)
                       (fields :id :issuedate :totalcost :datecreated)
                       (where { :facilityissue.issuedate [=      
                                      (util/reformat-date issue-date true)
                                                          ]
                               :facilityissue.facility_id [= facility-id]}))]
    (if (empty? issues)
      nil
      (first issues))))

(defn check-if-issue-item-exists
  [facility-issue-id order-id item-id]
  (let [matched-issue-item (select facilityissueitem
                                  (fields :id)
                                  (where {:facilityissue_id facility-issue-id
                                          :item_id item-id 
                                          :orderid order-id}))]
    (if (empty? matched-issue-item)
      nil
      (first matched-issue-item))))

(defn update-facility-issue
  "Update facility issue. Update data should be a map
   with keys matching table columns and vals with matching datatypes
   the respective columns e.g. {:totalcost 30000}"
  [facility-id update-data]
  (update facilityissue
    (set-fields update-data)
    (where {:facilityissue.facility_id [= facility-id]})))

(defn add-facility-issue
  "Add facility issue and return the Id of the inserted row"
  [facility-id issue-date expense]
  (insert facilityissue (values {:facility_id facility-id
                                   :totalcost expense
                                   :issuedate issue-date
                                   :datecreated (util/get-sql-date)
                                   :datemodified (util/get-sql-datetime)})))

(defn add-facility-issue-item
  [facility-id facility-issue-id item-id data]
  (let [{code :itemcode quantity :quantity unitsize :unitsize
         orderid :orderid unitcost :unitcost issuequantity :issuequantity
         issuecost :issuecost salesperson :salesperson} data]
    
    (try 
      (insert facilityissueitem (values (hash-map :unitsize (java.lang.Integer/parseInt unitsize) :unitcost (java.lang.Double/parseDouble unitcost)
                                                     :facilityissue_id facility-issue-id :orderid (java.lang.Integer/parseInt orderid)
                                                     :facility_id facility-id :salesperson salesperson
                                                     :issuequantity (cond (nil? (java.lang.Double/parseDouble issuequantity)) 0 
                                                                      :else (java.lang.Double/parseDouble issuequantity))                                                 
                                                     :issuecost (java.lang.Double/parseDouble issuecost) :item_id item-id
                                                     :datecreated (util/get-sql-date) :datemodified (util/get-sql-datetime))))
          (catch Exception e (taoensso.timbre/info (str " Error inserting " code " raw code " (Math/round code) 
                                                        "for facility " facility-id))))))

(defn update-or-add-facility-issue-item
  [facility-id facility-issue-id items data]
  (let [item-id (:id (find-match items (:itemcode data) :code))
        order-id (:orderid data)
        matched-issue-item (check-if-issue-item-exists facility-issue-id order-id item-id)]    
    (if (nil? matched-issue-item)   
      (add-facility-issue-item facility-id facility-issue-id item-id data)
      (taoensso.timbre/info (str " Item " item-id " already added for facility id " facility-id)))))

(defn update-or-add-facility-issue
  "Selects whether to update and 
   return existing facilityissue id to update or 
   generates a facilityIssue and returns Id"
  [facility-id issue-date matched-issue issues-data]
  (if (entered-today? matched-issue)
         (do
            (update-facility-issue 
                facility-id
                {:datemodified (cmp.util/get-sql-datetime)
                 :totalcost (+ (:totalcost matched-issue)
                               (compute-issue-total issues-data))})
              (:id matched-issue))
            (:GENERATED_KEY (add-facility-issue facility-id (util/reformat-date issue-date true)
                                               (compute-issue-total issues-data)))))

(defn add-facility-issue-with-details-raw
  [facility-code issue-date data]
   (let [;; split to remove LC appended to some facilities and replace with ED
        facility-code-changed  (str/replace facility-code "LC" "ED")
        facility-id ((facility-repository/get-facility-by-code                   
                       facility-code-changed) :id )
        matched-issue (check-if-issues-exist facility-id issue-date)  
        ;; This means that this should not be left to run through 2 days (better run after midnight)
        facility-issue-id (update-or-add-facility-issue facility-id issue-date matched-issue data)
        items (item-repository/get-all-items)]
    
    (doseq [ data-item data]
      (update-or-add-facility-issue-item facility-id facility-issue-id items data-item))))

(defn add-facility-issue-with-details
  [facility-code data]
 (transaction
  (try
    (if (empty? data) nil
        (add-facility-issue-with-details-raw facility-code (:issuedate (first data)) data))
  (catch Exception e
    (do
      (rollback)
      (taoensso.timbre/info (str " Error inserting data for facility " facility-code " Details: " 
                                 (.getMessage e))))))))
