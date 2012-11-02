(ns bacwn.datalwg
  (:use [bacwn.datalog.impl.database :only (make-database add-tuples)]))

(defn explode
  "Convert a map into a clj-Datalog tuple vector. Brittle, but
   works along the happy path."
  [entity]
  (let [relation-type (-> entity seq ffirst namespace keyword)
        id-key (keyword (name relation-type) "db.id")
        id  (get entity id-key)
        kvs (seq (dissoc entity id-key))]
    (vec
     (apply concat [relation-type id-key id]
            (reduce (fn [acc [k v]]
                      (cons [(keyword (name k)) v] acc))
                    []
                    kvs)))))

(defmacro facts [db & tuples]
  `(add-tuples ~db
    ~@(map explode tuples)))

(defn q
  [query db rules bindings]
  (run-work-plan
   (build-work-plan rules query)
   db
   bindings))

;; printing

(defmethod print-method :bacwn.datalog.impl.database/datalog-database
  [db ^java.io.Writer writer]
  (binding [*out* writer]
    (do
      (println "(datalog-database")
      (println "{")
      (doseq [key (keys db)]
        (println)
        (println key)
        (print-method (db key) writer))
      (println "})"))))

(defmethod print-method :bacwn.datalog.impl.database/datalog-relation
  [rel ^java.io.Writer writer]
  (binding [*out* writer]
    (do
      (println "(datalog-relation")
      (println " ;; Schema")
      (println " " (:schema rel))
      (println)
      (println " ;; Data")
      (println " #{")
      (doseq [tuple (:data rel)]
        (println "  " tuple))
      (println " }")
      (println)
      (println " ;; Indexes")
      (println "  {")
      (doseq [key (-> rel :indexes keys)]
        (println "  " key)
        (println "    {")
        (doseq [val (keys ((:indexes rel) key))]
          (println "      " val)
          (println "        " (get-in rel [:indexes key val])))
        (println "    }"))
      (println "  })"))))

