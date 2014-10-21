(ns cmp.routes.reports
  (:import java.util.Date)
  (:use compojure.core)
  (:use clj-pdf.core)
  (:require noir.response)  
  (:require [cmp.views.layout :as layout]
            [cmp.util :as util]
            [cmp.services.item-reports :as item-reports]
            [cmp.services.facility-reports :as facility-reports]
            [cmp.repository.item-repository :as item-repository]
            [cmp.repository.facility-repository :as facility-repository]
            [cmp.repository.procurement-plan-repository :as procurement-plan-repository]
            [cmp.repository.facility-order-repository :as facility-order-repository]
            [cmp.repository.facility-issue-repository :as facility-issue-repository]
            [cmp.repository.orderdeliveryschedule-repository :as orderdeliveryschedule-repository]
            [cmp.excel-util :as excel-util]))

(defn generate-order-comment
  [proc-quantity order-quantity]
  (let [lower-limit (* 0.8 proc-quantity)
        upper-limit (* 1.2 proc-quantity)]
    (cond 
      (and (= 0 order-quantity) (< 0 proc-quantity)) "Ordered but not planned for"
      (and (= 0 proc-quantity) (< 0 order-quantity)) "Planned for but not ordered"
      (< order-quantity lower-limit) "Under Ordered"
      (> order-quantity upper-limit) "Over Ordered"
      (and (>= order-quantity lower-limit) (<= order-quantity upper-limit)) "Within limit"
      :else "No Comment")))

(defn get-matching-data
  "Search collection xs (vector of hashmaps) for x matching key (compare-key) in hashmap
 then extract appropriate key (lookup-key) value"
    ([xs x compare-key]
  (try (let [position (util/binary-search xs compare-key x)]
    (cond 
      (or (nil? position) (not (number? position))) 
         {}
     :else 
         (nth xs position)))
    (catch Exception e {})))
   
  ([xs x compare-key lookup-key]
  (let [position (util/binary-search xs compare-key x)]
    (cond 
      (nil? position) 0
      (not (number? position)) 0
      :else (lookup-key (nth xs position))))))

(defn match-proc-order-issue-data
  [proc-plan order issue numberoforders]
  
  (for  [{procitemcode :itemcode itemname :itemname  plannedquantity :plannedquantity
            unitsize :unitsize proclinecost :totalcost proccost :totalcredit  categoryname :categoryname} proc-plan
           
           {orderitemcode :itemcode orderquantity :orderquantity orderunitcost :unitcost
              orderlinecost :ordercost ordercost :totalcost} order
           
           {issueitemcode :itemcode issuequantity :issuequantity
              issuelinecost :issuecost issuecost :totalcost}  issue
           
           :when (and (= procitemcode orderitemcode)
                       (= procitemcode issueitemcode)
                        (= orderitemcode issueitemcode))] 
      
       (hash-map :procitemcode procitemcode :itemname itemname
                 :unitsize unitsize :plannedquantity (* numberoforders plannedquantity) ;This is multiplied by number of orders
                 :proclinecost proclinecost :proccost proccost :categoryname categoryname
                 :orderquantity orderquantity :orderlinecost (* orderunitcost orderquantity) :ordercost ordercost
                 :issuequantity issuequantity :issuelinecost issuelinecost :issuecost issuecost
                 :ordercomment (generate-order-comment (* numberoforders plannedquantity) orderquantity))))

(defn match-proc-order-issue-data1
  [proc-plan order issue]
  
  (for  [{procitemcode :itemcode itemname :itemname  plannedquantity :plannedquantity
            unitsize :unitsize proclinecost :totalcost proccost :totalcredit  categoryname :categoryname} proc-plan
           
           {orderitemcode :itemcode orderquantity :orderquantity orderunitcost :unitcost
              orderlinecost :ordercost ordercost :totalcost} order
           
           {issueitemcode :itemcode issuequantity :issuequantity
              issuelinecost :issuecost issuecost :totalcost}  issue
           
           :when (and (= procitemcode orderitemcode)
                       (= procitemcode issueitemcode)
                        (= orderitemcode issueitemcode))] 
      
       (hash-map :procitemcode procitemcode :itemname itemname
                 :unitsize unitsize :plannedquantity plannedquantity
                 :proclinecost proclinecost :proccost proccost :categoryname categoryname
                 :orderquantity orderquantity :orderlinecost (* orderunitcost orderquantity) :ordercost ordercost
                 :issuequantity issuequantity :issuelinecost issuelinecost :issuecost issuecost
                 :ordercomment (generate-order-comment plannedquantity orderquantity))))


(defn match-proc-order-issue-data1
  [items proc-plans orders issues]
  (try
  (into [] (for [{code :code itemname :itemname unitsize :unitsize 
         categoryname :categoryname} items
                 :let [
                       {procitemcode :procitemcode plannedquantity :plannedquantity proclinecost :proclinecost proccost :proccost} (get-matching-data proc-plans code :itemcode)
                       {orderquantity :orderquantity orderunitcost :unitcost orderlinecost :ordercost ordercost :totalcost} (get-matching-data orders code :itemcode)
                       {issuequantity :issuequantity issuelinecost :issuecost issuecost :totalcost} (get-matching-data issues code :itemcode)
                       ]]
      (hash-map :procitemcode code :itemname itemname
                 :unitsize unitsize :plannedquantity plannedquantity
                 :proclinecost proclinecost :proccost proccost :categoryname categoryname
                 :orderquantity orderquantity 
                 :orderlinecost (if (or (nil? orderunitcost) (nil? orderquantity)) 0.0 (* orderunitcost orderquantity)) 
                 :ordercost ordercost
                 :issuequantity issuequantity :issuelinecost issuelinecost :issuecost issuecost
                 :ordercomment generate-order-comment plannedquantity orderquantity)))
  (catch Exception e (taoensso.timbre/info (str "Match proc order issue error:" (.getMessage e))))))

;; new matching code based on inbuilt filters
(defn match-proc-order-issue-data2
  [items proc-plans orders issues]
  
  (into [] (for [{code :itemcode itemname :itemname unitsize :unitsize 
         categoryname :categoryname} items
                 :let [
                       {plannedquantity :plannedquantity proclinecost :proclinecost proccost :proccost} (get-matching-data proc-plans code :itemcode)
                       {orderquantity :orderquantity orderunitcost :unitcost orderlinecost :ordercost ordercost :totalcost} (get-matching-data orders code :itemcode)
                       {issuequantity :issuequantity issuelinecost :issuecost issuecost :totalcost} (get-matching-data issues code :itemcode)
                       ]]
      (hash-map :procitemcode code :itemname itemname
                 :unitsize unitsize :plannedquantity plannedquantity
                 :proclinecost proclinecost :proccost proccost :categoryname categoryname
                 :orderquantity orderquantity 
                 :orderlinecost (if (or (nil? orderunitcost) (nil? orderquantity)) 0.0 (* orderunitcost orderquantity)) 
                 :ordercost ordercost
                 :issuequantity issuequantity :issuelinecost issuelinecost :issuecost issuecost
                 :ordercomment generate-order-comment plannedquantity orderquantity))))

(defn all-facility-report-data
   "Gets vector of vector for all facility data"
  ([]
    (facility-reports/get-all-facility-report-data))
  
  ([level year]
    (facility-reports/get-all-facility-report-data {:level level :year year})))

(defn all-item-report-data
  "Gets vector of vector for all items data"
  ([]
    (item-reports/get-item-report-data))
  
  ([ordercycle level year]
    (item-reports/get-item-report-data {:ordercycle ordercycle :level level :year year})))

(defn all-item-report-data-sorted
  "Returns all item report data sorted by plan/order deviation desc"
  ([]
    (item-reports/sorted-item-report-data)))

(defn import-data-report-data
  []
  (let [generate-vector-from-map (fn [{code :code
                                      name :name
                                      facilitylevel :facilitylevel
                                      zone :zone
                                      procplan :hasprocplan
                                      cycle1 :hasordercycle1 cycle2 :hasordercycle2 cycle3 :hasordercycle3 
                                      cycle4 :hasordercycle4 cycle5 :hasordercycle5 cycle6 :hasordercycle6 }]
                                   
                                   (vector code name facilitylevel zone procplan cycle1 cycle2 cycle3 cycle4 cycle5 cycle6 ))]
    (apply vector (map generate-vector-from-map (facility-repository/get-data-import-report-data)))))


(defn single-item-report-data
  "Gets vector of vectors for a single item broken down by facility"
 ([code]
  (let [
       facilitys (facility-repository/get-all-facilities)
       item-data (item-repository/get-single-item-report-data code)
       latest-order-delivery-schedule 
          (orderdeliveryschedule-repository/get-last-orderdeliveryschedule-by-date-before 
            (util/get-current-financial-year)
            5
            (util/get-sql-date))
          number-of-orders (:cycle latest-order-delivery-schedule)
       item-proc-data (:procurementplandata item-data)
       item-order-data (:orderdata item-data)
       item-issue-data (:issuedata item-data)]
   
   (into [] (for [facility facilitys]
     [
      (:code facility)
      (:name facility)
      (:levelname facility)
      (format "%.1f" (* number-of-orders (util/get-fraction (get-matching-data item-proc-data (:id facility) :facility-id :numberplanned) 6.0 0.0)))
      (get-matching-data item-order-data (:id facility) :facility-id :numberordered)
      (get-matching-data item-issue-data (:id facility) :facility-id :numberissued)]))))
 
 ([code ordercycle level year]
   (let [
       facilitys (if (= 0 (java.lang.Integer/parseInt level))
                  (facility-repository/get-all-facilities) 
                   (facility-repository/get-facilities-by-level-id (java.lang.Integer/parseInt level)))
       financial-year (if (nil? year)(util/get-current-financial-year) year)
       start-end-dates-financial-year (util/get-financial-year-start-end-dates financial-year)
       item-data (item-repository/get-single-item-report-data code ordercycle level year)
       latest-order-delivery-schedule
         (if (> (java.lang.Integer/parseInt ordercycle) 0)
           (orderdeliveryschedule-repository/get-last-orderdeliveryschedule-by-cycle 
                 financial-year 5 (java.lang.Integer/parseInt ordercycle))
           (orderdeliveryschedule-repository/get-last-orderdeliveryschedule-by-date-before 
            financial-year 5 (util/get-sql-date)))
       number-of-orders (if (nil? (:cycle latest-order-delivery-schedule))
                             0 (:cycle latest-order-delivery-schedule))
       item-proc-data (:procurementplandata item-data)
       item-order-data (:orderdata item-data)
       item-issue-data (:issuedata item-data)]
   
   (into [] (for [facility facilitys]
     [
      (:code facility)
      (:name facility)
      (:levelname facility)
      (format "%.1f" (* number-of-orders (util/get-fraction (get-matching-data item-proc-data (:id facility) :facility-id :numberplanned) 6.0 0.0)))
      (get-matching-data item-order-data (:id facility) :facility-id :numberordered)
      (get-matching-data item-issue-data (:id facility) :facility-id :numberissued)
      ])))))


(defn single-facility-report-data
  "Gets vector of vectors for a single facility broken down by item"
  ([code]
    (let [
       number-of-orders (facility-order-repository/get-number-of-orders code)
       items (item-repository/get-all-items)
       facility-data (facility-repository/get-facility-report-data code)
       item-proc-data (:procurementplandata facility-data)
       item-order-data (:orderdata facility-data)
       item-issue-data (:issuedata facility-data)]
   
     (into [] (for [item items]
         (facility-reports/format-single-facility-report-data (:code item)
          (:name item)
          (get-matching-data item-proc-data (:id item) :item-id :numberplanned)
          (get-matching-data item-order-data (:id item) :item-id :numberordered)
          (get-matching-data item-issue-data (:id item) :item-id :numberissued)
          number-of-orders)))))
  ([code ordercycle year]
    (let [
       number-of-orders 1 ;TODO: Need to review the feasibility of this
       items (item-repository/get-all-items)
       facility-data (facility-repository/get-facility-report-data code ordercycle year)
       item-proc-data (:procurementplandata facility-data)
       item-order-data (:orderdata facility-data)
       item-issue-data (:issuedata facility-data)]
   
     (into [] (for [item items]
         (facility-reports/format-single-facility-report-data (:code item)
          (:name item)
          (get-matching-data item-proc-data (:id item) :item-id :numberplanned)
          (get-matching-data item-order-data (:id item) :item-id :numberordered)
          (get-matching-data item-issue-data (:id item) :item-id :numberissued)
          number-of-orders))))))


(defn all-facility-print-report-data
  "Gets vector of vector for all facility data"
  []
  (let [generate-vector-from-map (fn  [{code :code 
                                        name :name                                        
                                        level :levelname
                                        zone :zone
                                        numberofplans :numberofplans
                                        numberoforders :numberoforders
                                        numberofissues :numberofissues}]
                                   ;; The empty string is a placeholder for the input checkbox that is added postprocessing in the UI
                                   (vector "" code name level zone numberofplans numberoforders numberofissues))]
  (apply vector
         (map generate-vector-from-map 
              ;(facility-repository/get-facilities-with-proc-order-issue-number);))))
              (filter (fn [dataline] (and (> (:numberofplans dataline) 0)
                                          (> (:numberoforders dataline) 0)
                                          (> (:numberofissues dataline) 0)))
                      (facility-repository/get-facilities-with-proc-order-issue-number))))))

(def item-template
  (template
    [[:pdf-cell $procitemcode] [:pdf-cell $itemname] [:pdf-cell {:align :center} (str $unitsize)]
         [:pdf-cell {:align :center} (str $plannedquantity)] [:pdf-cell {:align :center} (str $orderquantity)] 
         [:pdf-cell {:align :center} (str $issuequantity)]
         (cond 
           (and (= $plannedquantity 0.0)  (= $orderquantity 0.0))[:pdf-cell {:size 9} "0%"]
           (= $plannedquantity 0.0) [:pdf-cell {:size 9} "N/A"]
           (= $orderquantity 0.0) [:pdf-cell {:size 9} "N/A"]
           :else [:pdf-cell (str (format "%.1f"( * ( / $orderquantity $plannedquantity) 100))  "%")])
         (cond 
           (= $ordercomment "Within limit") [:pdf-cell {:size 9 :color [154 205 50]} $ordercomment]
           (= $ordercomment "Over Ordered") [:pdf-cell {:size 9 :color [255 69 0]} $ordercomment]
           (= $ordercomment "Under Ordered") [:pdf-cell {:size 9 :color [255 255 0]} $ordercomment]
           :else [:pdf-cell $ordercomment])
         ]))

(defn compute-budget-total
  [data creditlimit]
  (let [issue-cost (:issuecost (first data))
        proc-cost (:proccost (first data))]
    (try (str "Amount of Budget Used to Date: "                       
                     ;(format "%.2f" (/ (* 200 (/ issue-cost
                      ;                           (if (and (not (nil? creditlimit))
                       ;                                   (> creditlimit 0))
                        ;                           creditlimit
                         ;                          (* 6 proc-cost))))
                     
                      (format "%.2f"  (* 100 (/ issue-cost
                                                 (if (and (not (nil? creditlimit))
                                                          (> creditlimit 0))
                                                   creditlimit
                                                   (* 6 proc-cost))))
                                        )"% of Budget Total")
                    (catch NullPointerException e " Amount of Budget Used to Date: 19.4% of Budget Total"))))


;; Actual print method that brings together all the data
;; TODO: Add column for order plan deviation
(defn print-facility-report
  [facilitycode facilityname creditlimit procplanid filename]
  (let [
      ;  items (item-repository/get-all-items)
       ;; {facilityname :facility-name facilitycode :facility-code} (facility-repository/get-facility-by-proc-plan-id proc-plan-id)
        proc-plan-data (procurement-plan-repository/get-procurement-plan-details procplanid)
        order-data (facility-order-repository/get-facility-order-details-by-facility-code-grouped facilitycode)
        issues-data (facility-issue-repository/get-facility-issue-details-by-facility-code-grouped facilitycode)
        number-of-orders (facility-order-repository/get-number-of-orders facilitycode)
       ; data (match-proc-order-issue-data1 items proc-plan-data order-data issues-data)
        data (match-proc-order-issue-data proc-plan-data order-data issues-data number-of-orders)
        category-data (group-by :categoryname data) categories (keys category-data) 
        category-number (count categories)
        
        orders (facility-order-repository/get-single-facility-order-dates facilitycode)
        procurement-plan-year (:year (procurement-plan-repository/get-plan-details-by-code facilitycode))
        last-issue (facility-issue-repository/get-last-issue facilitycode)
        data-source-column-widths (into [6 6] (repeat  (+ 1 (count orders)) 5 ))]
     
  (pdf [{
    :font  {:size 9}
    }
    [:heading {:style {:size 14 :align :center}} (str"Facility: " facilityname)]
    [:heading {:style {:size 14 :align :center}} (str "CODE: " facilitycode)]
    [:heading {:style {:size 14 :align :center}} "NMS ESSENTIAL MEDICINES ORDER REPORT"]
    [:phrase (util/format-time (Date.))]
        [:heading {:style {:size 12 :align :center}} "Data Included in this Report"]
    [:paragraph "Please note thtat the data sources below were included in this report. The extent to which the dat sources are not close in date, especially orders and issues data, does affect the assessment this report makes."]
   
    [:pdf-table
     {:spacing-before 10
      :spacing-after 15
       :bounding-box [45 100]}
 
      data-source-column-widths
      (into (into [[:pdf-cell " "]
       [:pdf-cell "Procurement Plan"]]
       
       (for [order (range 1 (+ 1 (count orders)))]
      [:pdf-cell (str "Order " order)]
      )) [[:pdf-cell "Issues Data"]])
      
     (into         
      (into [[:pdf-cell "As of Date:"]
       [:pdf-cell (str "FY "procurement-plan-year)]]
      (for [order orders]
        [:pdf-cell (util/format-time  (:orderdate order))]))
      [[:pdf-cell (util/format-time  (:issuedate last-issue))]]) ]
       
    [:heading {:style {:size 14 :align :center}} "Summary"]
    [:heading {:style {:size 12 :align :left}} "I. Budget"]
    
    ;TODO: Fix this, it is causing a divide by zero error (NullPonterException)
     [:paragraph (compute-budget-total data creditlimit) ]
    
     [:paragraph {:style :italic} "This figure is an estimate to guide your planning, subject to the final reconciliation of the Finance Department.\n\n"]
     [:heading {:style {:size 12 :align :left}} "II Ordering To Date"]
     [:pdf-table
      { :spacing-before 10}
      [50 20]
      [[:pdf-cell "Total items planned"] [:pdf-cell  {:align :center} (str (count (filter (fn [{planned :plannedquantity}]
                                                                          (< 0  planned))
                                                                        proc-plan-data)))]]
      [[:pdf-cell "Total items ordered"] [:pdf-cell {:align :center}  (str (count (filter (fn [{ordered :orderquantity}]
                                                                          (< 0 ordered))
                                                                        order-data)))]]
      [[:pdf-cell "Total items issued"] [:pdf-cell {:align :center}  (str (count issues-data))]]
      [[:pdf-cell "Total items ordered within range"] [:pdf-cell {:color [154 205 50] :align :center}  (str (count (filter (fn [{comment :ordercomment}]
                                                                          (= comment "Within limit"))
                                                                        data)))]]
      [[:pdf-cell "Total items over ordered"] [:pdf-cell {:color [255 69 0] :align :center}  (str (count (filter (fn [{comment :ordercomment}]
                                                                          (= comment "Over Ordered"))
                                                                        data)))]]
      [[:pdf-cell "Total items under ordered"] [:pdf-cell {:color [255 255 0] :align :center}  (str (count (filter (fn [{comment :ordercomment}]
                                                                          (= comment "Under Ordered"))
                                                                        data)))]]
      [[:pdf-cell "Total items planned but not ordered"] [:pdf-cell {:align :center}  (str (count (filter (fn [{comment :ordercomment}]
                                                                          (= comment "Planned for but not ordered"))
                                                                        data)))]]
      [[:pdf-cell "Total items ordered but not planned for"] [:pdf-cell {:align :center}  (str (count (filter (fn [{comment :ordercomment}]
                                                                          (= comment "Ordered but not planned for"))
                                                                        data)))]]]
     
      [:heading {:style {:size 12 :align :left}} "III Category Summary"]
       
       (into [:pdf-table
      {:bounding-box [30 100]
       :spacing-before 10}
      [15 4 4 4 3]
      [[:pdf-cell "Category Name"] [:pdf-cell "Within Range"]
      [:pdf-cell "Over Ordered"] [:pdf-cell "Under Ordered"]
      [:pdf-cell "Delivered according to plan"]]]
        (for [data-point category-data]
          (into [] 
                [[:pdf-cell (key data-point)] 
                 [:pdf-cell {:color [154 205 50] :align :center}  (str (count (filter (fn [{comment :ordercomment}]
                                                                          (= comment "Within limit"))
                                                                        (val data-point))))]
                 [:pdf-cell {:color [255 69 0] :align :center}  (str (count (filter (fn [{comment :ordercomment}]
                                                                          (= comment "Over Ordered"))
                                                                        (val data-point))))]
                 [:pdf-cell {:color [255 255 0] :align :center}  (str (count (filter (fn [{comment :ordercomment}]
                                                                          (= comment "Under Ordered"))
                                                                        (val data-point))))]
                 [:pdf-cell {:align :center} (str (count (filter (fn [{issuequantity :issuequantity
                                                      plannedquantity :plannedquantity}]
                                                                          (and
                                                                            (< 0 issuequantity)
                                                                            (< 0 plannedquantity)
                                                                            (= issuequantity plannedquantity)))
                                                                        (val data-point))))]
                 ])))
       [:pagebreak]
        [:heading {:style {:size 12 :align :left}} "IV Item Ordering Details"]
       (into [] (for [data-point category-data]
         (into [:pdf-table
      {:spacing-before 10
       :bounding-box [45 100]}
      [3 19 3 4 4 4 4 6]
      [[:pdf-cell {:colspan 8 :align :left} (key data-point)]]
      [[:pdf-cell "Code"] [:pdf-cell "Item Name"]
      [:pdf-cell "Unit"] [:pdf-cell "Planned"]
      [:pdf-cell "Ordered"] [:pdf-cell "Issued"]
      [:pdf-cell "Order Plan Deviation"][:pdf-cell "Comment"]]]
      (item-template  (val data-point)))))
   ]
 filename)))

(defn select-single-facility-print-report
  [code]
  (let [{name :name budget-ceiling :budget-ceiling proc-plan-id :proc-plan-id order-id :order-id issue-id :issue-id}
       (facility-repository/get-proc-order-issue-ids code)] 
   (do
    (print-facility-report code name budget-ceiling proc-plan-id (str code ".pdf"))
    (str code ".pdf"))))


(defn select-single-facility-print-report1
  [code]
  (let [{name :name budget-ceiling :budget-ceiling proc-plan-id :proc-plan-id order-id :order-id issue-id :issue-id}
       (facility-repository/get-proc-order-issue-ids code)] 
   (do
    (print-facility-report code name budget-ceiling proc-plan-id order-id issue-id (str code ".pdf"))
    (str code ".pdf"))))

;; Pick the appropriate facility ids for each of the codes
(defn select-facility-print-report-data
  [codes]
 (util/zip-files "temp/zipped.zip" (into [] (for [code (filter (fn [x] ( not (clojure.string/blank? x))) (clojure.string/split codes #"-"))] 
   (select-single-facility-print-report code)))))

(defn item-reports-page
  []
  (layout/render
    "reports/item.html"))

(defn single-item-report-page
  [code]
  (let [{name :name} (item-repository/get-item-by-code code)]
  (layout/render
    "reports/singleitem.html" {:name name :code code})))

(defn single-facility-report-page
  [code]
  (let [{name :name code :code} (facility-repository/get-facility-by-code code)]
  (layout/render
    "reports/singlefacility.html" {:name name :code code})))

(defn facility-reports-page
  []
  (layout/render
    "reports/facility.html"))

(defn select-facility-print-report-page
  [codes]
  (layout/render
    "reports/singlefacilityprint.html"))

(defn facility-print-reports-page
  []
  (layout/render
    "reports/facilityprint.html"))

(defn general-reports-page
  []
  (layout/render
    "reports/general.html"))

(defn import-data-report-page
  []
  (layout/render
    "reports/dataimport.html"))

(defroutes reports-routes
  (GET "/reports" [] (general-reports-page))
  (GET "/reports/item" [] (item-reports-page))
  (GET "/reports/item/itemdata" [] (util/json-response (all-item-report-data)))
  (GET "/reports/item/itemdatafilter" [ordercycle level year] (util/json-response 
                                                                (all-item-report-data ordercycle level year)))
  (GET "/reports/item/itemdata/:code" [code] (util/json-response (single-item-report-data code)))
  (GET "/reports/item/itemdatafilter/one" [ordercycle level year code] 
                                 (util/json-response (single-item-report-data code ordercycle level year)))
  (GET "/reports/item/one/:code" [code] (single-item-report-page code))
  (GET "/reports/importdata" [] (import-data-report-page))
  (GET "/reports/importdata/facilitydata" [] (util/json-response (import-data-report-data)))
  (GET "/reports/facility" [] (facility-reports-page))
  (GET "/reports/facility/facilitydata" [] (util/json-response (all-facility-report-data)))
  (GET "/reports/facility/facilitydatafilter" [level year] (util/json-response (all-facility-report-data level year)))
  (GET "/reports/facility/one/:code" [code] (single-facility-report-page code))
  (GET "/reports/facility/facilitydata/:code" [code] (util/json-response (single-facility-report-data code)))
  (GET "/reports/facility/facilitydatafilter/one" [ordercycle year code] (util/json-response (single-facility-report-data code ordercycle year)))
  (GET "/reports/facilityprint" [] (facility-print-reports-page))
  (GET "/reports/facilityprint/facilitydata" [] (util/json-response (all-facility-print-report-data)))
  (GET "/reports/facilityprint/selected/:codes" [codes] (select-facility-print-report-page codes))
  ;; Print reports for actual data
  (GET "/reports/facilityprint/print/:codes" [codes]  (noir.response/content-type 
                                                            "application/x-zip-compressed"
                                                             (clojure.java.io/input-stream (select-facility-print-report-data codes)))))

