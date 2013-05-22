;;  Copyright (c) Michael Fogus. All rights reserved.  The use and
;;  distribution terms for this software are covered by the Eclipse Public
;;  License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can
;;  be found in the file epl-v10.html at the root of this distribution.  By
;;  using this software in any fashion, you are agreeing to be bound by the
;;  terms of this license.  You must not remove this notice, or any other,
;;  from this software.
;;
;;  syntax.clj
;;
;;  A Clojure implementation of Datalog -- Support for in-memory database
;;

(ns fogus.datalog.bacwn.impl.syntax)

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

