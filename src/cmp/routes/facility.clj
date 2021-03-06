(ns cmp.routes.facility
  (:use compojure.core)
  (:require [cmp.views.layout :as layout]
            [cmp.util :as util]
            [cmp.repository.facility-repository :as facility-repository]))       

(defn all-facility-data
  "Gets vector of vector for all facility data"
  []
  (let [generate-vector-from-map (fn  [{code :code 
                                        name :name                                        
                                        level :levelname
                                        district :districtname}]
                                   (vector code name district level))]
  (apply vector
         (map generate-vector-from-map 
              (facility-repository/get-all-facilities)))))


(defn add-facility-data
  ;; hard coded districtid of 1
  [name code ccregion zone levelid 
   districtname]
      (facility-repository/add-new-facility name code 
                                               ccregion zone (Integer. levelid) 
                                               districtname 1))

(defn edit-facility-data
  [id name code levelid]
      (facility-repository/edit-facility id name code (Integer. levelid)))

(defn edit-facility-page 
  [facilitycode]
  (let [{id :id code :code name :name}
        (facility-repository/get-facility-by-code facilitycode)]
    (layout/render
    "facility/edit.html" {:id id
                          :code code
                          :name name
                          })))

(defn all-facilities-page 
  []
  (layout/render
    "facility/all.html"))

(defn single-facility-page
  [code]
  (layout/render
    "facility/one.html"))

(defn add-facility-page
  []
  (layout/render
    "facility/add.html"))

(defroutes facility-routes
  (GET "/facility" [] (all-facilities-page))
  (GET "/facility/facilitydata" [] (util/json-response (all-facility-data)))
  (GET "/facility/one/:code" [code] (single-facility-page code))
  (GET "/facility/add" [] (add-facility-page))
  (GET "/facility/edit/:code" [code] (edit-facility-page code))
  (POST "/facility/editdata" [id name code zone levelid] 
        (util/json-response (edit-facility-data id name code levelid)))
  (POST "/facility/adddata" [name code ccregion zone levelid districtname] 
        (util/json-response (add-facility-data name code ccregion zone levelid districtname))))

