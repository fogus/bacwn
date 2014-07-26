(ns bacwn.test.trans.read
  (:use-macros [cemerick.cljs.test :only (is deftest with-test run-tests testing
                                             use-fixtures)]
               [fogus.datalog.bacwn.macros :only (<- ?- make-database)])
  (:require [cemerick.cljs.test :as t]
            [fogus.datalog.bacwn.trans.read :as tr]
            [cljs.reader :as reader]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; fixtures

(def db-base
  (make-database
   (relation :parent [:ancestor :child])
   (index :parent :child)

   (relation :male [:person])
   (index :male :person)

   (relation :female [:person])
   (index :female :person)))

(defn fake-db
  "Creates a database for testing"
  [do-tests]
  (def woot (tr/db-new "woot"))
  (do-tests)
  (js/ydn.db.deleteDatabase "woot"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; tests


(def test-db-1
  (tr/add-tuples! "pepl" db-base
                  [:parent :ancestor "terach" :child "abraham"]
                  [:parent :ancestor "terach" :child "nachor"]))

;; FIXME: how can these be tested if the values are only accessible in the
;;  callback?
(deftest test-add-tuples!
  (let [rqst (-> (.get woot "pepl")
                 (.done (fn [e]
                          (let [obj (-> (js->clj e :keywordize-keys true)
                                        (:value)
                                        (reader/read-string))]
                            (def test-obj# obj)))))]
    (is (= test-db-1 v))))

(def test-db-2
  (tr/remove-tuples! "pepl" db-base
                     [:parent :ancestor "terach" :child "nachor"]))

(deftest test-remove-tuples!
  (let [rqst (-> (.get woot "pepl")
                 (.done (fn [e]
                          (let [obj (-> (js->clj e :keywordize-keys true)
                                        (:value)
                                        (reader/read-string))]
                            (def test-obj# obj)))))
        v test-obj#]
    (is (= test-db-2 v))))

(use-fixtures :once fake-db)

(run-tests)
