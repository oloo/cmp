(ns cmp.routes.procurement-plan 
  (:use compojure.core)
  (:require [cmp.views.layout :as layout]
            [cmp.util :as util]
            [cmp.repository.procurement-plan-repository :as procurement-plan-repository]
            [cmp.repository.category-repository :as category-repository]))


(defn get-all-procurement-plans-data
  "Gets vector of vector for all procurement plans data"
  []
  (let [generate-vector-from-map (fn  [{id :id 
                                        year :year
                                        facilitylevel :facilitylevel
                                        totalcredit :totalcredit                                        
                                        facilityname :facilityname}]
                                   (vector id facilityname facilitylevel year totalcredit))]
  (apply vector
         (map generate-vector-from-map 
              (procurement-plan-repository/get-all-procurement-plans)))))


;; Changed this to pick only categories where there is data
(defn get-procurement-plan-category-data
  [proc-plan-id]
  (let [generate-vector-from-map (fn  [{id :id 
                                        name :name }]
                                   (hash-map :categoryid id :categoryname name))]
  ( hash-map :Result "OK" :Records (apply vector
         (map generate-vector-from-map 
              (category-repository/get-all-categories-by-proc-plan-id proc-plan-id))))))

(defn get-procurement-plan-item-data
  [procplanid itemcategoryid]
  (let [generate-vector-from-map (fn  [{categoryid :categoryid
                                        itemid :itemid
                                        code :code
                                        itemname :itemname
                                        unit :unit
                                        price :price
                                        quantity :quantity
                                        total :total }]
                                   (hash-map :categoryid categoryid :itemid itemid :code code :itemname itemname
                                             :unit unit :price price :quantity quantity :total (* price quantity) ))]
  ( hash-map :Result "OK" :Records (apply vector
         (map generate-vector-from-map 
              (procurement-plan-repository/get-items-by-category procplanid itemcategoryid))))))


(defn single-procurement-plan-page
  [id]
  (let [details (procurement-plan-repository/get-plan-details id)] 
  (layout/render
    "procurementplan/one.html"
    {:facilityname (:facilityname (first details) details)
     :totalcredit (format "%.0f" (:totalcredit (first details)))
     :id id})))

(defn all-procurement-plans-page
  []
  (layout/render
    "procurementplan/all.html"))

(defn delete-procurement-plan
  [id]
  (procurement-plan-repository/delete-procurement-plan-with-details id)
  (all-procurement-plans-page))

(defroutes procurement-plan-routes
  (GET "/procurementplan" [] (all-procurement-plans-page))
  (GET "/procurementplan/procurementplandata" [] (util/json-response (get-all-procurement-plans-data)))
  (GET "/procurementplan/one/:id" [id] (single-procurement-plan-page id))
  (GET "/procurementplan/categorydata/:procplanid" [procplanid] (util/json-response (get-procurement-plan-category-data procplanid)))
  (POST "/procurementplan/categorydata/:procplanid" [procplanid] (util/json-response (get-procurement-plan-category-data procplanid)))
  (GET "/procurementplan/itemdata/:procplanid-categoryid" [procplanid-categoryid] (util/json-response (get-procurement-plan-item-data
                                                                                                        (nth (clojure.string/split procplanid-categoryid #"-") 0)
                                                                                                         (nth (clojure.string/split procplanid-categoryid #"-") 1))))
  (POST "/procurementplan/itemdata/:procplanid-categoryid" [procplanid-categoryid] (util/json-response (get-procurement-plan-item-data 
                                                                                                         (nth (clojure.string/split procplanid-categoryid #"-") 0)
                                                                                                         (nth (clojure.string/split procplanid-categoryid #"-") 1))))
  (POST "/procurementplan/delete" [id] (delete-procurement-plan id)))

