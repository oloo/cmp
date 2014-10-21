(ns cmp.routes.facility-order 
  (:use compojure.core)
  (:require [cmp.views.layout :as layout]
            [cmp.util :as util]
            [cmp.repository.facility-order-repository :as facility-order-repository]
            [cmp.repository.category-repository :as category-repository]))


(defn get-all-facility-orders-data
  "Gets vector of vector for all facility orders data"
  []
  (let [generate-vector-from-map (fn  [{id :id 
                                        orderdate :orderdate 
                                        totalcost :totalcost 
                                        cycle :cycle
                                        facilitylevel :facilitylevel
                                        facilityname :facilityname}]
                                   (vector id facilityname facilitylevel cycle (str orderdate) totalcost))]
  (apply vector
         (map generate-vector-from-map 
              (facility-order-repository/get-all-facility-orders)))))

(defn get-facility-order-category-data
  [facility-order-id]
  (let [generate-vector-from-map (fn  [{id :id 
                                        name :name }]
                                   (hash-map :categoryid id :categoryname name))]
  ( hash-map :Result "OK" :Records (apply vector
         (map generate-vector-from-map 
              (category-repository/get-all-categories-by-order-id facility-order-id))))))

(defn get-facility-order-item-data
  [orderid itemcategoryid]
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
              (facility-order-repository/get-items-by-category orderid itemcategoryid))))))

(defn single-facility-order-page
  [id]
  (let [details (facility-order-repository/get-single-facility-order id)] 
  (layout/render
    "order/one.html"
    {:facilityname (:facilityname (first details) details)
     :totalcost (format "%.0f" (:totalcost (first details)))
     :id id
     })))

(defn all-facility-orders-page
  []
  (layout/render
    "order/all.html"))

(defn delete-facility-order 
  [id]
  (facility-order-repository/delete-facility-order id)
  (all-facility-orders-page))

(defroutes facility-order-routes
  (GET "/order" [] (all-facility-orders-page))
  (GET "/order/orderdata" [] (util/json-response (get-all-facility-orders-data)))
  (GET "/order/one/:id" [id] (single-facility-order-page id))
  (GET "/order/categorydata/:facilityorderid" [facilityorderid] (util/json-response (get-facility-order-category-data facilityorderid)))
  (POST "/order/categorydata/:facilityorderid" [facilityorderid] (util/json-response (get-facility-order-category-data facilityorderid)))
  (GET "/order/itemdata/:orderid-categoryid" [orderid-categoryid] (util/json-response (get-facility-order-item-data 
                                                                                        (nth (clojure.string/split orderid-categoryid #"-") 0)
                                                                                        (nth (clojure.string/split orderid-categoryid #"-") 1))))
  (POST "/order/itemdata/:orderid-categoryid" [orderid-categoryid] (util/json-response (get-facility-order-item-data
                                                                         (nth (clojure.string/split orderid-categoryid #"-") 0)
                                                                         (nth (clojure.string/split orderid-categoryid #"-") 1))))
  (POST "/order/delete" [id] (delete-facility-order id)))

