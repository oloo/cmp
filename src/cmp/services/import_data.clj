(ns cmp.services.import-data
  (:require [cmp.views.layout :as layout]
            [cmp.repository.facility-issue-repository :as facility-issue-repository]
            [cmp.file-util :as file-util]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.jobs :as j]
            [clojurewerkz.quartzite.jobs :refer [defjob]]
            [clojurewerkz.quartzite.schedule.daily-interval 
             :refer [schedule monday-through-friday starting-daily-at time-of-day 
                     ending-daily-at with-interval-in-minutes]])
  (:import [java.io File]))

(defn save-facility-issues
  [issues-data]
  (if (empty? issues-data)
    nil
    ; Check wether it is the first row, this has facilitycode as Ocode
    (if (= "Ocode" (:facilitycode (first issues-data))) nil
      (facility-issue-repository/add-facility-issue-with-details 
        (:facilitycode (first issues-data)) issues-data))))


(defn import-facility-issues
  [file-name]
  (file-util/read-file-into-collection 
    file-name
    save-facility-issues
    {:unitsize 5 :unitcost -5 :facilitycode 2 
     :orderid 1 :salesperson -1 :issuequantity -6 
     :issuecost -2 :itemcode 3 :quantity -6 :issuedate 0 }))

(defn schedule-import-facility-issues
  [file-name]
  (taoensso.timbre/info file-name))

(defjob NoOpJob
  [ctx]
  (schedule-import-facility-issues "Running as scheduled!"))

(defn start-point
  []
  (qs/initialize)
  (qs/start)
  (let [job (j/build
              (j/of-type NoOpJob)
              (j/with-identity (j/key "jobs.noop.1")))
        trigger (t/build
                  (t/with-identity (t/key "triggers.1"))
                  (t/start-now)
                  (t/with-schedule (schedule
                                     (with-interval-in-minutes 1)
                                     (monday-through-friday)
                                     (starting-daily-at (time-of-day 9 00 00))
                                     (ending-daily-at (time-of-day 18 00 00)))))]
  (qs/schedule job trigger)))

