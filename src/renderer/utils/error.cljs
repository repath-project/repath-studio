(ns renderer.utils.error
  "https://react.dev/reference/react/Component#catching-rendering-errors-with-an-error-boundary"
  (:require
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.history.events :as-alias history.e]
   [renderer.window.events :as-alias window.e]))

(defn submit-error-url
  [message]
  (str "https://github.com/repath-project/repath-studio/issues/new?"
       "&title=" message
       "&template=bug_report.md"))

(defn message
  [error]
  (let [message (if (string? @error) @error (.-message @error))
        stack (when-not (string? @error) (.-stack @error))]
    [:div.flex.w-full.h-full.justify-center.items-center
     [:div.w-full.max-w-md.bg-primary.p-8.m-2
      [:div.text-xl.pr-10.pb-5 "The following unhandled error was thrown"]

      [:p.text-error message]
      (when stack
        [:details.mb-5
         [:summary "Stacktrace"]
         [:pre.border.mt-2.p-2.border-default.text-wrap.text-2xs stack]])

      [:p "Please consider submitting an error report to improve your experience."]

      [:button.button.px-2.rounded.w-full.mb-5.border.border-default.hover:bg-transparent
       {:on-click #(rf/dispatch [::window.e/open-remote-url (submit-error-url message)])}
       "Submit an error report"]

      [:p "You can try to undo your last action in order to recover to a previous working state."]

      [:button.button.px-2.rounded.w-full.mb-5.overlay
       {:on-click #(do (rf/dispatch [::history.e/undo])
                       (reset! error nil))}
       "Undo your last action"]

      [:p "If undoing did't work, you can try restarting the application."]

      [:button.button.px-2.rounded.w-full.mb-5.overlay
       {:on-click #(rf/dispatch [::window.e/relaunch])}
       "Restart the application"]

      [:p "If you keep getting tha same error after restarting, try clearing your local storage and restart.
            Please note that by doing so, you will loose any unsaved changes, and also your local app settings."]

      [:button.button.px-2.rounded.w-full.warning
       {:on-click #(rf/dispatch [::window.e/clear-local-storage-and-relaunch])}
       "Clear local storage and restart"]]]))

(defn boundary
  []
  (let [error (ra/atom nil)]
    (ra/create-class
     {;; Try to revert to a working state
      ;; https://react.dev/reference/react/Component#static-getderivedstatefromerror
      :get-derived-state-from-error
      #(reset! error %)

      :reagent-render
      (fn [children]
        (if @error
          [message error]
          children))})))
