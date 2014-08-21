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
;; For reference: http://www.chartgeek.com/star-wars-family-tree/
(defonce sw-characters
  (tr/db-init "characters" db-base
              [:parent :ancestor "Shmi Skywalker" :child "Anakin Skywalker"]
              [:parent :ancestor "Ruwee Naberrie" :child "Padme Amidala"]
              [:parent :ancestor "Jorbal Naberrie" :child "Padme Amidala"]
              [:parent :ancestor "Cliegg Lars" :child "Owen Lars"]
              [:parent :ancestor "Ownen Lars" :child "Luke Skywalker"]
              [:parent :ancestor "Beru Lars" :child "Luke Skywalker"]
              [:parent :ancestor "Luke Skywalker" :child "Ben Skywalker"]
              [:parent :ancestor "Mara Jade" :child "Ben Skywalker"]
              [:parent :ancestor "Anakin Skywalker" :child "Luke Skywalker"]
              [:parent :ancestor "Padme Amidala" :child "Luke Skywalker"]
              [:parent :ancestor "Anakin Skywalker" :child "Princess Leia"]
              [:parent :ancestor "Padme Amidala" :child "Princess Leia"]
              [:parent :ancestor "Bail Organa" :child "Princess Leia"]
              [:parent :ancestor "Breha Organa" :child "Princess Leia"]
              [:parent :ancestor "Princess Leia" :child "Jaina Solo"]
              [:parent :ancestor "Princess Leia" :child "Jacen Solo"]
              [:parent :ancestor "Princess Leia" :child "Anakin Solo"]
              [:parent :ancestor "Han Solo" :child "Jaina Solo"]
              [:parent :ancestor "Han Solo" :child "Jacen Solo"]
              [:parent :ancestor "Han Solo" :child "Anakin Solo"]

              [:female :person "Shmi Skywalker"]
              [:female :person "Jorbal Naberrie"]
              [:female :person "Beru Lars"]
              [:female :person "Mara Jade"]
              [:female :person "Padme Amidala"]
              [:female :person "Breha Organa"]
              [:female :person "Princess Leia"]
              [:female :person "Jaina Solo"]

              [:male :person "Cliegg Lars"]
              [:male :person "Owen Lars"]
              [:male :person "Ruwee Naberrie"]
              [:male :person "Anakin Skywalker"]
              [:male :person "Bail Organa"]
              [:male :person "Ben Skywalker"]
              [:male :person "Han Solo"]
              [:male :person "Jacen Solo"]
              [:male :person "Anakin Solo"]))

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
       (:parent :ancestor ?z :child ?y)
       (:parent :ancestor ?x :child ?z)
       (:male :person ?x))))

(def wp-1 (bacwn/build-work-plan rules (?- :father :dad ?x :child '??name)))
(def wp-2 (bacwn/build-work-plan rules (?- :mother :mom ?x :child '??name)))
(def wp-3 (bacwn/build-work-plan rules (?- :grandfather :gramps ?x :grandchild '??name)))

;; Find Luke's father
(bacwn/run-work-plan wp-1 sw-characters {'??name "Luke Skywalker"})

;; Find Luke's mum (step-mom too)
(bacwn/run-work-plan wp-2 sw-characters {'??name "Luke Skywalker"})

;; Luke's has only one grandpa (not an error)
(bacwn/run-work-plan wp-3 sw-characters {'??name "Luke Skywalker"})

;; another way to do queries, let's find all the father-child relations
(bacwn/q (?- :father :dad ?x :child ?y) ; ad-hoc work plan, query parameters must be quoted (i.e. '??name)
         sw-characters ; database of facts
         rules ; logic predicates (a.k.a. rules or the rules-set)
         {})


;; Now let's add more datums to our database and see how the changes are
;; reflected in the queries
(def db-new
  (tr/add-tuples! "characters" sw-characters
                  [:parent :ancestor "???" :child "Chewbacca"]
                  [:parent :ancestor "???" :child "Anakin Skywalker"]
                  [:male :person "???"]
                  [:male :person "Chewbacca"]))

;; So now we have unknown fathers in our data
(bacwn/q (?- :father :dad "???" :child ?y)
         db-new
         rules
         {})

;; Let's get rid of that and go back to the default
(def db-gone
  (tr/remove-tuples! "characters" db-new
                  [:parent :ancestor "???" :child "Chewbacca"]
                  [:parent :ancestor "???" :child "Anakin Skywalker"]
                  [:male :person "???"]
                  [:male :person "Chewbacca"]))

;; Double-check that the datums are gone
(bacwn/q (?- :father :dad "???" :child ?y)
         db-gone
         rules
         {})

;; What if we only care about one relationship and want to delete everything else?
(def db-evil
  (tr/add-tuples! "characters" db-base
                  [:parent :ancestor "???" :child "Emperor Palpatine"]
                  [:female :person "???"]
                  [:male :person "???"]
                  [:male :person "Emperor Palpatine"]))

(bacwn/q (?- :mother :mom ?x :child ?y)
         db-evil
         rules
         {})

;; Try reloading the page and evalute the default the template (sw-characters) from above.
;; This smaller database should now be loaded into memory instead of the template.
(bacwn/q (?- :crazy-evil-villains :villain ?x)
         sw-characters
         (r/rules-set (<- (:crazy-evil-villains :villain ?x)
                          (:male :person ?x)
                          (if = ?x "Emperor Palpatine")))
         {})
