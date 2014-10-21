(ns cmp.routes.import-data 
  (:use [compojure.core] 
        [ring.middleware.params]
        [ring.middleware.multipart-params]
        [clojure.java.io])
  (:require [cmp.views.layout :as layout]
            [cmp.util :as util]
            [cmp.excel-util :as excel-util]
            [cmp.file-util :as file-util]
            [taoensso.timbre :as timbre]
            [cmp.repository.procurement-plan-repository :as procurement-plan-repository]
            [cmp.repository.facility-order-repository :as facility-order-repository]
            [cmp.repository.facility-issue-repository :as facility-issue-repository]
            [cmp.services.import-data :as services.import-data])
  (:import [java.io File]))

(defn import-facility-order
  [sent-file cycle] 
 (let [file-name (format "temp/%s" (sent-file :filename))]
  (try
    (copy (sent-file :tempfile) 
        (File. file-name))
    (facility-order-repository/add-facility-order-with-details
       (excel-util/read-facility-order-new file-name 
                                           (java.lang.Integer/parseInt cycle)))
    (str " Imported "
         (sent-file :filename)
         " sucessfully <br/>")
   (catch Exception e (str "<br/> Error importing "
                           (sent-file :filename)
                           " <br/>" (.getMessage e) "<br/>"))
   (finally (delete-file file-name true)))))

(defn import-procurement-plan
  [sent-file]
  (let [file-name (format "temp/%s" (sent-file :filename))]
    (try
      (copy (sent-file :tempfile) 
        (File. file-name))
    (procurement-plan-repository/add-procurement-plan-with-details
       (excel-util/read-procurement-plan file-name))
    (str " Imported "
         (sent-file :filename) 
         " sucessfully <br/>")
    (catch Exception e (str "<br/> Error importing "
                            (sent-file :filename)
                            " <br/>" (.getMessage e) "<br/>"))
   (finally (delete-file file-name true))))
  )

(defn import-procurement-plans
  [sent-files]
  (into []
    (for [sent-file sent-files]
      (import-procurement-plan sent-file))))

(defn import-facility-orders
  [sent-files cycle]
  (into []
  (for [sent-file sent-files]
    (import-facility-order sent-file cycle))))

(defn import-data-page
  [error-message]
  (if (or(= "" error-message) (nil? error-message))
    (layout/render "order/all.html")
    (layout/render
     "importdata/all.html" {:error error-message})))

(defroutes import-data-routes
  (GET "/importdata" [] (import-data-page "Import"))
  (wrap-multipart-params
    (wrap-params
    (POST "/importdata/procurementplan" {params :params}
        (let [sent-files (params :procplan)]
        (import-data-page (import-procurement-plans sent-files))))))
  (wrap-multipart-params
    (wrap-params
    (POST "/importdata/facilityorder" {params :params}
        (let [sent-files (params :order)
              cycle (params :cycle)]
           (import-data-page (import-facility-orders sent-files cycle)))))))