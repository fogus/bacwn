(ns fogus.datalog.bacwn
  (:require [fogus.datalog.bacwn.impl.database :as db]
            [fogus.datalog.bacwn.impl.rules :as rules]
            [fogus.datalog.bacwn.impl.softstrat :as soft]
            clojure.set))

(def ID_KEY :db.id)
(def ^:private nums (atom 0))

(defn- explode
  "Convert a map into a clj-Datalog tuple vector. Brittle, but
   works along the happy path."
  [entity]
  (let [relation-type (-> entity seq ffirst namespace keyword)
        id-key (keyword (name relation-type) "db.id")
        id  (get entity id-key)
        id  (if id id (swap! nums inc))
        kvs (seq (dissoc entity id-key))]
    (vec
     (apply concat [relation-type :db.id id]
            (reduce (fn [acc [k v]]
                      (cons [(keyword (name k)) v] acc))
                    []
                    kvs)))))

(defrecord WorkPlan
  [work-plan        ; The underlying structure
   rules            ; The original rules
   query            ; The original query
   work-plan-type]) ; The type of plan

(defn- validate-work-plan
  "Ensure any top level semantics are not violated"
  [work-plan database]
  (let [common-relations (-> work-plan :rules (clojure.set/intersection (-> database keys set)))]
    (when (-> common-relations
              empty?
              not)
      (throw (Exception. (str "The rules and database define the same relation(s):" common-relations))))))

(defn build-work-plan
  "Given a list of rules and a query, build a work plan that can be
   used to execute the query."
  [rules query]
  (->WorkPlan (soft/build-soft-strat-work-plan rules query) rules query ::soft-stratified))

(defn run-work-plan
  "Given a work plan, a database, and some query bindings, run the
   work plan and return the results."
  [work-plan database query-bindings]
  (validate-work-plan work-plan database)
  (soft/evaluate-soft-work-set (:work-plan work-plan) database query-bindings))

(defmacro facts [db & tuples]
  `(db/add-tuples ~db
    ~@(map explode tuples)))

;; querying

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

;; WiP

(defn agg [tuples]
  (group-by (comp keyword namespace second) tuples))

(defn propagate [agg]
  (apply concat
         (for [[k v] agg]
           (map #(vec (cons k %)) v))))

(defn shuffle-tuples [tups]
  (let [ids (atom {})]
    (map (fn [[nspace id prop val]]
           [nspace
            ID_KEY (get (swap! ids
                               (fn [m]
                                 (if-let [i (get m id)]
                                   m
                                   (let  [i (swap! nums inc)]
                                     (assoc m id i)))))
                        id)
            (keyword (name prop)) val])
         tups)))

(comment

  (def tom {:character/name "Tom Servo" :character/human? false})
  
  (explode tom)
  ;;=> [:character :db.id 0 :human? true :name "Joel"]
 
  (def tuples
   '[[#bacwn/id :joel, :character/name   "Joel"]
     [#bacwn/id :joel, :character/human? true]
     [#bacwn/id :joel, :person/age       42]
     [#bacwn/id :crow, :character/name   "Crow"]])

  (defn normalize [tuples]
    (mapcat (fn [segment]
              (if (map? segment)
                [(explode segment)]
                [segment]))
            tuples))

  (normalize (conj tuples tom))
  (split-at 3 (first (normalize [tom])))
  (shuffle-tuples (propagate (agg tuples)))
  
  (->> tuples
       normalize
       agg
       propagate
       shuffle-tuples
       (sort-by #(nth % 2)))

    (->> (conj tuples tom)
       normalize
       agg
       propagate
       shuffle-tuples
       (sort-by #(nth % 2)))
)

