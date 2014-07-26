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

(defn db-new
  "Takes a string and creates a YDN clientside database with the given name. Use a
   unique name to avoid collisions with other projects (they will overwrite)."
  [name]
  {:pre [(string? name)]}
  (def db (new js/ydn.db.Storage name clientside-schema))
  (new js/ydn.db.Storage name clientside-schema))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Add/Remove Datums

(def ^:private db-chan (chan))

(defn- parse-tuples
  "Helper fn to parse bacwn databases."
  [tupls]
  (let [rel-name (first (keys tupls))]
    (reduce #(conj %1 (reduce (fn [a b]
                                  (conj a (first b) (second b)))
                                [rel-name] %2))
              []
              (get-in tupls [rel-name :data]))))

(defn- read
  "Main loop that saves the bacwn database whenever it is edited in memory."
  []
  (go-loop  [[name v] (<! db-chan)]
    (let [datums (parse-tuples v)
          obj (clj->js {:name name
                        :value (pr-str datums)})]
      (do
        (.clear db "database" (js/ydn.db.KeyRange.only name))
        (.add db "database" obj)))
    (recur (<! db-chan))))

(defn add-tuples!
  "Adds and saves tuples to a bacwn database."
  [name schema & tupls]
  (let [new-db (apply database/add-tuples schema tupls)]
    (put! db-chan [name new-db])
    new-db))

;; TODO: remove-tuples instead?
;; FIXME: get tuples to store!
(defn remove-tuples!
  "Removes and saves tuples from a bacwn database."
  [name schema tupls]
  (let [new-db (database/remove-tuple schema tupls)]
    (put! db-chan [name new-db])
    new-db))

(read)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Initialization Function

(defn db-init
  "Either instatiates new datums if the given bacwn database does not exist or
   loads an existing database from clientside storage if there is a bacwn
   database."
  [name schema & data]
  (-> (.get db "database" name)
      (.done (fn [e] (if (= e js/undefined)
                      (def var#  ; HACK: is there any way to do this differently?
                        (apply add-tuples! name schema data))  ; no clientside data found
                      (let [obj (-> (js->clj e :keywordize-keys true) ; load data
                                    (:value)
                                    (reader/read-string))]
                        (def var#
                          (apply add-tuples! name schema obj)))))))
  var#)
