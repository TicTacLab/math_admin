(ns malt-admin.controller.configuration
  (:require [malt-admin.view :refer (render)]
            [malt-admin.storage.configuration :as st]
            [formative.parse :as fp]
            [ring.util.response :as res]
            [formative.core :as f]))

(defn index [{{storage :storage} :web :as req}]
  (render "configuration/index" req {:profiles (st/get-profiles storage)}))

(defn create [{{storage :storage} :web
               {:keys [config profile]} :params :as req}]
  (st/write-config! storage profile config)
  (res/redirect-after-post "/configuration"))

(defn new [req] (render "configuration/new" req {}))

(defn update [req] (create req))
(defn edit [{{profile :profile} :params
             {storage :storage} :web :as req}]
  (let [config (st/read-config storage profile)]
    (render "configuration/edit" req {:profile profile :config config})))

(defn delete [{{profile :profile} :params
               {storage :storage} :web :as req}]
  (st/delete-config! storage profile)
  (res/redirect-after-post "/configuration"))
