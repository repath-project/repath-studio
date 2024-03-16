(ns renderer.utils.error
  "https://react.dev/reference/react/Component#catching-rendering-errors-with-an-error-boundary"
  (:require
   [re-frame.core :as rf]
   [reagent.core :as ra]))

(def error-message
  "Your last action was canceled due to the following error:")

(defn pretty-stacktrace
  [stack]
  (str "<details>
        <summary>Stacktrace</summary>"
       (.-componentStack stack)
       "</details>"))

(defn boundary
  []
  (ra/create-class
   {;;https://react.dev/reference/react/Component#componentdidcatch
    :component-did-catch
    (fn [_this error _info]
      (rf/dispatch [:notification/add
                    [:div
                     [:h2.mb-4.font-bold error-message]
                     [:p.text-error (str error)]
                     [:a.button.bg-primary.px-2.w-full
                      {:target "_blank"
                       :href (str "https://github.com/re-path/studio/issues/new?"
                                  "&title=" error
                                  "&template=bug_report.md")}
                      "Submit error report"]]]))

    ;; Try to revert to a working state
    ;; https://react.dev/reference/react/Component#static-getderivedstatefromerror
    :get-derived-state-from-error
    #(rf/dispatch-sync [:history/restore])

    :reagent-render
    (fn [children]
      children)}))
