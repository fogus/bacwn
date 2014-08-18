(ns family
  (:use-macros [fogus.datalog.bacwn.macros :only (<- ?- make-database)])
  (:require [fogus.datalog.bacwn :as bacwn]
            [fogus.datalog.bacwn.impl.rules :as r]
            [fogus.datalog.bacwn.impl.database :as database]
            [fogus.datalog.bacwn.trans.read :as tr]))

;; Create a new clientside database to store bacwn datums.
(defonce fams
  (tr/db-new "fams"))

;; Do a bacwn schema like normal.
(def db-base
  (make-database
   (relation :parent [:ancestor :child])
   (index :parent :child)

   (relation :male [:person])
   (index :male :person)

   (relation :female [:person])
   (index :female :person)))

;; This serves as a default set of datums. Any subsequent additions or retractions
;; will be saved and override this set of datums. This allows a program to store
;; its info persistently!
(def db
  (tr/db-init "biblical" db-base
              [:parent :ancestor "terach" :child "abraham"]
              [:parent :ancestor "terach" :child "nachor"]
              [:parent :ancestor "terach" :child "haran"]
              [:parent :ancestor "abraham" :child "isaac"]
              [:parent :ancestor "haran" :child "lot"]
              [:parent :ancestor "haran" :child "milcah"]
              [:parent :ancestor "haran" :child "yiscah"]
              [:parent :ancestor "sarah" :child "isaac"]

              [:female :person "sarah"]
              [:female :person "milcah"]
              [:female :person "yiscah"]

              [:male :person "terach"]
              [:male :person "abraham"]
              [:male :person "nachor"]
              [:male :person "haran"]
              [:male :person "isaac"]
              [:male :person "lot"]))

;; The rest is normal bacwn.
(def rules
  (r/rules-set
   (<- (:father :dad ?x :child ?y)
       (:parent :ancestor ?x :child ?y)
       (:male :person ?x))
   (<- (:mother :mom ?x :child ?y)
       (:parent :ancestor ?x :child ?y)
       (:female :person ?x))
   (<- (:grandpa :gramps ?x :grandchild ?y)
       (:father :dad ?z :child ?y)
       (:parent :ancestor ?x :child ?z)
       (:male :person ?x))))

(def wp-1 (bacwn/build-work-plan rules (?- :father :dad ?x :child '??name)))
(def wp-2 (bacwn/build-work-plan rules (?- :mother :mom ?x :child '??name)))
(def wp-3 (bacwn/build-work-plan rules (?- :grandpa :gramps ?x :grandchild '??name)))

;; Find haran's father
(bacwn/run-work-plan wp-1 db {'??name "milcah"})

;; Find isaac's mum
(bacwn/run-work-plan wp-2 db {'??name "isaac"})

(bacwn/run-work-plan wp-3 db {'??name "isaac"})

;; another way to do queries, let's find all the father-child relations
(bacwn/q (?- :father :dad ?x :child ?y) ; ad-hoc work plan, query parameters must be quoted (i.e. '??name)
         db ; database of facts
         rules ; logic predicates (a.k.a. rules or the rules-set)
         {})
