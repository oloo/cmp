(ns cmp.services.user-data
  (:require [cmp.views.layout :as layout]
            [cmp.util :as util]
            [cmp.repository.user-repository :as user-repository]))


(defn get-user-data
  "Gets vector of maps for all user data"
  ([]
   (apply vector 
          (user-repository/get-users-as-map))))

(defn delete-user
  "Deletes user record "
  [id]
  (user-repository/delete-user id))