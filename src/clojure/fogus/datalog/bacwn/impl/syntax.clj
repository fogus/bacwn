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
                (split-tuple (explode segment))
                [segment]))
            tuples))

  (defn split-tuple [tuple]
    (let [[head tails] (split-at 3 tuple)]
      (map #(vec (concat head %)) (partition 2 tails))))
  
  (split-tuple (first (normalize [tom])))

  (explode tom)
  (normalize (conj tuples tom))
  
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


