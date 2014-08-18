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

(deftest test-add-tuples!
  (is (= (tr/add-tuples! "pepl" db-base
                         [:parent :ancestor "terach" :child "abraham"]
                         [:parent :ancestor "terach" :child "nachor"])
         {:parent #fogus.datalog.bacwn.impl.database.Relation{:schema #{:ancestor :child}, :data #{{:ancestor "terach" :child "abraham"} {:ancestor "terach" :child "nachor"}}, :indexes {:child {"abraham" #{{:ancestor "terach" :child "abraham"}}, "nachor" #{{:ancestor "terach" :child "nachor"}}}}}, :male #fogus.datalog.bacwn.impl.database.Relation{:schema #{:person}, :data #{}, :indexes {:person {}}}, :female #fogus.datalog.bacwn.impl.database.Relation{:schema #{:person}, :data #{}, :indexes {:person {}}}})))

(deftest test-remove-tuples!
  (let [test-db-1 (tr/add-tuples! "pepl" db-base
                                  [:parent :ancestor "terach" :child "abraham"]
                                  [:parent :ancestor "terach" :child "nachor"])]
    (is (= (tr/remove-tuples! "pepl" test-db-1
                              [:parent :ancestor "terach" :child "nachor"])
           {:parent #fogus.datalog.bacwn.impl.database.Relation{:schema #{:ancestor :child}, :data #{{:ancestor "terach" :child "abraham"}}, :indexes {:child {"abraham" #{{:ancestor "terach" :child "abraham"}}}}}, :male #fogus.datalog.bacwn.impl.database.Relation{:schema #{:person}, :data #{}, :indexes {:person {}}}, :female #fogus.datalog.bacwn.impl.database.Relation{:schema #{:person}, :data #{}, :indexes {:person {}}}}))))


;; FIXME: For some reason the db-init function requires two calls to begin working.
;;   After the first call, it will return with the previous value of db-init (if
;;   it was called before) or nil. After the second call, db-init will work like
;;   normal. That is why these next two tests fail. Should blocking timeouts be
;;   added to the async operations or is there someting else that can solve this?
;;   Another more likely explaination is that clojurescript.test is not cleaning
;;   the database well and accidentally leaves values from test-db-init. If
;;   test-overriding is printed out, the values from test-db-init are shown (but
;;   they really should have been deleted
(deftest test-db-init
  (is (= (tr/db-init "new-store" db-base
                     [:parent :ancestor "init" :child "values"]
                     [:parent :ancestor "go" :child "here"])
         {:parent #fogus.datalog.bacwn.impl.database.Relation{:schema #{:ancestor :child}, :data #{{:ancestor "terach" :child "abraham"} {:ancestor "terach" :child "nachor"}}, :indexes {:child {"abraham" #{{:ancestor "terach" :child "abraham"}}, "nachor" #{{:ancestor "terach" :child "nachor"}}}}}, :male #fogus.datalog.bacwn.impl.database.Relation{:schema #{:person}, :data #{}, :indexes {:person {}}}, :female #fogus.datalog.bacwn.impl.database.Relation{:schema #{:person}, :data #{}, :indexes {:person {}}}})))

;; See the prinout here, it is using values from test-db-init!
(comment (deftest test-overriding-db-init
           (do (tr/add-tuples! "new-store" db-base
                               [:parent :ancestor "woot" :child "wootwoot"])
               (println (tr/db-init "new-store" db-base
                                    [:parent :ancestor "dont" :child "override"]
                                    [:parent :ancestor "the" :child "old-values"])
                        (is (= 1 1))))))

(deftest test-overriding-db-init
  (do (tr/add-tuples! "new-store" db-base
                      [:parent :ancestor "woot" :child "wootwoot"])
      (is (= (tr/db-init "new-store" db-base
                         [:parent :ancestor "dont" :child "override"]
                         [:parent :ancestor "the" :child "old-values"])
             {:parent #fogus.datalog.bacwn.impl.database.Relation{:schema #{:ancestor :child}, :data #{{:ancestor "woot" :child "wootwoot"}}, :indexes {:child {"wootwoot" #{{:ancestor "woot" :child "wootwoot"}}}}}, :male #fogus.datalog.bacwn.impl.database.Relation{:schema #{:person}, :data #{}, :indexes {:person {}}}, :female #fogus.datalog.bacwn.impl.database.Relation{:schema #{:person}, :data #{}, :indexes {:person {}}}}))))



(use-fixtures :each fake-db)

(run-tests)


(comment
  (def woot (tr/db-new "f"))

  (tr/db-init "new-store" db-base
              [:parent :ancestor "terach" :child "abraham"]
              [:parent :ancestor "terach" :child "nachor"])

  (tr/add-tuples! "new-store" db-base
                  [:parent :ancestor "woot" :child "wootwoot"])

  (js/ydn.db.deleteDatabase "f"))
