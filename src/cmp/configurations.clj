(ns cmp.configurations
  (:require [clojure.java.io :as cio]))

(defn- load-properties
  [file-name]
  (with-open [^java.io.Reader reader (cio/reader file-name)] 
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} (for [[k v] props] [(keyword k) (read-string v)])))))

(def config-map (load-properties "configuration.properties"))

