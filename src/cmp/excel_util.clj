(ns cmp.excel-util
  (:require [noir.io :as io]
            [markdown.core :as md]
            [clj-json.core :as json]
            [dk.ative.docjure.spreadsheet :as excel]
            [taoensso.timbre :as timbre]
            [cmp.util :as util]))


;; read excel data
(defn read-excel-sheet
  "Note that Columns are in the format {:A :mapping}" 
  [file-name columns]
  (let [workbook (excel/load-workbook file-name)]
  (excel/select-columns columns
                        (excel/select-sheet 
                          (excel/sheet-name (first
                                              (excel/sheet-seq workbook)))
                          workbook))))

(defn read-procurement-plan-rr
  "Structure is :financial-year :facility-code :monthly-supply
   :annual-supply :data <has multiple maps each like this> 
       {:code :description :unit :previous-amount 
        :price :previous-amc :current-amc :total}"
  [file-name]
  ;; The let statements a hack to pick out the header data
  (let [proc-data (read-excel-sheet file-name {:A :code :B :description :C :unitsize :D :unitcost
                                          :E :averagequantity :G :plannedquantity  :H :totalcost})
        data-extract (hash-map
          :year (clojure.string/replace ((nth proc-data 0) :code) #"PROCUREMENT PLAN FY" "")
          :facility-code ((nth proc-data 2) :unitsize)
          :totalcredit ((nth proc-data 3) :unitsize)
          :data (filter #(number? (% :totalcost)) proc-data))]
    (if (or (= (:year data-extract) "")
            (= (:facility-code data-extract) "")
            (= (:annual-supply data-extract) "")
            (empty? (:data data-extract)))
      (throw (Exception. (str " <br/> file " (last (clojure.string/split file-name #"/")) "<br/>"
                              " year " (:year data-extract) "<br/>"
                              " facility-code: " (:facility-code data-extract) "<br/>"
                              " totalcredit " (:totalcredit data-extract) "<br/>"
                              " number of items: " (count (:data data-extract)))))
      data-extract)))

(defn read-procurement-plan-gh
  "Structure is :year :facility-code :totalcredit
   :data <has multiple maps each like this> 
       {:code :description :unitsize :unitcost 
        :averagequantity :plannedquantity :totalcost}"
  [file-name]
  ;; The let statements a hack to pick out the header data
  (let [proc-data (read-excel-sheet file-name {:A :code :B :description :C :unitsize 
                                                          :D :unitcost :E :averagequantity 
                                                          :F :plannedquantity :G :totalcost }) 
        data-extract (hash-map
        :year (clojure.string/replace ((nth proc-data 0) :code) #"GENERAL HOSPITALS VITAL ITEMS PROCUREMENT PLAN FY" "")
        :facility-code ((nth proc-data 1) :unitsize)
        :totalcredit ((nth proc-data 4) :unitsize)
        :data (filter #(number? (% :totalcost)) proc-data))]
     (if (or (nil? (:year data-extract))
            (nil? (:facility-code data-extract))
            (nil? (:totalcredit data-extract))
            (empty? (:data data-extract)))
      (throw (Exception. (str " <br/> file " (last (clojure.string/split file-name #"/")) "<br/>"
                              " year " (:year data-extract) "<br/>"
                              " facility-code: " (:facility-code data-extract) "<br/>"
                              " totalcredit " (:totalcredit data-extract) "<br/>"
                              " number of items: " (count (:data data-extract)))))
      data-extract)))

;; Read procurement plan for HC IV
(defn read-procurement-plan-hc4
  "Structure is :year :facility-code :totalcredit
   :data <has multiple maps each like this> 
       {:code :description :unitsize :unitcost 
        :averagequantity :plannedquantity :totalcost}"
  [file-name]
  ;; The let statements a hack to pick out the header data
  (let [proc-data (read-excel-sheet file-name {:A :code :B :description :C :unitsize 
                                                          :D :unitcost :E :averagequantity 
                                                          :F :plannedquantity :G :totalcost }) 
        data-extract (hash-map
        :year (clojure.string/replace ((nth proc-data 0) :code) #"HEALTH CENTER IV PROCUREMENT PLAN FY" "")
        :facility-code ((nth proc-data 1) :unitsize)
        :totalcredit (+ ((nth proc-data 5) :unitsize)
                       ((nth proc-data 6) :unitsize))
        :data (filter #(number? (% :totalcost)) proc-data))]
     (if (or (nil? (:year data-extract))
            (nil? (:facility-code data-extract))
            (nil? (:totalcredit data-extract))
            (empty? (:data data-extract)))
      (throw (Exception. (str " <br/> file " file-name "<br/>"
                              " year " (:year data-extract) "<br/>"
                              " facility-code: " (:facility-code data-extract) "<br/>"
                              " totalcredit " (:totalcredit data-extract) "<br/>"
                              " number of items: " (count (:data data-extract)))))
      data-extract)))

;; General entry method for reading procurement plans
(defn read-procurement-plan
  "Read procurement plan data from excel file. 
   Delegates to approporaite method
   depending on Health Center Level"
  [file-name]
  (let [raw-data (read-excel-sheet file-name {:A :type})]
    (cond (not (nil? (re-find #"HEALTH CENTER IV PROCUREMENT PLAN" ((nth raw-data 0) :type))))
             (read-procurement-plan-hc4 file-name)
          (not (nil? (re-find #"GENERAL HOSPITALS VITAL ITEMS PROCUREMENT PLAN" ((nth raw-data 0) :type))))
             (read-procurement-plan-gh file-name)
          (not (nil? (re-find #"PROCUREMENT PLAN" ((nth raw-data 0) :type))))
             (read-procurement-plan-rr file-name)
          :else
             (throw (Exception. "The file does not match the structure for HC IV, General Hospital Or Referral Hosptial"))
      ))
  )

;; Read facility orders for regional referral hospitals
(defn read-facility-order-rr
  [file-name]
  ;; The let statements a hack to pick out the header data
  (let [order-data (read-excel-sheet file-name {:A :code :B :description :C :unitsize 
                                                          :D :unitcost :F :orderquantity :G :totalcost }) 
        data-extract (hash-map
        :order-date ((nth order-data 0) :orderquantity)
        :facility-code ((nth order-data 2) :unitsize)
        :expense ((nth order-data 3) :unitsize)
        :data (filter #(number? (% :totalcost)) order-data))]
    
     (if (or (nil? (:order-date data-extract))
            (nil? (:facility-code data-extract))
            (nil? (:expense data-extract))
            (empty? (:data data-extract)))
      (throw (Exception. (str ;" <br/> file " file-name "<br/>"
                              " order-date " (:order-date data-extract) "<br/>"
                              " facility-code: " (:facility-code data-extract) "<br/>"
                              " expense: " (:expense data-extract) "<br/>"
                              " number of items: " (count (:data data-extract)))))
      data-extract)))


;; Read facility orders for general hospitals
(defn read-facility-order-gh
  [file-name]
  ;; The let statements a hack to pick out the header data
  (let [order-data (read-excel-sheet file-name {:A :code :B :description :C :unitsize 
                                                          :D :unitcost :F :orderquantity :G :totalcost }) 
        data-extract (hash-map
        :order-date ((nth order-data 3) :unitsize)
        :facility-code ((nth order-data 2) :unitsize)
        :expense (+ ((nth order-data 4) :unitsize)
                       ((nth order-data 5) :unitsize))
        :data (filter #(number? (% :totalcost)) order-data))]
    
     (if (or (nil? (:order-date data-extract))
            (nil? (:facility-code data-extract))
            (nil? (:expense data-extract))
            (empty? (:data data-extract)))
      (throw (Exception. (str ;" <br/> file " file-name "<br/>"
                              " order-date " (:order-date data-extract) "<br/>"
                              " facility-code: " (:facility-code data-extract) "<br/>"
                              " expense: " (:expense data-extract) "<br/>"
                              " number of items: " (count (:data data-extract)))))
      data-extract)))

;; Read facility orders for hc IVs
(defn read-facility-order-hc4
  [file-name]
  ;; The let statements a hack to pick out the header data
  (let [order-data (read-excel-sheet file-name {:A :code :B :description :C :unitsize 
                                                :D :unitcost :F :orderquantity :G :totalcost }) 
        data-extract (hash-map
        :order-date ((nth order-data 3) :unitsize)
        :facility-code ((nth order-data 1) :unitsize)
        :expense (+ ((nth order-data 4) :unitsize)
                       ((nth order-data 5) :unitsize))
        :data (filter #(number? (% :totalcost)) order-data))]
    
     (if (or (nil? (:order-date data-extract))
            (nil? (:facility-code data-extract))
            (nil? (:expense data-extract))
            (empty? (:data data-extract)))
      (throw (Exception. (str ;" <br/> file " file-name "<br/>"
                              " order-date " (:order-date data-extract) "<br/>"
                              " facility-code: " (:facility-code data-extract) "<br/>"
                              " expense: " (:expense data-extract) "<br/>"
                              " number of items: " (count (:data data-extract)))))
      data-extract)))

;; General entry method for reading facility orders
(defn read-facility-order
  "Read facility order data from excel file. 
   Delegates to approporaite method
   depending on Health Center Level"
  [file-name]
  (let [raw-data (read-excel-sheet file-name {:A :level :C :level2 :H :type})]
    (cond
          (not (nil? (re-find #"GENERAL HOSPITAL NAME" ((nth raw-data 0) :level))))
             (read-facility-order-gh file-name)
          (not (nil? (re-find #"HEALTH CENTER IV NAME" ((nth raw-data 0) :level))))
             (read-facility-order-hc4 file-name)
          (or (not (nil? (re-find #"RRH" ((nth raw-data 1) :type))))
              (not (nil? (re-find #"REGIONAL" ((nth raw-data 1) :level2)))))
             (read-facility-order-rr file-name)
          :else
             (throw (Exception. "The file does not match the structure for HC IV, General Hospital Or Referral Hosptial"))
      ))
  )

;; Read facility orders for regional referral hospitals
(defn read-facility-order-rr-new
  [file-name default-cycle]
  ;; The let statements a hack to pick out the header data
  (let [order-data (read-excel-sheet file-name {:A :code :B :description :C :unitsize 
                                                          :D :unitcost :F :orderquantity :G :totalcost }) 
        data-extract (hash-map
        :order-date ((nth order-data 0) :orderquantity)
        :facility-code ((nth order-data 2) :unitsize)
        :cycle (if (= default-cycle 0)
            ((nth order-data 0) :unitcost)
            default-cycle)
        :expense ((nth order-data 3) :unitsize)
        :data (filter #(number? (% :totalcost)) order-data))]
    
     (if (or (nil? (:order-date data-extract))
            (nil? (:facility-code data-extract))
            (nil? (:expense data-extract))
            (empty? (:data data-extract)))
      (throw (Exception. (str ;" <br/> file " file-name "<br/>"
                              " order-date " (:order-date data-extract) "<br/>"
                              " facility-code: " (:facility-code data-extract) "<br/>"
                              " expense: " (:expense data-extract) "<br/>"
                              " number of items: " (count (:data data-extract)))))
      data-extract)))


;; Read facility orders for general hospitals
(defn read-facility-order-gh-new
  [file-name default-cycle]
  ;; The let statements a hack to pick out the header data
  (let [order-data (read-excel-sheet file-name {:A :code :B :description :C :unitsize 
                                                          :D :unitcost :F :orderquantity :G :totalcost }) 
        data-extract (hash-map
        :order-date ((nth order-data 0) :orderquantity)
        :facility-code ((nth order-data 1) :unitsize)
        :cycle (if (= default-cycle 0)
            ((nth order-data 0) :unitcost)
            default-cycle) 
        :expense (+ ((nth order-data 4) :unitsize)
                       ((nth order-data 5) :unitsize))
        :data (filter #(number? (% :totalcost)) order-data))]
    
     (if (or (nil? (:order-date data-extract))
            (nil? (:facility-code data-extract))
            (nil? (:expense data-extract))
            (empty? (:data data-extract)))
      (throw (Exception. (str ;" <br/> file " file-name "<br/>"
                              " order-date " (:order-date data-extract) "<br/>"
                              " facility-code: " (:facility-code data-extract) "<br/>"
                              " expense: " (:expense data-extract) "<br/>"
                              " number of items: " (count (:data data-extract)))))
      data-extract)))

;; Read facility orders for hc IVs
(defn read-facility-order-hc4-new
  [file-name default-cycle]
  ;; The let statements a hack to pick out the header data
  (let [order-data (read-excel-sheet file-name {:A :code :B :description :C :unitsize 
                                                :D :unitcost  :F :orderquantity :G :totalcost }) 
        data-extract (hash-map
        :order-date ((nth order-data 0) :orderquantity)
        :facility-code ((nth order-data 1) :unitsize)
        :cycle (if (= default-cycle 0)
            ((nth order-data 0) :unitcost)
            default-cycle)
        :expense (+ ((nth order-data 4) :unitsize)
                       ((nth order-data 5) :unitsize))
        :data (filter #(number? (% :totalcost)) order-data))]
    
     (if (or (nil? (:order-date data-extract))
            (nil? (:facility-code data-extract))
            (nil? (:expense data-extract))
            (empty? (:data data-extract)))
      (throw (Exception. (str ;" <br/> file " file-name "<br/>"
                              " order-date " (:order-date data-extract) "<br/>"
                              " facility-code: " (:facility-code data-extract) "<br/>"
                              " expense: " (:expense data-extract) "<br/>"
                              " number of items: " (count (:data data-extract)))))
      data-extract)))

;; General entry method for reading facility orders
(defn read-facility-order-new
  "Read facility order data from excel file. 
   Delegates to approporaite method
   depending on Health Center Level"
  [file-name default-cycle]
  (let [raw-data (read-excel-sheet file-name {:A :level :C :level2 :H :type})]
    (cond
          (not (nil? (re-find #"GENERAL HOSPITALS ORDER FORM" ((nth raw-data 0) :level))))
             (read-facility-order-gh-new file-name default-cycle)
          (not (nil? (re-find #"HEALTH CENTER IV ORDER FORM" ((nth raw-data 0) :level))))
             (read-facility-order-hc4-new file-name default-cycle)
          (or (not (nil? (re-find #"ORDER FORM" ((nth raw-data 0) :level))))
              (not (nil? (re-find #"RRH" ((nth raw-data 1) :type)))))
            (read-facility-order-rr-new file-name default-cycle)
          :else
             (throw (Exception. "The file does not match the structure for HC IV, General Hospital Or Regional Referral Hosptial"))
      )))

;; Read facility Issues. This comes from Macs extractor 
(defn read-facility-issue
  [file-name]
  "Read facility issue dat from excel file.
   This data is from extractor and is 
   a scheduled export"
  (let [issues-data (read-excel-sheet file-name {:A :issuedate :B :issue :C :facilitycode :D :itemcode
                                                 :F :unitsize :L :issuequantity :M :unitcost :P :issuecost}) ]
    (group-by :facilitycode issues-data)))

(defn save-data-excel
  [data]
  (let [wb (excel/create-workbook "info" 
                                  data)]
    (excel/save-workbook! "info.xls" wb))) 





