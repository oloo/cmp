(ns cmp.routes.district
  (:use compojure.core)
  (:require [cmp.views.layout :as layout]
            [cmp.util :as util]
            [cmp.repository.district-repository :as district-repository]))

(defn all-district-data
  "Gets vector of vector for all districts data"
  []
  (let [generate-vector-from-map (fn  [{id :id
                                        name :name                                        
                                        region :region}]
                                   (vector id  name region))]
  (apply vector
         (map generate-vector-from-map 
              (district-repository/get-all-districts)))))

(defn add-district-data
  [name region]
  (cond
    (and (not= name "") (not= region "")
         (not (nil? name)) (not (nil? region)))
     (do (district-repository/add-new-district name region)  
       "OK")
     :else
     "Error"
   ))

(defn edit-district-data
  [id name region]
  (cond
    (and (not= id "") (not= name "") (not= region "")
         (not (nil? name)) (not (nil? region)))
     (do (district-repository/edit-district id name region)  
       "OK")
     :else
     "Error"
   ))

(defn edit-district-page
  [district-id]
  (let [{id :id name :name region :region} 
        (district-repository/get-district-by-id district-id)]
  (layout/render "district/edit.html" 
                 {:id id :name name :region region})))

(defn all-districts-page
  []
  (layout/render "district/all.html"))

(defn add-district-page
  []
  (layout/render "district/add.html"))

(defroutes district-routes
  (GET "/district" [] (all-districts-page))
  (POST "/district/districtdata" [] (util/json-response (all-district-data)))
  (GET "/district/districtdata" [] (util/json-response (all-district-data)))
  (GET "/district/add" [] (add-district-page))
  (GET "/district/edit/:id" [id] (edit-district-page id))
  (POST "/district/editdata" [id name region] (util/json-response (edit-district-data id name region)))
  (POST "/district/adddata" [name region] (util/json-response (add-district-data name region))))

