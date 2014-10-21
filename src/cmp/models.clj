(ns cmp.models
  (:use korma.core)
  (:use korma.db)
  (:require [clojure.string :as str]
            [cmp.configurations :as config]))

;; Or without predefining a connection map:
(defdb prod (mysql (:db-connection config/config-map)))

;; declaring the different Entities that exist in the system
;; TODO: Consider moving these to a general configuration class
(declare facility district level item facilitycycledata
         itemcategory facilityorder facilityorderitem
         facilityissue facilityissueitem procurementplan
         procurementplanitem users itemtype itemaccount
         itemcategory itemclass itemclassification
         facilitycycleitemdata itemordertype itemsource
         orderdeliveryschedule user usertype)
 
(defentity district
  (has-many facility)
  (entity-fields :name :id :region))

(defentity level
  (has-many facility)
  (entity-fields :name :id))

(defentity itemcategory
  (has-many item)
  (entity-fields :name :id))

(defentity facility
  (belongs-to district)
  (belongs-to level)
  (has-many procurementplan)
  (has-many facilityorder) 
  (has-many facilityissue)
  (has-many facilityissueitem)
  (has-many procurementplanitem )
  (has-many facilityorderitem)
  (has-many facilitycycledata)
  (has-many facilitycycleitemdata)
  (entity-fields :name :code))

(defentity item
  (belongs-to itemcategory)
  (belongs-to itemtype)
  (belongs-to itemaccount)
  (belongs-to itemclass)
  (belongs-to itemclassification)
  (belongs-to itemordertype)
  (belongs-to itemsource)
  (has-many procurementplanitem)
  (has-many facilityorderitem)
  (has-many facilityissueitem)
  (has-many facilitycycleitemdata)
  (entity-fields :name :code))

(defentity users
  (entity-fields :name :id))

(defentity facilityorder
  (belongs-to facility)
  (has-many facilityorderitem))

(defentity facilityissue
  (belongs-to facility)
  (has-many facilityissueitem))

(defentity procurementplan
  (belongs-to facility)
  (has-many procurementplanitem))

(defentity procurementplanitem
  (belongs-to procurementplan)
  (belongs-to facility)
  (belongs-to item))

(defentity facilityorderitem
  (belongs-to facilityorder)
  (belongs-to facility)
  (belongs-to item))

(defentity facilityissueitem
  (belongs-to facilityissue)
  (belongs-to facility)
  (belongs-to item))

(defentity itemtype
  (has-many item))

(defentity itemaccount
  (has-many item))

(defentity itemclass
  (has-many item))

(defentity itemclassification
  (has-many item))

(defentity itemordertype
  (has-many item))

(defentity itemsource
  (has-many item))

(defentity facilitycycledata
  (belongs-to facility)
  (has-many facilitycycleitemdata))

(defentity facilitycycleitemdata
  (belongs-to facilitycycledata)
  (belongs-to facility)
  (belongs-to item))

(defentity orderdeliveryschedule)

(defentity usertype
  (has-many user))

(defentity user
  (belongs-to usertype))