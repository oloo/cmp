(ns cmp.repository.test-facility-issue-repository
  (:use midje.sweet)
  (:use korma.core)
  (:use korma.db)
  (:require [clojure.string :as str]
            [cmp.repository.facility-issue-repository :as facility-issue-repository]))

(with-state-changes [(before 
                       :facts (defdb prod (mysql {:db "nms_cmp_test"
                                                  :user "root"
                                                  :password ""
                                                  ;; optional keys
                                                  :host "localhost"
                                                  :port "3306"})))
                     (around :facts (transaction ?form (rollback)))]
  
    (facts "Can find matching data in a collection"
       
       "Can find appropriate match when it exists"
       (facility-issue-repository/find-match
         [{:code "23" :other 123} {:code "24" :other 124}] "23"  :code)
       => {:code "23" :other 123}
       
       "Throws exception with no item found message if there is no match"
       (facility-issue-repository/find-match
         [{:code "23" :other 123} {:code "24" :other 124}] "25"  :code)
       =>  (throws Exception "No item with code: 25")
       
       "Can get single facility issue"
       (facility-issue-repository/get-single-facility-issue
         20383)
       => []
       
       "Can add facility issue"
         (facility-issue-repository/add-facility-issue
         1 "2013-01-01" 0)
       => (just {:GENERATED_KEY  number?})
      
    ))