(ns fogus.datalog.bacwn.macros
  (:require [fogus.datalog.bacwn.impl.syntax :as syntax]
            [fogus.datalog.bacwn.impl.database :as db]
            fogus.datalog.bacwn.impl.literals
            fogus.datalog.bacwn.impl.rules
            fogus.datalog.bacwn.impl.magic))

(defmacro facts [db & tuples]
  `(db/add-tuples ~db
    ~@(map syntax/explode tuples)))

(defmacro make-database
  "Makes a database, like this
   (make-database
     (relation :fred [:mary :sue])
     (index :fred :mary)
     (relation :sally [:jen :becky])
     (index :sally :jen)
     (index :sally :becky))"
  [& commands]
  (let [wrapper (fn [cur new]
                  (let [cmd (first new)
                        body (next new)]
                    (assert (= 2 (count body)))
                    (cond
                     (= cmd 'relation)
                     `(fogus.datalog.bacwn.impl.database/add-relation ~cur ~(first body) ~(fnext body))
                     (= cmd 'index)
                     `(fogus.datalog.bacwn.impl.database/add-index ~cur ~(first body) ~(fnext body))
                     :otherwise (throw (Exception. (str new "not recognized"))))))]
    (reduce wrapper `fogus.datalog.bacwn.impl.database/empty-database commands)))

(defmacro <-
  "Build a datalog rule.  Like this:

   (<- (:head :x ?x :y ?y) (:body-1 :x ?x :y ?y) (:body-2 :z ?z) (not! :body-3 :x ?x) (if > ?y ?z))"
  [hd & body]
  (let [head (fogus.datalog.bacwn.impl.literals/build-atom hd :fogus.datalog.bacwn.impl.literals/literal)
        body (map fogus.datalog.bacwn.impl.literals/build-literal body)]
    `(fogus.datalog.bacwn.impl.rules/is-safe? (fogus.datalog.bacwn.impl.rules/build-rule ~head [~@body]))))

(defmacro ?-
  "Define a datalog query"
  [& q]
  (let [qq (fogus.datalog.bacwn.impl.literals/build-atom q :fogus.datalog.bacwn.impl.literals/literal)]
    `(with-meta ~qq {:type :fogus.datalog.bacwn.impl.rules/datalog-query})))
