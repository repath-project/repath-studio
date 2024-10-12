(ns renderer.tool.db
  (:require [renderer.tool.hierarchy :as hierarchy]))

(defn tool?
  [tool]
  (isa? tool ::hierarchy/tool))

(def Tool
  [:fn {:error/fn (fn [{:keys [value]} _] (str value " is not a supported tool"))}
   tool?])
