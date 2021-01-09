(ns segue.wrappers
  (:require [cljs.nodejs :as nodejs]))

(def fs (nodejs/require "fs"))

(defn node-slurp [path]
  (.readFileSync ^js fs path "utf8"))

(defn node-write [path content]
  (.writeFileSync ^js fs path content "utf8"))
