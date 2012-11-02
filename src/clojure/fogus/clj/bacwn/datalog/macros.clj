(ns bacwn.datalog.macros
  (:require bacwn.datalog.impl.literals
            bacwn.datalog.impl.rules
            bacwn.datalog.impl.magic))

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
                     `(bacwn.datalog.impl.database/add-relation ~cur ~(first body) ~(fnext body))
                     (= cmd 'index)
                     `(bacwn.datalog.impl.database/add-index ~cur ~(first body) ~(fnext body))
                     :otherwise (throw (Exception. (str new "not recognized"))))))]
    (reduce wrapper `bacwn.datalog.impl.database/empty-database commands)))

(defmacro <-
  "Build a datalog rule.  Like this:

   (<- (:head :x ?x :y ?y) (:body-1 :x ?x :y ?y) (:body-2 :z ?z) (not! :body-3 :x ?x) (if > ?y ?z))"
  [hd & body]
  (let [head (bacwn.datalog.impl.literals/build-atom hd :bacwn.datalog.impl.literals/literal)
        body (map bacwn.datalog.impl.literals/build-literal body)]
    `(bacwn.datalog.impl.rules/is-safe? (bacwn.datalog.impl.rules/build-rule ~head [~@body]))))

(defmacro ?-
  "Define a datalog query"
  [& q]
  (let [qq (bacwn.datalog.impl.literals/build-atom q :bacwn.datalog.impl.literals/literal)]
    `(with-meta ~qq {:type :bacwn.datalog.impl.rules/datalog-query})))
