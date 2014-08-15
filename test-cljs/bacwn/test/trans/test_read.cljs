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


(def test-db-1
  (tr/add-tuples! "pepl" db-base
                  [:parent :ancestor "terach" :child "abraham"]
                  [:parent :ancestor "terach" :child "nachor"]))

(def test-db-2
  (tr/remove-tuples! "pepl" test-db-1
                     [:parent :ancestor "terach" :child "nachor"]))

(defn fake-db
  "Creates a database for testing"
  [do-tests]
  (def woot (tr/db-new "woot"))
  (do-tests)
  (js/ydn.db.deleteDatabase "woot"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; tests

(deftest test-add-tuples!
  (is (= test-db-1
         {:parent #fogus.datalog.bacwn.impl.database.Relation{:schema #{:ancestor :child}, :data #{{:ancestor "terach" :child "abraham"} {:ancestor "terach" :child "nachor"}}, :indexes {:child {"abraham" #{{:ancestor "terach" :child "abraham"}}, "nachor" #{{:ancestor "terach" :child "nachor"}}}}}, :male #fogus.datalog.bacwn.impl.database.Relation{:schema #{:person}, :data #{}, :indexes {:person {}}}, :female #fogus.datalog.bacwn.impl.database.Relation{:schema #{:person}, :data #{}, :indexes {:person {}}}})))

(deftest test-remove-tuples!
  (is (= test-db-2
         {:parent #fogus.datalog.bacwn.impl.database.Relation{:schema #{:ancestor :child}, :data #{{:ancestor "terach" :child "abraham"}}, :indexes {:child {"abraham" #{{:ancestor "terach" :child "abraham"}}}}}, :male #fogus.datalog.bacwn.impl.database.Relation{:schema #{:person}, :data #{}, :indexes {:person {}}}, :female #fogus.datalog.bacwn.impl.database.Relation{:schema #{:person}, :data #{}, :indexes {:person {}}}})))


(use-fixtures :once fake-db)

(run-tests)
