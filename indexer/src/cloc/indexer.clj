(ns cloc.indexer
  "Generate a documentation index."
  (:require [clojure.java.io :as io]
            [codox.reader :refer [read-namespaces]])
  (:import  [java.io File]))

(defn- jar?
  "Returns true if f looks like a jar file."
  [^File f]
  (.endsWith (.getName f) ".jar"))

(defn- relative-path
  [^File f]
  (let [local (.toURI (File. "."))]
    (.getPath (.relativize local (.toURI f)))))

(defn- pretty-name
  [^File f]
  (cond
   (.isDirectory f) (relative-path f)
   :else            (.getName f)))

(defn- index-namespaces
  "Index the given sequence of namespace metadata by namespace
   name and return as a map."
  [namespaces]
  (reduce
   (fn [ns-map n]
     (assoc ns-map (str (:name n)) n))
   {}
   namespaces))

(defn index-classpath
  "Returns a mapping of JAR file names to the a sequence of
   codox namespace info for that file."
  [classpath]
  (reduce
   (fn [doc-map ^File f]
     (if-let [nss (seq (read-namespaces f))]
       (update-in doc-map [(if (jar? f) :jars :dirs)]
                  assoc (pretty-name f) (index-namespaces nss))
       doc-map))
   {}
   (map io/file classpath)))

(defn local-code
  "Return a list of directories containing local namespaces"
  [index]
  (keys (:dirs index)))

(defn libraries
  "Return a list of the library names in the index."
  [index]
  (keys (:jars index)))

(defn namespaces
  "Returns a list of namespaces in given lib or directory."
  [index lib]
  (or (-> index :dirs (get lib) keys)
      (-> index :jars (get lib) keys)))

(defn docs
  "Returns a list of public vars from the requested namespace, as
   maps of the names, doc strings, arg lists, etc."
  [index lib namespace]
  (let [nsm (or (-> index :dirs (get lib))
                (-> index :jars (get lib)))]
    (get nsm namespace)))
