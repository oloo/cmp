(ns cmp.routes.facility-issue 
  (:use compojure.core)
  (:require [cmp.views.layout :as layout]
            [cmp.util :as util]
            [cmp.repository.facility-issue-repository :as facility-issue-repository]
            [cmp.repository.category-repository :as category-repository]))


(defn get-all-facility-issues-data
  "Gets vector of vector for all facility issues data"
  []
  (let [generate-vector-from-map (fn  [{id :id 
                                        issuedate :issuedate 
                                        totalcost :totalcost
                                        facilitylevel :facilitylevel                                        
                                        facilityname :facilityname}]
                                   (vector id facilityname facilitylevel (str issuedate) totalcost))]
  (apply vector
         (map generate-vector-from-map 
              (facility-issue-repository/get-all-facility-issues)))))

(defn get-facility-issue-category-data
  [facilityissueid]
  (let [generate-vector-from-map (fn  [{id :id 
                                        name :name }]
                                   (hash-map :categoryid id :categoryname name))]
  ( hash-map :Result "OK" :Records (apply vector
         (map generate-vector-from-map 
              (category-repository/get-all-categories-by-issue-id facilityissueid))))))

(defn get-facility-issue-item-data
  [issueid itemcategoryid]
  (let [generate-vector-from-map (fn  [{categoryid :categoryid
                                        itemid :itemid
                                        code :code
                                        itemname :itemname
                                        unit :unit
                                        price :price
                                        quantity :quantity
                                        total :total }]
                                   (hash-map :categoryid categoryid :itemid itemid :code code :itemname itemname
                                             :unit unit :price price :quantity quantity :total (* price quantity) ))]
  ( hash-map :Result "OK" :Records (apply vector
         (map generate-vector-from-map 
              (facility-issue-repository/get-items-by-category issueid itemcategoryid))))))

(defn single-facility-issue-page
  [id]
  (let [details (facility-issue-repository/get-single-facility-issue id)] 
  (layout/render
    "issue/one.html"
    {:facilityname (:facilityname (first details) details)
     :totalcost (format "%.0f" (:totalcost (first details)))
     :id id })))

(defn all-facility-issues-page
  []
  (layout/render
    "issue/all.html"))

(defroutes facility-issue-routes
  (GET "/issue" [] (all-facility-issues-page))
  (GET "/issue/issuedata" [] (util/json-response (get-all-facility-issues-data)))
  (GET "/issue/one/:id" [id] (single-facility-issue-page id))
  (GET "/issue/categorydata/:facilityissueid" [facilityissueid] (util/json-response (get-facility-issue-category-data (java.lang.Integer/parseInt facilityissueid))))
  (POST "/issue/categorydata/:facilityissueid" [facilityissueid] (util/json-response (get-facility-issue-category-data (java.lang.Integer/parseInt  facilityissueid))))
  (GET "/issue/itemdata/:issueid-categoryid" [issueid-categoryid] (util/json-response (get-facility-issue-item-data
                                                                       (java.lang.Integer/parseInt (nth (clojure.string/split issueid-categoryid #"-") 0))
                                                                        (java.lang.Integer/parseInt (nth (clojure.string/split issueid-categoryid #"-") 1))
                                                                        )))
  (POST "/issue/itemdata/:issueid-categoryid" [issueid-categoryid] (util/json-response (get-facility-issue-item-data
                                                                                         (java.lang.Integer/parseInt (nth (clojure.string/split issueid-categoryid #"-") 0))
                                                                                         (java.lang.Integer/parseInt (nth (clojure.string/split issueid-categoryid #"-") 1))))))

