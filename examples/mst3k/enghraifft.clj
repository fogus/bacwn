(ns bacwn.datalog.enghraifft
  (:use [bacwn.datalwg :as bacwn]
        [bacwn.datalog :only (build-work-plan run-work-plan)]
        [bacwn.datalog.impl.rules :only (<- ?- rules-set)]
        [bacwn.datalog.impl.database :only (make-database add-tuples)]
        [bacwn.datalog.impl.util :only (*trace-datalog*)]))

(def mst3k-schema
  (make-database
   (relation :character [:character/db.id :name :human?])
   (index :character :name)

   (relation :location [:location/db.id :character :name])
   (index :location :name)))

(def mst3k-db
  (-> mst3k-schema
      (facts {:character/db.id 0 :character/name "Joel" :character/human? true}
             {:character/db.id 1 :character/name "Crow" :character/human? false}
             {:character/db.id 2 :character/name "TV's Frank" :character/human? true}
             {:location/db.id  0 :location/character 0 :location/name "SoL"}
             {:location/db.id  0 :location/character 1 :location/name "SoL"}
             {:location/db.id  1 :location/character 2 :location/name "Gizmonics"})))

(def locate-rule
  (rules-set
   (<- (:stationed-at :location/name ?loc-name :character/name ?char-name)
       (:location  :name ?loc-name :character ?char)
       (:character :character/db.id ?char :name ?char-name))))

(run-work-plan
 (build-work-plan locate-rule
   (?- :stationed-at :location/name '??loc :character/name ?char-name))
 mst3k-db
 {'??loc "SoL"})
;; ({:location/name "SoL", :character/name "Crow"} {:location/name "SoL", :character/name "Joel"})


(def non-human-locate-rule
  (rules-set
   (<- (:stationed-at :location/name ?loc-name :character/name ?char-name)
       (:location  :name ?loc-name :character ?char)
       (:character :character/db.id ?char :name ?char-name)
       (not! :character :character/db.id ?char :human? true))))

(run-work-plan
 (build-work-plan non-human-locate-rule
   (?- :stationed-at :location/name '??loc :character/name ?char-name))
 mst3k-db
 {'??loc "SoL"})

(q (?- :stationed-at :location/name '??loc :character/name ?char-name)
   mst3k-db
   non-human-locate-rule
   {'??loc "SoL"})