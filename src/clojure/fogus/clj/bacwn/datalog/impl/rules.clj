;;  Copyright (c) Jeffrey Straszheim. All rights reserved.  The use and
;;  distribution terms for this software are covered by the Eclipse Public
;;  License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can
;;  be found in the file epl-v10.html at the root of this distribution.  By
;;  using this software in any fashion, you are agreeing to be bound by the
;;  terms of this license.  You must not remove this notice, or any other,
;;  from this software.
;;
;;  rules.clj
;;
;;  A Clojure implementation of Datalog -- Rules Engine
;;
;;  straszheimjeffrey (gmail)
;;  Created 2 Feburary 2009
;;  Converted to Clojure1.4 by Martin Trojer 2012.

(ns bacwn.datalog.impl.rules
  (:require [bacwn.datalog.impl.util :as util]
            [bacwn.datalog.impl.literals :as literal]
            [bacwn.datalog.impl.database :as db]
            clojure.set))

(defrecord DatalogRule [head body])

(defn display-rule
  "Return the rule in a readable format."
  [rule]
  (list* '<-
         (-> rule :head literal/display-literal)
         (map literal/display-literal (:body rule))))

(defn display-query
  "Return a query in a readable format."
  [query]
  (list* '?- (literal/display-literal query)))

;; =============================
;; Check rule safety

(defn is-safe?
  "Is the rule safe according to the datalog protocol?"
  [rule]
  (let [hv (literal/literal-vars (:head rule))
        bpv (apply clojure.set/union (map literal/positive-vars (:body rule)))
        bnv (apply clojure.set/union (map literal/negative-vars (:body rule)))
        ehv (clojure.set/difference hv bpv)
        env (clojure.set/difference bnv bpv)]
    (when-not (empty? ehv)
      (throw (Exception. (str "Head vars" ehv "not bound in body of rule" rule))))
    (when-not (empty? env)
      (throw (Exception. (str "Body vars" env "not bound in negative positions of rule" rule))))
    rule))

;; =============================
;; Rule creation and printing

(defn build-rule
  [hd bd]
  (with-meta (->DatalogRule hd bd) {:type ::datalog-rule}))

(defmethod print-method ::datalog-rule
  [rule ^java.io.Writer writer]
  (print-method (display-rule rule) writer))

(defn return-rule-data
  "Returns an untypted rule that will be fully printed"
  [rule]
  (with-meta rule {}))

(defmethod print-method ::datalog-query
  [query ^java.io.Writer writer]
  (print-method (display-query query) writer))

;; =============================
;; SIP

(defn compute-sip
  "Given a set of bound column names, return an adorned sip for this
   rule.  A set of intensional predicates should be provided to
   determine what should be adorned."
  [bindings i-preds rule]
  (let [next-lit (fn [bv body]
                   (or (first (drop-while
                               #(not (literal/literal-appropriate? bv %))
                               body))
                       (first (drop-while (complement literal/positive?) body))))
        adorn (fn [lit bvs]
                (if (i-preds (literal/literal-predicate lit))
                  (let [bnds (clojure.set/union (literal/get-cs-from-vs lit bvs)
                                                (literal/get-self-bound-cs lit))]
                    (literal/adorned-literal lit bnds))
                  lit))
        new-h (literal/adorned-literal (:head rule) bindings)]
    (loop [bound-vars (literal/get-vs-from-cs (:head rule) bindings)
           body (:body rule)
           sip []]
      (if-let [next (next-lit bound-vars body)]
        (recur (clojure.set/union bound-vars (literal/literal-vars next))
               (remove #(= % next) body)
               (conj sip (adorn next bound-vars)))
        (build-rule new-h (concat sip body))))))

;; =============================
;; Rule sets

(defn make-rules-set
  "Given an existing set of rules, make it a 'rules-set' for
   printing."
  [rs]
  (with-meta rs {:type ::datalog-rules-set}))

(def empty-rules-set (make-rules-set #{}))

(defn rules-set
  "Given a collection of rules return a rules set"
  [& rules]
  (reduce conj empty-rules-set rules))

(defmethod print-method ::datalog-rules-set
  [rules ^java.io.Writer writer]
  (binding [*out* writer]
    (do
      (print "(rules-set")
      (doseq [rule rules]
        (println)
        (print "   ")
        (print rule))
      (println ")"))))

(defn predicate-map
  "Given a rules-set, return a map of rules keyed by their predicates.
   Each value will be a set of rules."
  [rs]
  (let [add-rule (fn [m r]
                   (let [pred (-> r :head literal/literal-predicate)
                         os (get m pred #{})]
                     (assoc m pred (conj os r))))]
    (reduce add-rule {} rs)))

(defn all-predicates
  "Given a rules-set, return all defined predicates"
  [rs]
  (set (map literal/literal-predicate (map :head rs))))

(defn non-base-rules
  "Return a collection of rules that depend, somehow, on other rules"
  [rs]
  (let [pred (all-predicates rs)
        non-base (fn [r]
                   (if (some #(pred %)
                             (map literal/literal-predicate (:body r)))
                     r
                     nil))]
    (remove nil? (map non-base rs))))

;; =============================
;; Database operations

(def empty-bindings [{}])

(defn apply-rule
  "Apply the rule against db-1, adding the results to the appropriate
   relation in db-2.  The relation will be created if needed."
  ([db rule] (apply-rule db db rule))
  ([db-1 db-2 rule]
     (let [head (:head rule)
           body (:body rule)
           step (fn [bs lit]
                  (literal/join-literal db-1 lit bs))
           bs (reduce step empty-bindings body)]
       (literal/project-literal db-2 head bs))))

(defn apply-rules-set
  [db rs]
  (reduce (fn [rdb rule]
            (apply-rule db rdb rule)) db rs))