(ns renderer.core
  (:require
   #_["@sentry/electron/renderer" :as sentry-electron-renderer]
   #_["@sentry/react" :as sentry-react]
   ["electron-log/renderer"]
   ["mdn-data" :as mdn] ;; deprecating in favor of w3c/webref
   ["paper" :refer [paper]]
   [devtools.core :as devtools]
   [platform :as platform]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [reagent.dom :as ra.dom]
   [renderer.attribute.core]
   [renderer.dialog.events]
   [renderer.dialog.subs]
   [renderer.document.events]
   [renderer.document.subs]
   [renderer.element.events]
   [renderer.element.subs]
   [renderer.events]
   [renderer.frame.events]
   [renderer.frame.subs]
   [renderer.history.events]
   [renderer.history.subs]
   [renderer.notification.events]
   [renderer.notification.subs]
   [renderer.reepl.replumb :as replumb]
   [renderer.ruler.subs]
   [renderer.snap.events]
   [renderer.snap.subs]
   [renderer.subs]
   [renderer.theme.events :as theme.e]
   [renderer.theme.subs]
   [renderer.timeline.events]
   [renderer.timeline.subs]
   [renderer.tool.core]
   [renderer.tree.events]
   [renderer.utils.dom :as dom]
   [renderer.utils.error :as error]
   [renderer.utils.keyboard :as keyb]
   [renderer.views :as v]
   [renderer.window.events :as window.e]
   [renderer.window.subs]
   [renderer.worker.events]
   [renderer.worker.subs]
   [replumb.repl :as repl]
   [shadow.cljs.bootstrap.browser :as bootstrap]
   [user]))

(def easter-egg "
██████╗░███████╗██████╗░░█████╗░████████╗██╗░░██╗
██╔══██╗██╔════╝██╔══██╗██╔══██╗╚══██╔══╝██║░░██║
██████╔╝█████╗░░██████╔╝███████║░░░██║░░░███████║
██╔══██╗██╔══╝░░██╔═══╝░██╔══██║░░░██║░░░██╔══██║
██║░░██║███████╗██║░░░░░██║░░██║░░░██║░░░██║░░██║
╚═╝░░╚═╝╚══════╝╚═╝░░░░░╚═╝░░╚═╝░░░╚═╝░░░╚═╝░░╚═╝

░██████╗████████╗██╗░░░██╗██████╗░██╗░█████╗░
██╔════╝╚══██╔══╝██║░░░██║██╔══██╗██║██╔══██╗
╚█████╗░░░░██║░░░██║░░░██║██║░░██║██║██║░░██║
░╚═══██╗░░░██║░░░██║░░░██║██║░░██║██║██║░░██║
██████╔╝░░░██║░░░╚██████╔╝██████╔╝██║╚█████╔╝
╚═════╝░░░░╚═╝░░░░╚═════╝░╚═════╝░╚═╝░╚════╝░")

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (let [root-el (dom/root-element)]
    (ra.dom/unmount-component-at-node root-el)
    (ra.dom/render [error/boundary [v/root]] root-el)))

(defn bootstrap-cb
  []
  (replumb/run-repl "(in-ns 'user)" identity)
  (print "Welcome to your REPL!")
  (print "")
  (print "You can create or modify shapes using the command line.")
  (print "Type (help) to see a list of commands."))

(defn register-ipc-on-events!
  []
  (doseq
   [[channel f]
    [["window-maximized" #(rf/dispatch [::window.e/set-maximized true])]
     ["window-unmaximized" #(rf/dispatch [::window.e/set-maximized false])]
     ["window-entered-fullscreen" #(rf/dispatch [::window.e/set-fullscreen true])]
     ["window-leaved-fullscreen" #(rf/dispatch [::window.e/set-fullscreen false])]
     ["window-minimized" #(rf/dispatch [::window.e/set-minimized true])]]]
    (js/window.api.on channel f)))

(defn handle-system-theme!
  []
  (let [query (.matchMedia js/window "(prefers-color-scheme: dark)")
        get-theme (fn [query] (if (.-matches query) :dark :light))]
    (rf/dispatch [::theme.e/set-native-mode (get-theme query)])
    (.addListener query #(rf/dispatch [::theme.e/set-native-mode (get-theme %)]))))


(defn ^:export init []
  #_(if platform/electron?
      (sentry-electron-renderer/init (clj->js config/sentry-options) sentry-react/init)
      (sentry-react/init (clj->js config/sentry-options)))

  (js/console.log (str "%c" easter-egg) "color: #e93976")

  (devtools/set-pref!
   :cljs-land-style
   (str "filter:invert(1);" (:cljs-land-style (devtools/get-prefs))))

  ;; https://code.thheller.com/blog/shadow-cljs/2017/10/14/bootstrap-support.html
  (bootstrap/init repl/st {:path "js/bootstrap" :load-on-init '[user]} bootstrap-cb)

  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch-sync [:load-local-db])
  (rf/dispatch-sync [::theme.e/init-mode])
  (rf/dispatch-sync [:set-mdn (js->clj mdn :keywordize-keys true)])

  (rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (rf/dispatch-sync [::rp/set-keydown-rules keyb/keydown-rules])

  (.addEventListener js/document "keydown" keyb/event-handler)
  (.addEventListener js/document "keyup" keyb/event-handler)

  (.addEventListener js/window "focus" #(rf/dispatch-sync [::window.e/set-focused true]))
  (.addEventListener js/window "blur" #(rf/dispatch-sync [::window.e/set-focused false]))

  (handle-system-theme!)

  (.setup paper) ; REVIEW

  (if platform/electron?
    (do (register-ipc-on-events!)
        (rf/dispatch [:load-system-fonts])
        (rf/dispatch [:load-webref]))
    (.addEventListener js/document
                       "fullscreenchange"
                       #(rf/dispatch [::window.e/set-fullscreen (boolean (.-fullscreenElement js/document))])))

  (mount-root))
