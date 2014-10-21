(ns cmp.repository.test-facility-procurementplan-repository
  (:use midje.sweet)
  (:use korma.core)
  (:use korma.db)
 ; (:require 
  ;  [cmp.repository.facility-procurement-plan-repository :as facility-proc-plan-repository])
 ; )

(with-state-changes [(before 
                       :facts (defdb prod (mysql {:db "nms_cmp_test"
                                                  :user "root"
                                                  :password ""
                                                  ;; optional keys
                                                  :host "localhost"
                                                  :port "3306"})))
                     (around :facts (transaction ?form (rollback)))]
  
    (facts "Can find matching data in a collection"
           
           "Can get all procurement plans"
         ;  (facility-proc-plan-repository/get-all-procurement-plans)
          ; => []
           ))

