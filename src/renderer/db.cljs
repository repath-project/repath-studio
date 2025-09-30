(ns renderer.db)

(def JS_Element
  [:fn (fn [x] (instance? js/Element x))])

(def JS_Object
  [:fn (fn [x] (instance? js/Object x))])

(def JS_Array
  [:fn (fn [x] (instance? js/Array x))])

(def JS_Promise
  [:fn (fn [x] (instance? js/Promise x))])
