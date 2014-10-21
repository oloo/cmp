(ns cmp.file-util
  (:require [clojure-csv.core :as csv]))

(defn find-current-value
  [coll key]
  (if (empty? coll)
    nil
    (key (first coll))))

(defn format-data
  "This takes a list of a single vector of data elements and returns a hasmap of the selected bits"
  [rawdata keyindexmap]
  (let [data (first rawdata)
        numberofitems (count data)]
     (into {} 
        (for [ [k v] keyindexmap] 
          (hash-map k 
                    (if (> v -1)
                      (nth data v)
                      (nth data 
                           (+ v numberofitems))))))))

(defn add-to-collection-or-handle
  [data key previousvalue coll handler]
  (let [currentvalue (key data)]
    (if (or (= previousvalue currentvalue)
            (empty? coll))
      (conj coll data)
      (do (handler coll)
        (conj (empty coll) data)))))

;; Typical location "/Users/oloo/Documents/chai/data/testdata.CSV"
(defn read-file-into-collection
  [filename handler keyindexmap]
  (with-open [rdr (clojure.java.io/reader filename)]
    (let [alldata (line-seq rdr)]
      ;Pass Last collection to handler after intermediate reduction
      (handler
        (reduce 
        (fn [coll x1]
          (add-to-collection-or-handle (format-data (csv/parse-csv x1) keyindexmap)
                                       :facilitycode 
                                        (find-current-value coll :facilitycode)
                                         coll
                                         handler))
      [] alldata)))))


  
  