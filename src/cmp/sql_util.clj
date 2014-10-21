(ns cmp.sql-util)

(defn where-wrapper 
  [predicates]
     (into {} 
            (for [predicate predicates]
              (if (or (nil? (nth (val predicate) 1))
                      (= 0 (nth (val predicate) 1)))
                {}
                 (hash-map (key predicate) (val predicate))))))