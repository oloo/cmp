(ns cmp.services.item-reports
  (:require [cmp.views.layout :as layout]
            [cmp.util :as util]
            [cmp.repository.item-repository :as item-repository]))


(defn format-all-item-report  
  [{code :code 
    itemname :name
    numberplanned :numberplanned
    numberordered :numberordered
    numberissued :numberissued    
    }]                             
      (vector code 
           itemname 
           (if (or (nil? numberplanned) (= 0 numberplanned)
                (= 0.0 numberplanned)) 0
                  (format "%.1f"  numberplanned))
                    (if (nil? numberordered) 0 numberordered)
                    (if (nil? numberissued) 0 numberissued)
                    (if (or (nil? numberordered) (nil? numberplanned) 
                       (= 0 numberordered) (= 0 numberplanned)
                       (= 0.0 numberordered) (= 0.0 numberplanned)) 
                        "N/A" 
                        (str  (format "%.2f" (* (/ numberordered numberplanned) 100)) " %"))
                    (if (or (nil? numberissued) (nil? numberordered)
                         (= 0 numberissued) (= 0 numberordered)
                         (= 0.0 numberissued) (= 0.0 numberordered)) 
                           "N/A" 
                           (str (format "%.2f" (* (/ numberissued numberordered) 100)) " %"))))

(defn format-all-item-report-keyed  
  [{code :code 
    itemname :name
    numberplanned :numberplanned
    numberordered :numberordered
    numberissued :numberissued    
    }]                             
      (hash-map :code code 
           :itemname itemname 
           :numberplanned
           (if (or (nil? numberplanned) (= 0 numberplanned)
                (= 0.0 numberplanned)) 0
                  (format "%.1f"  numberplanned))
                    :numberordered
                    (if (nil? numberordered) 0 numberordered)
                    :numberissued
                    (if (nil? numberissued) 0 numberissued)
                    :orderplandev
                    (if (or (nil? numberordered) (nil? numberplanned) 
                       (= 0 numberordered) (= 0 numberplanned)
                       (= 0.0 numberordered) (= 0.0 numberplanned)) 
                        "N/A" 
                        (str  (format "%.2f" (* (/ numberordered numberplanned) 100)) " %"))))

(defn compute-order-plan-deviation 
  [{code :code 
    itemname :name
    numberplanned :numberplanned
    numberordered :numberordered
    numberissued :numberissued    
    }]
  (if (or (nil? numberordered) (nil? numberplanned) 
                       (= 0 numberordered) (= 0 numberplanned)
                       (= 0.0 numberordered) (= 0.0 numberplanned)) 
                        nil 
                        (* (/ numberordered numberplanned) 100)))

(defn compute-issue-order-deviation 
  [{code :code 
    itemname :name
    numberplanned :numberplanned
    numberordered :numberordered
    numberissued :numberissued    
    }]
  (if (or (nil? numberordered) (nil? numberissued) 
                       (= 0 numberordered) (= 0 numberissued)
                       (= 0.0 numberordered) (= 0.0 numberissued)) 
                        nil 
                        (* (/ numberissued numberordered) 100)))

(defn get-item-report-data
  "Gets vector of vector for all items data"
  ([]
    (apply vector
         (map format-all-item-report
              (item-repository/get-item-report-data))))
  
   ([params] 
    (apply vector
         (map format-all-item-report
              (item-repository/get-item-report-data params)))))

(defn get-item-report-data-map
  "Gets a vector of maps for all items data sorted by order-plan deviation"
  ([]
    (reverse(sort-by :orderplandev  
                (remove (fn [{orderplandev :orderplandev}] (= orderplandev "N/A")) 
                    (apply vector
                        (map format-all-item-report-keyed
                            (item-repository/get-item-report-data)))))))
  
   ([params]
     
    (apply vector
         (map format-all-item-report-keyed
              (item-repository/get-item-report-data params)))))

(defn sort-item-report-data 
  [data]
  (->> data
       (filter (comp #(re-find #"^([0-9.]+)\s%$" %) #(get % 5)))
       (sort-by (comp #(read-string (second (re-find #"^([0-9.]+)\s%$" %))) #(get % 5)) >)
       (into [])))

(defn sorted-item-report-data
  ([]
    (sort-item-report-data (get-item-report-data))))

(defn get-grouped-item-report-data
  "Get Item report data grouped by percentage"
  [compute-fn]
 (let [grouped-data (->>
    (item-repository/get-item-report-data)
    (map compute-fn)
    (remove nil?)
    (group-by #(cond
                 (< % 80) :less-than-80
                 (and (>= % 80) (<= 120)) :80-120
                 (> % 120) :more-than-120
                 :else :other)))]
   (hash-map :less (count (:less-than-80 grouped-data))
             :within (count (:80-120 grouped-data))
             :over (count (:more-than-120 grouped-data)))))

(defn item-report-vector-of-maps
  [data]
  (->> data
    (filter (comp #(re-find #"^([0-9.]+)\s%$" %) #(get % 5)))
    (sort-by (comp #(read-string (second (re-find #"^([0-9.]+)\s%$" %))) #(get % 5)) >)
    (map #(zipmap [:code :itemname :planned :ordered :issued :itemdeviation :issuedeviation] %))
    (into[])))







