(ns cmp.routes.item
  (:use compojure.core)
  (:require [cmp.views.layout :as layout]
            [cmp.util :as util]
            [cmp.repository.item-repository :as item-repository]
            [cmp.repository.category-repository :as category-repository]))       

(defn all-item-data
  "Gets vector of vector for all items data"
  []
  (let [generate-vector-from-map (fn  [{code :code 
                                        itemname :name 
                                        categoryname :categoryname                                        
                                        packsize :packsize
                                        unitcost :unitcost
                                        itemtypename :itemtypename}]
                                   (vector code itemname categoryname itemtypename packsize unitcost))]
  (apply vector
         (map generate-vector-from-map 
              (item-repository/get-all-items)))))

(defn add-item-data
  [name code pack-size unitcost category type ordertype 
   itemsource itemclassification itemclass itemaccount]
      (item-repository/add-new-item name code pack-size (Integer. unitcost) (Integer. category) 
                                    (Integer. type) (Integer. ordertype) (Integer. itemsource)
                                    (Integer. itemclassification) (Integer. itemclass) (Integer. itemaccount)))
(defn edit-item-page 
  [itemcode]
  (let [{id :id code :code name :name unitcost :unitcost packsize :packsize}
        (item-repository/get-item-by-code itemcode)]
    (layout/render
    "item/edit.html" {:id id
                          :code code
                          :name name
                          :packsize packsize
                          :unitcost unitcost
                          })))

(defn all-items-page 
  []
  (layout/render
    "item/all.html"))

(defn single-item-page
  []
  (layout/render
    "item/one.html"))

(defn add-item-page
  []
  (layout/render
    "item/add.html" {:categories (category-repository/get-all-categories)}))

(defroutes item-routes
  (GET "/item" [] (all-items-page))
  (GET "/item/itemdata" [] (util/json-response (all-item-data)))
  (GET "/item/one/:code" [code] (single-item-page))
  (GET "/item/add" [] (add-item-page))
  (GET "/item/edit/:code" [code] (edit-item-page code))
  (POST "/item/one" [data] (add-item-page))
  (POST "/item/adddata" [name code packsize unitcost category type ordertype itemsource itemclassification itemclass itemaccount]
        (util/json-response (add-item-data name code packsize unitcost category type ordertype itemsource itemclassification itemclass itemaccount))))
