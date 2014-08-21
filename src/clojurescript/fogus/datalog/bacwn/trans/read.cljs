(ns fogus.datalog.bacwn.trans.read
  (:require [cljs.reader :as reader]
            [cljs.core.async :as async :refer [chan <! >! put!]]
            [fogus.datalog.bacwn.impl.database :as database])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def ^:private clientside-schema
  (clj->js {:stores [{:name "database"
                      :keyPath "name"
                      :indexes [{:keyPath "value"
                                 :type "TEXT"}]}]}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Add/Remove Datums

(def ^:private db-chan (chan))

(defn- parse-tuples
  "Helper fn to parse bacwn databases."
  [tupls]
  (loop [rel-name (keys tupls) acc []]
    (if-not (seq rel-name)
      acc
      (recur (rest rel-name)
             (apply conj acc (map #(vec (flatten (into [(first rel-name)] %)))
                                  (get-in tupls [(first rel-name) :data])))))))

(declare ydn-db)

(defn- db-read
  "Main loop that saves the bacwn database whenever it is edited in memory."
  []
  (go-loop [[name v] (<! db-chan)]
    (let [datums (parse-tuples v)
          obj (clj->js {:name name
                        :value (pr-str datums)})]
      (.clear ydn-db "database" (js/ydn.db.KeyRange.only name))
      (.add ydn-db "database" obj))
    (recur (<! db-chan))))

(defn add-tuples!
  "Adds and saves tuples to a bacwn database."
  [name db & tupls]
  (let [new-db (apply database/add-tuples db tupls)]
    (put! db-chan [name new-db])
    new-db))

(defn remove-tuples!
  "Removes and saves tuples from a bacwn database."
  [name db & tupls]
  (let [new-db (apply database/remove-tuples db tupls)]
    (put! db-chan [name new-db])
    new-db))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Initialization Functions

(def ^:private build-chan (chan))

(defn- write-db []
  (go-loop [db (<! build-chan)]
    (def ^:private ydn-db db)))

(defn db-new
  "Takes a string and creates a YDN clientside database with the given name. Use a
   unique name to avoid collisions with other projects (they will overwrite each
   other)."
  [name]
  {:pre [(string? name)]}
  (let [ydn-db (new js/ydn.db.Storage name clientside-schema)]
    (write-db)
    (db-read)
    (put! build-chan ydn-db)))

(declare var)

;; FIXME: When there is no data and a (db-int was run on a previous value, this will
;;   accidentally return the previous value instead of the new one! This can be a problem
;;   when using (db-int for multiple data sets. When a program uses two or more init
;;   templates, some of them will retain the wrong values.
;;   A possible fix is to enforce some kind of delay with .setTimeout (or similar). This
;;   is a last resort because it will enforce a big startup tax on programs. Better would
;;   be if there is some way to load without forcing the program to wait for (db-init.
(defn db-init
  "Either instantiates new datums if the given bacwn database does not exist or
   loads an existing database from clientside storage if there is a bacwn
   database."
  [name schema & data]
  (-> (.get ydn-db "database" name)
      (.done (fn [e]
               (if (= e js/undefined) ; HACK: is there any way to do this differently?
                 (def ^:private var
                   (apply add-tuples! name schema data)) ; no clientside data found
                 (let [tupls (-> (js->clj e :keywordize-keys true) ; load data
                                 (:value)
                                 (reader/read-string))
                       read-tupls (into [] (filter #(not (nil? %)) tupls))
                       ;; below assumes the old data follows the current schema
                       datums (apply database/add-tuples schema read-tupls)]
                   (if-not (= datums var) ; database is not correct, so reset it
                     (def ^:private var
                       (apply add-tuples! name schema read-tupls))))))))
  var)
