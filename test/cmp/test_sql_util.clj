(ns cmp.test-sql-util
  (:use midje.sweet)
  (:require [clojure.string :as str]
            [cmp.sql-util :as sql-util]))


  
(facts "Can test the output rom wher-wrapper"
       
       "Can output accurate where clause"
       (sql-util/where-wrapper {:a [= 5] :b [= 2]})
       => {:a [= 5] :b [= 2]}
       
        "Can exclude nil values in the fitler"
       (sql-util/where-wrapper {:a [= 5] :b [= nil]})
       => {:a [= 5]}
  )
    

