(ns cljs.bacwn.datalog.macros)

#_(defmacro make-database
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
                     `(cljs.bacwn.datalog.impl.database/add-relation ~cur ~(first body) ~(fnext body))
                     (= cmd 'index)
                     `(cljs.bacwn.datalog.impl.database/add-index ~cur ~(first body) ~(fnext body))
                     :otherwise (throw (Exception. (str new "not recognized"))))))]
    (reduce wrapper `cljs.bacwn.datalog.impl.database/empty-database commands)))

#_(defmacro <-
  "Build a datalog rule.  Like this:

   (<- (:head :x ?x :y ?y) (:body-1 :x ?x :y ?y) (:body-2 :z ?z) (not! :body-3 :x ?x) (if > ?y ?z))"
  [hd & body]
  (let [head (cljs.bacwn.datalog.impl.literals/build-atom hd :bacwn.datalog.impl.literals/literal)
        body (map cljs.bacwn.datalog.impl.literals/build-literal body)]
    `(is-safe? (cljs.bacwn.datalog.impl.rules/build-rule ~head [~@body]))))

#_(defmacro ?-
  "Define a datalog query"
  [& q]
  (let [qq (cljs.bacwn.datalog.impl.literals/build-atom q :bacwn.datalog.impl.literals/literal)]
    `(with-meta ~qq {:type :bacwn.datalog.impl.rules/datalog-query})))
