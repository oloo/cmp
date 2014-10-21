(ns cmp.services.facility-reports
    (:require  [cmp.util :as util]
               [cmp.repository.facility-repository :as facility-repository]
               [cmp.repository.facility-order-repository :as facility-order-repository]))

(defn format-facilty-report-data
  [{code :code 
   name :name                                        
   level :levelname
   totalcredit :totalcredit}]
 (vector code name level (format "%.0f" totalcredit)))

(defn format-single-facility-report-data
  [code name plannedquantity orderquantity issuequantity number-of-orders]
                                     (vector code name 
                                             (format "%.1f" (* 1.0 (* number-of-orders plannedquantity)))
                                             orderquantity 
                                             issuequantity
                                             (cond 
                                             (and (or (= plannedquantity 0) (= plannedquantity 0.0) (nil? plannedquantity))
                                                  (or (= orderquantity 0) (= orderquantity 0.0) (nil? plannedquantity))) "0%"
                                             (or (= plannedquantity 0.0) (= plannedquantity 0) (nil? plannedquantity)) "N/A"
                                             (or (= orderquantity 0.0) (= orderquantity 0) (nil? orderquantity)) "N/A"
                                             :else (str (format "%.1f"  ( * ( / orderquantity (* number-of-orders plannedquantity)) 100))  "%"))
                                           (cond 
                                              (and (or (= plannedquantity 0) (= plannedquantity 0.0) (nil? plannedquantity))
                                                  (or (= issuequantity 0) (= issuequantity 0.0) (nil? issuequantity))) "0%"
                                             (or (= plannedquantity 0.0) (= plannedquantity 0) (nil? plannedquantity)) "N/A"
                                             (or (= issuequantity 0.0) (= issuequantity 0) (nil? issuequantity)) "N/A"
                                             :else (str (format "%.1f" ( * ( / issuequantity (* number-of-orders plannedquantity)) 100))  "%"))))

(defn get-all-facility-report-data
   "Gets vector of vector for all facility data"
  ([]
  (apply vector
         (map format-facilty-report-data
              (facility-repository/get-all-facility-report-data))))
  
  ([{level :level year :year}]
    (apply vector
         (map format-facilty-report-data 
              (facility-repository/get-all-facility-report-data level year)))))

(defn express-issue-costs-as-percentage-of-budget
  [facility-issues-plans]
  (into [] 
        (for [facility-issue-plan facility-issues-plans]
            (hash-map :name (:name facility-issue-plan)
              :expenditure (* 100 (/ 
                                    (:totalcost facility-issue-plan)
                                    (:totalcredit facility-issue-plan)))
                             ))))

(defn get-facility-budget-utilization
  "Gets facility budget utilization as a percentage. 
   number-of-facilities restricts the facilities returned to that number"
  [number-of-facilities]
  (let [facility-issues-and-plans (facility-repository/get-facility-issues-and-plans)
        facility-expenditures (express-issue-costs-as-percentage-of-budget facility-issues-and-plans)
        sorted-facility-expenditures (util/sort-and-reverse facility-expenditures :expenditure)]
    (take number-of-facilities sorted-facility-expenditures)))

(defn percentage-of-orders
  []
  (let [number-of-facilities (facility-repository/get-number-of-facilities #{4 5 6 7 8 9})]
    (map (fn [{cycle :order-cycle number-of-orders :number-of-orders}]
           (hash-map :cycle cycle  :orders 
                     (* (/ number-of-orders number-of-facilities) 100.0)))
         (facility-order-repository/get-number-of-orders-grouped-by-cycle))))
