;;  Copyright (c) Jeffrey Straszheim. All rights reserved.  The use and
;;  distribution terms for this software are covered by the Eclipse Public
;;  License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can
;;  be found in the file epl-v10.html at the root of this distribution.  By
;;  using this software in any fashion, you are agreeing to be bound by the
;;  terms of this license.  You must not remove this notice, or any other,
;;  from this software.
;;
;;  test-literals.clj
;;
;;  A Clojure implementation of Datalog -- Literals tests
;;
;;  straszheimjeffrey (gmail)
;;  Created 25 Feburary 2009


(ns bacwn.test.impl.test-literals
  (:use-macros [cemerick.cljs.test :only (is deftest with-test run-tests testing)]
               [fogus.datalog.bacwn.macros :only (<- ?- make-database)])
  (:use [fogus.datalog.bacwn.impl.literals :only [build-literal literal-predicate literal-columns literal-vars positive-vars negative-vars
                                                  negated? get-vs-from-cs literal-appropriate? adorned-literal get-adorned-bindings get-cs-from-vs
                                                  get-base-predicate magic-literal join-literal project-literal]]
        [fogus.datalog.bacwn.impl.database :only [add-tuples datalog-relation]]))

(def pl (fogus.datalog.bacwn.impl.literals/->AtomicLiteral :fred {:z 3, :y (quote ?y), :x (quote ?x)} :fogus.datalog.bacwn.impl.literals/literal))
(def nl (fogus.datalog.bacwn.impl.literals/->AtomicLiteral :fred {:z 3, :y (quote ?y), :x (quote ?x)} :fogus.datalog.bacwn.impl.literals/negated))
(def cl (fogus.datalog.bacwn.impl.literals/->ConditionalLiteral (clojure.core/fn [binds__5075__auto__] (clojure.core/apply > binds__5075__auto__)) (quote >) (quote (?x 3)) :fogus.datalog.bacwn.impl.literals/conditional))

(def bl (fogus.datalog.bacwn.impl.literals/->AtomicLiteral :fred nil :fogus.datalog.bacwn.impl.literals/literal))

(def bns {:x '?x :y '?y :z 3})

(deftest test-build-literal
  (is (= (:predicate pl) :fred))
  (is (= (:term-bindings pl) bns))
  (is (= (:predicate nl) :fred))
  (is (= (:term-bindings nl) bns))
  (is (= (:symbol cl) '>))
  (is (= (:terms cl) '(?x 3)))
  (is ((:fun cl) [4 3]))
  (is (not ((:fun cl) [2 4])))
  (is (= (:predicate bl) :fred)))

(deftest test-literal-predicate
  (is (= (literal-predicate pl) :fred))
  (is (= (literal-predicate nl) :fred))
  (is (nil? (literal-predicate cl)))
  (is (= (literal-predicate bl) :fred)))

(deftest test-literal-columns
  (is (= (literal-columns pl) #{:x :y :z}))
  (is (= (literal-columns nl) #{:x :y :z}))
  (is (nil? (literal-columns cl)))
  (is (empty? (literal-columns bl))))

(deftest test-literal-vars
  (is (= (literal-vars pl) #{'?x '?y}))
  (is (= (literal-vars nl) #{'?x '?y}))
  (is (= (literal-vars cl) #{'?x}))
  (is (empty? (literal-vars bl))))

(deftest test-positive-vars
  (is (= (positive-vars pl) (literal-vars pl)))
  (is (nil? (positive-vars nl)))
  (is (nil? (positive-vars cl)))
  (is (empty? (positive-vars bl))))

(deftest test-negative-vars
  (is (nil? (negative-vars pl)))
  (is (= (negative-vars nl) (literal-vars nl)))
  (is (= (negative-vars cl) (literal-vars cl)))
  (is (empty? (negative-vars bl))))

(deftest test-negated?
  (is (not (negated? pl)))
  (is (negated? nl))
  (is (not (negated? cl))))

(deftest test-vs-from-cs
  (is (= (get-vs-from-cs pl #{:x}) #{'?x}))
  (is (empty? (get-vs-from-cs pl #{:z})))
  (is (= (get-vs-from-cs pl #{:x :r}) #{'?x}))
  (is (empty? (get-vs-from-cs pl #{}))))

(deftest test-cs-from-vs
  (is (= (get-cs-from-vs pl #{'?x}) #{:x}))
  (is (= (get-cs-from-vs pl #{'?x '?r}) #{:x}))
  (is (empty? (get-cs-from-vs pl #{}))))

(deftest test-literal-appropriate?
  (is (not (literal-appropriate? #{} pl)))
  (is (literal-appropriate? #{'?x} pl))
  (is (not (literal-appropriate? #{'?x} nl)))
  (is (literal-appropriate? #{'?x '?y} nl))
  (is (not (literal-appropriate? #{'?z} cl)))
  (is (literal-appropriate? #{'?x} cl)))

(deftest test-adorned-literal
  (is (= (literal-predicate (adorned-literal pl #{:x}))
         {:pred :fred :bound #{:x}}))
  (is (= (literal-predicate (adorned-literal nl #{:x :y :q}))
         {:pred :fred :bound #{:x :y}}))
  (is (= (:term-bindings (adorned-literal nl #{:x}))
         {:x '?x :y '?y :z 3}))
  (is (= (adorned-literal cl #{})
         cl)))

(deftest test-get-adorned-bindings
  (is (= (get-adorned-bindings (literal-predicate (adorned-literal pl #{:x})))
         #{:x}))
  (is (= (get-adorned-bindings (literal-predicate pl))
         nil)))

(deftest test-get-base-predicate
  (is (= (get-base-predicate (literal-predicate (adorned-literal pl #{:x})))
         :fred))
  (is (= (get-base-predicate (literal-predicate pl))
         :fred)))

;(deftest test-magic-literal
;  (is (.equals (magic-literal pl)
;               {:predicate {:pred :fred :magic true}, :term-bindings {}, :literal-type :fogus.datalog.bacwn.impl.literals/literal}))
;  (is (.equals (magic-literal (adorned-literal pl #{:x}))
;               {:predicate {:pred :fred :magic true :bound #{:x}},
;                :term-bindings {:x '?x},
;                :literal-type :fogus.datalog.bacwn.impl.literals/literal})))


(def db1 (make-database
           (relation :fred [:x :y])
           (index :fred :x)
           (relation :sally [:x])))

(def db2 (add-tuples db1
             [:fred :x 1 :y :mary]
             [:fred :x 1 :y :becky]
             [:fred :x 3 :y :sally]
             [:fred :x 4 :y :joe]
             [:sally :x 1]
             [:sally :x 2]))

(def lit1 (fogus.datalog.bacwn.impl.literals/->AtomicLiteral :fred {:y (quote ?y), :x (quote ?x)} :fogus.datalog.bacwn.impl.literals/literal))
(def lit2 (fogus.datalog.bacwn.impl.literals/->AtomicLiteral :fred {:x (quote ?x)} :fogus.datalog.bacwn.impl.literals/negated))
(def lit3 (fogus.datalog.bacwn.impl.literals/->ConditionalLiteral (clojure.core/fn [binds__5075__auto__] (clojure.core/apply > binds__5075__auto__)) (quote >) (quote (?x ?y)) :fogus.datalog.bacwn.impl.literals/conditional))
(def lit4 (adorned-literal (fogus.datalog.bacwn.impl.literals/->AtomicLiteral :joan {:y (quote ?y), :x (quote ?x)} :fogus.datalog.bacwn.impl.literals/literal) #{:x}))

(deftest test-join-literal
  (is (= (set (join-literal db2 lit1 [{'?x 1} {'?x 2} {'?x 3}]))
         #{{'?x 1, '?y :mary} {'?x 1, '?y :becky} {'?x 3, '?y :sally}}))
  (is (= (join-literal db2 lit2 [{'?x 1} {'?x 2} {'?x 3}])
         [{'?x 2}]))
  (is (= (join-literal db2 lit3 [{'?x 1 '?y 2} {'?x 3 '?y 1}])
         [{'?x 3 '?y 1}])))

(deftest test-project-literal
  (is (= ((project-literal db2 lit4 [{'?x 1 '?y 3}{'?x 4 '?y 2}]) {:pred :joan :bound #{:x}})
         (datalog-relation
          ;; Schema
          #{:y :x}

          ;; Data
          #{
            {:x 1, :y 3}
            {:x 4, :y 2}
            }

          ;; Indexes
          {
           :x
           {
            4
            #{{:x 4, :y 2}}
            1
            #{{:x 1, :y 3}}
            }
           }))))
