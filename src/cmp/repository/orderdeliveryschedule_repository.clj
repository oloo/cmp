(ns cmp.repository.orderdeliveryschedule-repository
    (:use korma.db)
  (:use korma.core)
  (:use cmp.models)
  (:require [clojure.string :as str]
            [cmp.util :as util]))

(defn get-orderdeliveryschedules
  []
  (select orderdeliveryschedule
          (fields :id :year :cycle :zone :deliverydeadline :orderdeadline)))

(defn get-orderdeliveryschedule
  [year zone cycle]
  (first (select orderdeliveryschedule
                 (fields :id :year :cycle :zone :deliverydeadline :orderdeadline)
                 (where {:year year
                         :zone zone
                         :cycle cycle}))))

(defn get-matched-orderdeliveryschedule
  [col year zone cycle]
  (util/filter-map-and-return-first 
      (fn [xs] (and 
           (= year (:year xs))
           (= cycle (:cycle xs))
           (= zone (:zone xs))))
        col {}))

(defn get-last-orderdeliveryschedule-by-date-before
  [year zone date]
  (first (select orderdeliveryschedule
          (fields :id :cycle :deliverydeadline :orderdeadline)
          (where {:orderdeadline [< date]
                  :zone [= zone]
                  :year [= year]})
          (order :orderdeadline :DESC))))

(defn get-last-orderdeliveryschedule-by-cycle
  [year zone cycle]
  (first (select orderdeliveryschedule
          (fields :id :cycle :deliverydeadline :orderdeadline)
          (where {:cycle [= cycle]
                  :zone [= zone]
                  :year [= year]}))))