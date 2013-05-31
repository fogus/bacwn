(ns clojure.test
  (:require cljs.compiler
            [cljs.analyzer :refer (*cljs-ns* get-expander)]
            [clojure.template :as temp]))

;; TODO seems like there's no reason to expose this for cljs; you're not
;; likely to be shipping .cljs files into production
(defonce ^:dynamic ^:private
  ^{:doc "True by default.  If set to false, no test functions will
   be created by deftest, set-test, or with-test.  Use this to omit
   tests when compiling or loading production code."
    :added "1.1"}
  *load-tests* true)

(defmacro with-test-out
  "Runs body with *print-fn* bound to the value of *test-print-fn* is bound non-nil."
  {:added "1.1"}
  [& body]
  `(binding [cljs.core/*print-fn* (or *test-print-fn* cljs.core/*print-fn*)]
     ~@body))

;;; UTILITIES FOR ASSERTIONS

(def ^:private ^:dynamic *cljs-env*
  "The current ClojureScript compilation environment, necessary
to determine if a symbol refers to a macro (or not) when determining
whether to use assert-predicate or not."
  nil)

(defn function?
  "Returns true if argument is a function or a symbol that resolves to
  a function (not a macro)."
  {:added "1.1"}
  [x]
  (and (symbol? x)
    (not (.startsWith (name x) "."))
    (not (get-expander x *cljs-env*))))

(defn assert-predicate
  "Returns generic assertion code for any functional predicate.  The
  'expected' argument to 'report' will contains the original form, the
  'actual' argument will contain the form with all its sub-forms
  evaluated.  If the predicate returns false, the 'actual' form will
  be wrapped in (not...)."
  {:added "1.1"}
  [msg form]
  (let [args (rest form)
        pred (first form)]
    `(let [values# (list ~@args)
           result# (apply ~pred values#)]
       (if result#
         (.log js/console "OK")
         (.log js/console {:type :fail, :message ~msg,
                  :expected '~form, :actual (list '~'not (cons '~pred values#))}))
       result#)))

(defn assert-any
  "Returns generic assertion code for any test, including macros, Java
  method calls, or isolated symbols."
  {:added "1.1"}
  [msg form]
  `(let [value# ~form]
     (if value#
       (.log js/console "OK")
       (.log js/console {:type :fail, :message ~msg,
                :expected '~form, :actual value#}))
     value#))



;;; ASSERTION METHODS

;; You don't call these, but you can add methods to extend the 'is'
;; macro.  These define different kinds of tests, based on the first
;; symbol in the test expression.

(defmulti assert-expr
  (fn [msg form]
    (cond
      (nil? form) :always-fail
      (seq? form) (first form)
      :else :default)))

(defmethod assert-expr :always-fail [msg form]
  ;; nil test: always fail
  `(.log js/console {:type :fail, :message ~msg}))

(defmethod assert-expr :default [msg form]
  (if (and (seq? form) (function? (first form)))
    (assert-predicate msg form)
    (assert-any msg form)))

(defmethod assert-expr 'instance? [msg form]
  ;; Test if x is an instance of y.
  `(let [object# ~(nth form 2)]
     (let [result# (instance? ~(nth form 1) object#)]
       (if result#
         (.log js/console "OK")
         (.log js/console {:type :fail, :message ~msg,
                  :expected '~form, :actual (type object#)}))
       result#)))

(defmethod assert-expr 'thrown? [msg form]
  ;; (is (thrown? c expr))
  ;; Asserts that evaluating expr throws an exception of class c.
  ;; Returns the exception thrown.
  (let [klass (second form)
        body (nthnext form 2)]
    `(try ~@body
          (.log js/console {:type :fail, :message ~msg,
                   :expected '~form, :actual nil})
          (~'catch ~klass e#
            (.log js/console "OK")
            e#))))

(defmethod assert-expr 'thrown-with-msg? [msg form]
  ;; (is (thrown-with-msg? c re expr))
  ;; Asserts that evaluating expr throws an exception of class c.
  ;; Also asserts that the message string of the exception matches
  ;; (with re-find) the regular expression re.
  (let [klass (nth form 1)
        re (nth form 2)
        body (nthnext form 3)]
    `(try ~@body
          (.log js/console {:type :fail, :message ~msg, :expected '~form, :actual nil})
          (~'catch ~klass e#
            (let [m# (.-message e#)]
              (if (re-find ~re m#)
                (.log js/console "OK")
                (.log js/console {:type :fail, :message ~msg,
                         :expected '~form, :actual e#})))
            e#))))


(defmacro try-expr
  "Used by the 'is' macro to catch unexpected exceptions.
  You don't call this."
  {:added "1.1"}
  [msg form]
  `(try ~(binding [*cljs-env* &env]
           (assert-expr msg form))
        (~'catch js/Error t#
          (.log js/console {:type :error, :message ~msg,
                      :expected '~form, :actual t#}))))



;;; ASSERTION MACROS

;; You use these in your tests.

(defmacro is
  "Generic assertion macro.  'form' is any predicate test.
  'msg' is an optional message to attach to the assertion.

  Example: (is (= 4 (+ 2 2)) \"Two plus two should be 4\")

  Special forms:

  (is (thrown? c body)) checks that an instance of c is thrown from
  body, fails if not; then returns the thing thrown.

  (is (thrown-with-msg? c re body)) checks that an instance of c is
  thrown AND that the message on the exception matches (with
  re-find) the regular expression re."
  {:added "1.1"}
  ([form] `(is ~form nil))
  ([form msg] `(try-expr ~msg ~form)))

(defmacro are
  "Checks multiple assertions with a template expression.
  See clojure.template/do-template for an explanation of
  templates.

  Example: (are [x y] (= x y)
                2 (+ 1 1)
                4 (* 2 2))
  Expands to:
           (do (is (= 2 (+ 1 1)))
               (is (= 4 (* 2 2))))

  Note: This breaks some reporting features, such as line numbers."
  {:added "1.1"}
  [argv expr & args]
  (if (or
       ;; (are [] true) is meaningless but ok
       (and (empty? argv) (empty? args))
       ;; Catch wrong number of args
       (and (pos? (count argv))
            (pos? (count args))
            (zero? (mod (count args) (count argv)))))
    `(temp/do-template ~argv (is ~expr) ~@args)
    (throw (IllegalArgumentException. "The number of args doesn't match are's argv."))))

(defmacro testing
  "Adds a new string to the list of testing contexts.  May be nested,
  but must occur inside a test function (deftest)."
  {:added "1.1"}
  [string & body]
  `(binding [*testing-contexts* (conj *testing-contexts* ~string)]
     ~@body))

;;; DEFINING TESTS

(defn- munged-symbol
  [& strs]
  (symbol (cljs.compiler/munge (apply str strs))))

;; metadata on functions (esp top level fns?) in cljs is bizarre;
;; the result of with-meta isn't a js/Function, but it is an IFn?
;; TODO might be better to just define another top-level and register
;; that instead
(defmacro set-test
  [name & body]
  (when *load-tests*
    `(do
       (set! ~name (vary-meta ~name assoc
                              :name '~name
                              :test (fn ~(symbol (str name "-test")) [] ~@body)))
       (register-test! '~*cljs-ns* ~(munged-symbol *cljs-ns* "." name))
       ~name)))

(defmacro with-test
  "Takes any definition form (that returns a Var) as the first argument.
  Remaining body goes in the :test metadata function for that Var.

  When *load-tests* is false, only evaluates the definition, ignoring
  the tests."
  {:added "1.1"}
  [definition & body]
  `(do ~definition (set-test ~(second definition) ~@body)))

(defmacro deftest
  "Defines a test function with no arguments.  Test functions may call
  other tests, so tests may be composed.  If you compose tests, you
  should also define a function named test-ns-hook; run-tests will
  call test-ns-hook instead of testing all vars.

  Note: Actually, the test body goes in the :test metadata on the var,
  and the real function (the value of the var) calls test-var on
  itself.

  When *load-tests* is false, deftest is ignored."
  {:added "1.1"}
  [n & body]
  (let [n (name n)]
    `(do
       (.log js/console (str "Testing " ~n))
       ~@body)))

(defmacro deftest-
  "Like deftest but creates a private var."
  {:added "1.1"}
  [name & body]
  `(deftest ~(vary-meta name assoc :private true) ~@body))

(defmacro deftesthook
  [name & body]
  `(do
     (defn ~name ~@body)
     (register-test-ns-hook! '~*cljs-ns* ~(munged-symbol *cljs-ns* "." name))
     ~name))

;;; DEFINING FIXTURES

(defmacro use-fixtures
  "Wrap test runs in a fixture function to perform setup and
  teardown. Using a fixture-type of :each wraps every test
  individually, while :once wraps the whole run in a single function."
  [fixture-type & args]
  `(register-fixtures! '~(munged-symbol *cljs-ns*) ~fixture-type ~@args))

;;; RUNNING TESTS; (many more options available in test.cljs)

(defmacro run-tests
  "Runs all tests in the given namespaces; prints results.
  Defaults to current namespace if none given.  Returns a map
  summarizing test results."
  {:added "1.1"}
  ([] `(run-tests* '~*cljs-ns*))
  ([& namespaces] `(run-tests* ~@namespaces)))