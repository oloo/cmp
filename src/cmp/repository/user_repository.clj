(ns cmp.repository.user-repository 
  (:use korma.db)
  (:use korma.core)
  (:use cmp.models) 
  (:require [clojure.string :as str]
            [cmp.util :as util]
             [cemerick.friend.credentials :as creds]))

(defn get-usertype-by-name
  [name]
  (first (select usertype
                 (fields :id :name)
                 (where {:name name}))))

(defn add-user
  [firstname othername username rawpassword usertype]
  (let [usertype-id (:id (get-usertype-by-name usertype))]
    (insert user
        (values {:firstname firstname :othername othername
                 :username username :password (creds/hash-bcrypt rawpassword)
                 :usertype_id usertype-id
           :datecreated (util/get-sql-date)}))))

;; This method does extra formating to meet the format expected by Friend
;; TODO: Consider moving to authentication namespace

(defn edit-user
  [id firstname othername username usertype passwd]
  (update user 
          (set-fields {:firstname firstname
                       :othername othername
                       :username username
                       :usertype_id usertype
                       :password (creds/hash-bcrypt passwd)})
          (where  {:id id})))


(defn get-users
  []
  (let [users (select user
                      (with usertype)
                      (fields :id :firstname :othername :lastlogin
                               :username :password
                               [:usertype.name :usertypename]))]
    (into {}
          (for [current-user users]
            {(:username current-user)
             {
              :username (:username current-user)
              :password (:password current-user)
              :roles #{(keyword (:usertypename current-user))}
              }}))))


;; Get all use data from mysql database

(defn get-users-as-map
  []
  (select user
          (with usertype)
          (fields :id :firstname :othername :lastlogin
                  :username :password
                  [:usertype.name :usertypename])))

;; Get user data by userid

(defn get-user-by-id
  [id]
  (first (select user 
          (fields :id :firstname :othername :username :usertype_id :password)
          (where (= :id id)))))

(defn delete-user
  [id]
  (delete user
   (where (= :id id))))