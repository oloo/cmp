(ns cmp.test-util
  (:use midje.sweet)
  (:require [clojure.string :as str]
            [cmp.util :as util]))

(facts "Can reformat date based on the format For now onlty checking the one with two 
        arguments"
       
       (util/reformat-date "23/11/2013" true)
       => "2013-11-23"
       
       (util/reformat-date "09/05/2013" false)
       => "2013-09-05"
       
       (util/reformat-date "1/8/2013" true)
       => "2013-08-01"
       
       (util/reformat-date "1/8/13" true)
       => "2013-08-01"
       )

(facts "Can get date range for given year. Financial year starts on July 1 and ends on June 30"
       
       "Can return hashmap with date ranges for startdate and end date"
       (util/get-financial-year-start-end-dates "2013/14")
       => {:startdate "2013-07-01" :enddate "2014-06-30"}
       
       (util/get-financial-year-start-end-dates "2013/2014")
       => {:startdate "2013-07-01" :enddate "2014-06-30"}
       
       )