(ns renderer.utils.error
  "https://react.dev/reference/react/Component#catching-rendering-errors-with-an-error-boundary"
  (:require
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.history.events :as-alias history.e]
   [renderer.notification.events :as-alias notification.e]
   [renderer.window.events :as-alias window.e]))

(def error-message
  "Your last action was canceled due to the following error:")

#_(defn pretty-stacktrace
    [stack]
    (str "<details>
        <summary>Stacktrace</summary>"
         (.-componentStack stack)
         "</details>"))

(defn notification
  [error]
  (let [url (str "https://github.com/repath-project/repath-studio/issues/new?"
                 "&title=" error
                 "&template=bug_report.md")]
    [:div
   [:h2.mb-4.font-bold error-message]
   [:p.text-error error]
   [:button.button.bg-primary.px-2.w-full.rounded
    {:on-click #(rf/dispatch [::window.e/open-remote-url url])}
    "Submit error report"]]))

(defn boundary
  []
  (ra/create-class
   {;;https://react.dev/reference/react/Component#componentdidcatch
    :component-did-catch
    (fn [_this error _info]
      (rf/dispatch [::notification.e/add [notification (str error)]]))

    ;; Try to revert to a working state
    ;; https://react.dev/reference/react/Component#static-getderivedstatefromerror
    :get-derived-state-from-error
    #(rf/dispatch-sync [::history.e/restore])

    :reagent-render
    (fn [children]
      children)}))
