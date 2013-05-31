(ns fogus.datalog.bacwn
  (:require [fogus.datalog.bacwn.impl.database :as db]
            [fogus.datalog.bacwn.impl.rules :as rules]
            [fogus.datalog.bacwn.impl.softstrat :as soft]
            [fogus.datalog.bacwn.impl.syntax :as syntax]
            [clojure.set :as sets]))

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
      (throw (js/Error. (str "The rules and database define the same relation(s):" common-relations))))))

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

;; querying

(defn q
  [query db rules bindings]
  (run-work-plan
   (build-work-plan rules query)
   db
   bindings))
