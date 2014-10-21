(ns cmp.util
  (:import java.util.Date)
  (:import java.text.SimpleDateFormat)
  (:require [noir.io :as io]
            [markdown.core :as md]
            [clj-json.core :as json]
            [dk.ative.docjure.spreadsheet :as excel]
            [clj-time.core :as clj-t]
            [taoensso.timbre :as timbre]))

(defn format-time
  "formats the time using SimpleDateFormat, the default format is
   \"dd MMM, yyyy\" and a custom one can be passed in as the second argument"
  ([time] (format-time time "dd MMM, yyyy"))
  ([time fmt]
    (.format (new java.text.SimpleDateFormat fmt) time)))

(defn md->html
  "reads a markdown file from public/md and returns an HTML string"
  [filename]
  (->>
    (io/slurp-resource filename)
    (md/md-to-html-string)))

;; generate JSON response
(defn json-response 
  "Return a Json encoded response for the data
   TODO: Move this to a generalized namespace of utilities"
  [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data)})

;; parse pdf response
(defn pdf-response
  "Return Pdf encoded response for the data"
  [name]
  {:status 200
   :headers {"Content-Type" "application/pdf"}
   :body (clojure.java.io/input-stream name)})

;; method to get date in the format
;; "yyyy-mm-dd"
(defn get-sql-date
  []
  (let [today (Date.)
        formatter (SimpleDateFormat. "yyyy-MM-dd")]
    (.format formatter today)))

;; method to get datetime in the format
;; "yyyy-mm-dd"
(defn get-sql-datetime
  []
  (let [today (Date.)
        formatter (SimpleDateFormat. "yyyy-MM-dd hh:mm:ss")]
    (.format formatter today)))

(defn reformat-date
  ([date-string]
  (let [split-date (clojure.string/split date-string #"/")]
    (if (< (java.lang.Integer/parseInt (nth split-date 0)) 12)
      
      (str (java.lang.Integer/parseInt (nth split-date 2)) "-" 
           (java.lang.Integer/parseInt (nth split-date 0)) "-" 
           (java.lang.Integer/parseInt (nth split-date 1)) )
      
      (str (java.lang.Integer/parseInt (nth split-date 2)) "-" 
           (java.lang.Integer/parseInt (nth split-date 1)) "-" 
           (java.lang.Integer/parseInt (nth split-date 0))))))
  
  ([date-string dayfirst?]
    (let [split-date (clojure.string/split date-string #"/")
          first (java.lang.Integer/parseInt (nth split-date 0))
          second (java.lang.Integer/parseInt (nth split-date 1))
          third-raw (java.lang.Integer/parseInt (nth split-date 2))
          third (if (< third-raw 2000) 
                  (+ third-raw 2000)
                  third-raw)
          ]
      (if dayfirst?
        (str third "-" 
           (if (< second 10) 
             (str "0" second)
             second) "-" 
           (if (< first 10) 
             (str "0" first)
             first))
        
       (str third "-" 
           (if (< first 10) 
             (str "0" first)
             first) "-"
           (if (< second 10) 
             (str "0" second)
             second))))))

(defn sql-date-is-after?
  "First date is after second date?
First date is after second date first date with  days offset and second date with days offset"
  ([firstdate seconddate]
  (let [
        [first-year first-month first-day] (clojure.string/split (.toString firstdate) #"-")
        [second-year second-month second-day] (clojure.string/split (.toString seconddate) #"-")]
  (clj-t/after? (clj-t/date-time (java.lang.Integer/parseInt first-year) (java.lang.Integer/parseInt first-month) (java.lang.Integer/parseInt first-day))
                (clj-t/date-time (java.lang.Integer/parseInt second-year) (java.lang.Integer/parseInt second-month) (java.lang.Integer/parseInt second-day)))))
  
    ([first second firstdateoffset seconddateoffset]
  (let [
        [first-year first-month first-day] (clojure.string/split (.toString first) #"-")
        [second-year second-month second-day] (clojure.string/split (.toString second) #"-")]
  (clj-t/after? (clj-t/plus (clj-t/date-time (java.lang.Integer/parseInt first-year) (java.lang.Integer/parseInt first-month) 
                                             (java.lang.Integer/parseInt first-day)) (clj-t/days firstdateoffset))
                (clj-t/plus (clj-t/date-time (java.lang.Integer/parseInt second-year) (java.lang.Integer/parseInt second-month) 
                                             (java.lang.Integer/parseInt second-day)) (clj-t/days seconddateoffset))))))

(defn get-current-financial-year
  []
  (let [current-date (get-sql-date)
        [year-string month-string day-string] (clojure.string/split current-date #"-")
        year (java.lang.Integer/parseInt year-string)
        month (java.lang.Integer/parseInt month-string)]
    (if (< month 7)
      (str (- year 1) "/" (- year 2000))
      (str year "/" (- year 1999)))))

(defn get-financial-year-start-end-dates
  [financial-year]
  (let [[start-year end-year-part] (clojure.string/split financial-year #"/")
        end-year (if (> 1000 (java.lang.Integer/parseInt end-year-part))
                   (str "20" end-year-part)
                   end-year-part)]
    
    {:startdate (str start-year "-07-01")
     :enddate (str end-year "-06-30")}))

(defn get-fraction
  "Helper method to retuern fraction or 0"
  [x divisor default-vaule]
  (if (or (nil? x) (= 0 x) (= 0.0 x))
      default-vaule
      (/ x divisor)))

(defn filter-map-and-return-first
  ([f col default-vaule]
  (if-let [xs (filter f col)]
    (if (empty? xs) 
      default-vaule
      (first xs))
    default-vaule))
  
  ([f col key default-vaule]
  (if-let [xs (filter f col)]
    (if (empty? xs) 
      default-vaule
     (key (first xs)))
    default-vaule)))

;; custom binary search
(defn binary-search
        "Finds earliest occurrence of x in xs (a vector of hash map) using binary search.
         Specifically built to search through a vector of hashmaps
         Returns index at which hashmap is located"
        ([xs key x]
          (if (or (nil? xs) (empty? xs))
            nil
           (loop [l 0 h (unchecked-dec (count xs))]
             (if (<= h (inc l))
               (cond
                 (== x (key (xs l))) l
                 (== x (key (xs h))) h
                 :else nil)
               (let [m (unchecked-add l (bit-shift-right (unchecked-subtract h l) 1))]
                 (if (< (key (xs m)) x)
                   (recur (unchecked-inc m) h)
                   (recur l m))))))))

(defn sort-and-reverse
  "This works for collection of vectors, maps, sets etc"
  [col comparator]
  (reverse (sort-by comparator col)))

(defn zip-files
  [zipfilename files]
  (cmp.java.ZipFiles/zip zipfilename (into-array files))
  zipfilename)
