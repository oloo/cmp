(ns cmp.routes.login
  (:use compojure.core)
  (:require [cmp.views.layout :as layout]
            [cmp.util :as util]
            [cmp.services.facility-reports :as facility-reports]
            [cmp.services.item-reports :as item-reports]
            [cmp.services.user-data :as user-data]
            [cmp.repository.user-repository :as user-repository]
            [cemerick.friend :as friend]))


 ;;TODO Rename this file to something appropriate

(defn home-page
  []
  (layout/render "home.html"))

(defn login-page
  []
  (layout/render "login/login.html"))

(defn register-user-page
  []
  (layout/render "login/register.html"))

(defn register-user 
  [firstname othername username password usertype ]
  (cond 
    (and (not= firstname "") (not= othername  "") (not= username "") (not= password "") (not= usertype "")
         (not (nil? firstname)) (not (nil? othername)) (not (nil? username)) (not (nil? password)) (not (nil? usertype)))
     (do (user-repository/add-user firstname othername username password usertype)
       "Succesfully Added User ")
     :else
     "Error -Why cant u fix me ASAP"
   )) 
    
(defn get-facilities-highest-expediture
  [number-of-facilities]
  (facility-reports/get-facility-budget-utilization number-of-facilities))

(defn get-item-order-deviation
  []
  (cmp.services.item-reports/item-report-vector-of-maps(cmp.services.item-reports/get-item-report-data)))

(defn get-item-ordering-performance
  []
  (item-reports/get-grouped-item-report-data 
     item-reports/compute-order-plan-deviation))

(defn get-item-issues-performance
  []
  (item-reports/get-grouped-item-report-data 
     item-reports/compute-issue-order-deviation))

(defn get-percentage-of-orders
  []
  (facility-reports/percentage-of-orders))

(defn get-user-list
  "Gets vector of vector for user data"
  []
  (let [generate-vector-from-map (fn [{id :id
                                       firstname :firstname
                                       othername :othername
                                       username :username
                                       usertypename :usertypename
                                       lastlogin :lastlogin
                                       }]
                                   (vector id firstname othername username usertypename lastlogin))]
    (apply vector 
           (map generate-vector-from-map
                (user-data/get-user-data)))))

(defn all-users-page 
  []
  (layout/render
    "login/users.html"))

(defn edit-user-data
  [id firstname othername username usertype passwd]
  (cond
    (and (not= id "") (not= firstname "") (not= othername "") (not= username "") (not= usertype "") (not= passwd "")
         (not (nil? firstname)) (not (nil? othername)) (not (nil? username)) (not (nil? usertype)) (not (nil? passwd)))
     (do (user-repository/edit-user id firstname othername username usertype passwd)  
       "OK")
     :else
     "Error"
   ))

(defn edit-user-page
  [user-id]
  (let [{id :id firstname :firstname othername :othername username :username usertype :usertype_id passwd :password} 
        (user-repository/get-user-by-id user-id)]
  (layout/render "login/edit.html" 
                 {:id id :firstname firstname :othername othername :username username :usertype_id usertype :password passwd})))

(defn delete-user-record
  [userid]
  (user-data/delete-user userid))

(defroutes login-routes
  (GET "/home/user_list" [] (util/json-response (cmp.routes.login/get-user-list)))
  (POST "/home/user_list" [] (util/json-response (cmp.routes.login/get-user-list)))
  (GET "/home/users" [] (friend/authorize #{:administrator} (all-users-page)))
  (GET "/home/edit/:id" [id] (friend/authorize #{:administrator} (edit-user-page id)))
  (POST "/home/delete_user" [id] (friend/authorize #{:administrator} (delete-user-record id)))
  (POST "/home/edituser" [id firstname othername username usertype] (util/json-response (edit-user-data id firstname othername username usertype "")))
  (GET "/home/facility_expenditure" [] (util/json-response (get-facilities-highest-expediture 3))) ;Restricted number to 3
  (GET "/home/item_order_deviation" [] (util/json-response (get-item-order-deviation)))
  (GET "/home/item_ordering_performance" [] (util/json-response (get-item-ordering-performance)))
  (GET "/home/item_issues_performance" [] (util/json-response (get-item-issues-performance)))
  (GET "/home/facility_order_percentage" [] (util/json-response (get-percentage-of-orders)))
  (GET "/" [] (friend/authorize #{:user :administrator} (home-page)))
  ;; Broke consistency due to requirements of Friend library
  ;; TODO: Change this to standard format
  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))
  (GET "/login" [] (login-page))
  (GET "/login/register" [] (friend/authorize #{:administrator} (register-user-page)))
  (POST "/login/registeruser" [firstname othername username password usertype] (util/json-response (register-user firstname othername username password usertype))))
