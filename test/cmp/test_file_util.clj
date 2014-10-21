(ns cmp.test-file-util
    (:use midje.sweet)
  (:require [clojure.string :as str]
            [cmp.file-util :as file-util]))

(facts "Can add to collection and pass to handler"
  
      (file-util/add-to-collection-or-handle 
        {:facilitycode "123" :other 100} :facilitycode nil [] (fn [xs] (count xs)))
      => [{:facilitycode "123" :other 100}]
     
      (file-util/add-to-collection-or-handle 
        {:facilitycode "123" :other 100} :facilitycode "123" [{:facilitycode "123" :other 101}] (fn [xs] (count xs)))
      => [{:facilitycode "123" :other 101} {:facilitycode "123" :other 100}]
      
      (file-util/add-to-collection-or-handle 
        {:facilitycode "124" :other 100} :facilitycode "123" [{:facilitycode "123" :other 101}] (fn [xs] (count xs)))
      => [{:facilitycode "124" :other 100}]
      )

(facts "Can find current value in a collection or return nil"
       (file-util/find-current-value [{:code "123" :other 100}] :code)
       => "123"
       
       (file-util/find-current-value [] :code)
       => nil
       )

(facts "Can format a list of vector of data into a hashmap basing a keyindex map"
       
       (file-util/format-data '(["We" "are" "the" "best" "option"]) {:first 0 :last 4})
       => {:first "We" :last "option"}
       
       (file-util/format-data '(["We" "are" "the" "best" "option"]) {:first 0 :last -2})
       => {:first "We" :last "best"}
       )

(facts "Can read lines from csv file, format and pas through handler depending on structure"
  
  (file-util/read-file-into-collection "testdata/testcsv.csv" (fn [xs] (print xs)) {:enterdate 0 :code 2 :amount -6})
  => nil
  )


