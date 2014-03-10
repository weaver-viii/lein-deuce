(ns leiningen.deuce
  (:require [leiningen.core.classpath :as cp]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import java.util.Properties))

(defn- build-init [project]
  ;;TODO: support ring options
    (if-let [init-fn (-> project :immutant :init)]
    (pr-str `(do (require '~(symbol (namespace init-fn)))
                 (~init-fn)))))

(defn- build-descriptor [project]
  {:root (:root project)
   :language "clojure"
   :classpath (str/join ":" (cp/get-classpath project))
   ;; TODO: don't barf if :init is nil
   :init (build-init project)})

(defn- map->properties [m]
  (reduce (fn [p [k v]]
            (doto p
              (.setProperty (name k) v)))
    (Properties.)
    m))

(defn- copy-deploy-jar [to]
  ;; TODO: deal with different versions of wboss
  ;; pull that version from the existing deps somehow?
  (-> (cp/resolve-dependencies :dependencies
        {:dependencies '[[org.projectodd.wunderboss/wunderboss-wildfly "0.1.0-SNAPSHOT"]]})
    first
    (io/copy to)))

(defn deuce
  "Deploys the current project to the given WildFly home."
  [project wf-home]
  (let [dep-dir (io/file wf-home "standalone/deployments")
        props-file (io/file dep-dir (str (:name project) ".properties"))
        jar-file (io/file dep-dir (str (:name project) ".jar"))]
    (println "Creating" (.getAbsolutePath jar-file))
    (copy-deploy-jar jar-file)
    (println "Creating" (.getAbsolutePath props-file))
    (with-open [writer (io/writer props-file)]
      (-> project
        build-descriptor
        map->properties
        (.store writer "")))))

