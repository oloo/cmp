(ns cmp.handler
  (:use cmp.routes.login
        cmp.routes.facility
        cmp.routes.procurement-plan
        cmp.routes.district
        cmp.routes.item
        cmp.routes.import-data
        cmp.routes.facility-order
        cmp.routes.facility-issue
        cmp.routes.reports
        compojure.core)
  (:require [cmp.repository.user-repository :as user-repository]
            [noir.util.middleware :as middleware]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [com.postspectacular.rotor :as rotor]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Page Not Found"))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (timbre/set-config!
    [:appenders :rotor]
    {:min-level :info
     :enabled? true
     :async? false ; should be always false for rotor
     :max-message-per-msecs nil
     :fn rotor/append})
      
  (timbre/set-config!
    [:shared-appender-config :rotor]
    {:path "{{sanitized}}.log" :max-size 10000 :backlog 10})
   
  (timbre/info "CMP has been fired up!..."))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (println "shutting down..."))

;;append your application routes to the all-routes vector
(def all-routes [login-routes facility-routes procurement-plan-routes district-routes item-routes import-data-routes facility-order-routes facility-issue-routes
                 reports-routes app-routes])

(def app (-> all-routes
             middleware/app-handler
             (friend/authenticate {:credential-fn 
                                       (partial creds/bcrypt-credential-fn (user-repository/get-users))
                                   :workflows [(workflows/interactive-form)]})
             
             ring.middleware.keyword-params/wrap-keyword-params
             ring.middleware.nested-params/wrap-nested-params
             ring.middleware.params/wrap-params
             (ring.middleware.session/wrap-session)
             ;;add your middlewares here
             ))

(def war-handler (middleware/war-handler app))
