(ns malt-admin.offloader
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [malt-admin.storage.models :as models]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [com.stuartsierra.component :as component]
            [schema.core :as s]
            [clojurewerkz.cassaforte.cql :as cql])
  (:import [com.datastax.driver.core.utils Bytes]
           [java.io FilenameFilter File]
           [java.nio.file Files]
           [java.util Date]))

(def repo-path "mathmods")

(defn write-model-to-file! [folder-path model]
  (let [{:keys [file file_name]} model
        model-file-path (format "%s/%s" folder-path file_name)
        json-file-path (str model-file-path ".json")]
    (.mkdir (io/file folder-path))                 ;; ensure folder exists
    (io/copy (io/input-stream (Bytes/getArray file))
             (io/file model-file-path))
    (spit json-file-path (-> model
                             (dissoc :file)
                             (update-in [:last_modified] #(.getTime %))
                             (json/generate-string {:pretty true})))))

(defn write-models-to-folder! [folder-path models]
  (mapv (partial write-model-to-file! folder-path) models))

(defn git-init! [repo-path]
  (sh "git" "config" "push.default" "current" :dir repo-path)
  (sh "git" "config" "user.email" "malt@deploy" :dir repo-path)
  (sh "git" "config" "user.name" "malt_deploy" :dir repo-path))

(defn git-clone! [remote-repo-path repo-path]
  (sh "git" "clone" remote-repo-path repo-path))

(defn git-pull! [repo-path]
  (sh "git" "pull" :dir repo-path))

(defn git-push! [repo-path]
  (sh "git" "push" :dir repo-path))

(defn git-add-all! [repo-path]
  (sh "git" "add" "-A" :dir repo-path))

(defn git-commit! [repo-path message]
  (sh "git" "commit" "-m" message :dir repo-path))

(defn commit-all! [repo-path]
  (git-add-all! repo-path)
  (git-commit! repo-path "restart commit")
  (git-push! repo-path))

(defn init-repo! [repo-path remote-repo-path]
  (try
    (when-not (.exists (io/file repo-path))
      (git-clone! remote-repo-path repo-path)
      (git-init! repo-path))
    (git-pull! repo-path)
    (commit-all! repo-path)
    (catch Exception e
      (log/error e "while init-repo")))
  repo-path)

(defn add-and-commit-model! [repo-path storage model-id]
  (try
    (let [model (models/get-model-file storage model-id)
          file-name (:file_name model)
          message (str "update model " file-name)]
      (write-model-to-file! repo-path model)
      (git-add-all! repo-path)
      (git-commit! repo-path message)
      (git-push! repo-path)
      (log/infof "model %s commited" (:id model)))
    (catch Exception e
      (log/error e "while commit-model")))
  repo-path)

;; PUBLIC API

(defn offload-model! [offloader model-id]
  ;; reasonable usage of some!
  (some-> offloader
          :offloader
          (send-off add-and-commit-model! (:storage offloader) model-id)))

(defrecord Offloader [offload-repo offloader storage]
  component/Lifecycle

  (start [component]
    (let [offloader (when offload-repo (agent repo-path))]
      (if offloader
        (do
          (send-off offloader init-repo! offload-repo)
          (await)
          (log/info "Offloader started"))
        (log/info "Offloader disabled"))
      (assoc component
        :offload-repo offload-repo
        :offloader offloader)))

  (stop [component]
    (when offloader
      (await offloader)
      (log/info "Offloader stopped"))
    (assoc component
      :offloader nil)))

(def OffloaderSchema
  {:offload-repo (s/maybe s/Str)})

(defn new-offloader [m]
  (as-> m $
        (select-keys $ (keys OffloaderSchema))
        (s/validate OffloaderSchema $)
        (map->Offloader $)))


(defn read-file-bytes [file]
  (Files/readAllBytes (.toPath file)))

(defn list-selected-files-in-dir [dir-path extension]
  (let [filter (reify FilenameFilter
                 (^boolean accept [_this ^File _dir ^String name]
                   (.endsWith name extension)))]
    (seq (.listFiles (io/file dir-path) filter))))

(defn read-model [meta-file]
  (-> meta-file
      (slurp)
      (json/parse-string true)
      (update-in [:last_modified] #(Date. %))
      ((fn [model] (assoc model
                     :file
                     (read-file-bytes (File. (.getParentFile meta-file)
                                             (:file_name model))))))))

(defn load-models-from-dir [dir-path]
  (as-> dir-path $
        (list-selected-files-in-dir $ ".json")
        (map read-model $)
        (doall $)))

(comment
  (def models-dir "de-models")

  (write-models-to-folder! models-dir (models/get-models (:storage dev/system)))

  (doseq [table #{"models" "cache_q" "caches" "calculation_log" "in_params"}]
    (cql/truncate (get-in dev/system [:storage :conn]) table))

  (doseq [model (load-models-from-dir models-dir)]
    (models/write-model! (:storage dev/system) model))

  )