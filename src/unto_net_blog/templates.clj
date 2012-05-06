(ns unto-net-blog.templates
  (:require [clojure.string :as string]))

(defn build-replacer [key value]
  "Takes a key value pair and returns a function that when applied
   will replace all instances of {{ key }} with value in a string."
  (let [k (if (keyword? key) (name key) (str key))
        re (re-pattern (str "\\{\\{\\s*" k "\\s*\\}\\}"))
        v (java.util.regex.Matcher/quoteReplacement (str value))]
    (fn [s] (clojure.string/replace s re v))))

(defn build-composite-replacer [data]
  "Takes a map of key value pairs and returns a function that will
  replace all instances of {{ key }} with value in a string"
  (apply comp (for [[key, value] data] (build-replacer key value))))

(defn interpolate [template data]
  "Replace all instances of {{ key }} in s with their values."
  ((build-composite-replacer data) template))
