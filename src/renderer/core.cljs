(ns renderer.core
  (:require
   ["electron-log/renderer"]
   ["paper" :refer [paper]]
   [devtools.core :as devtools]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [reagent.dom :as ra.dom]
   [renderer.app.events :as app.e]
   [renderer.app.subs]
   [renderer.app.views :as app.v]
   [renderer.attribute.impl.core]
   [renderer.color.effects]
   [renderer.dialog.events]
   [renderer.dialog.subs]
   [renderer.document.events :as document.e]
   [renderer.document.subs]
   [renderer.element.events]
   [renderer.element.impl.core]
   [renderer.element.subs]
   [renderer.frame.events]
   [renderer.frame.subs]
   [renderer.history.events]
   [renderer.history.subs]
   [renderer.notification.events]
   [renderer.notification.subs]
   [renderer.reepl.replumb :as replumb]
   [renderer.ruler.events]
   [renderer.ruler.subs]
   [renderer.snap.events]
   [renderer.snap.subs]
   [renderer.theme.effects :as theme.fx]
   [renderer.theme.events :as theme.e]
   [renderer.theme.subs]
   [renderer.timeline.events]
   [renderer.timeline.subs]
   [renderer.tool.impl.core]
   [renderer.tree.events]
   [renderer.utils.dom :as dom]
   [renderer.utils.error :as error]
   [renderer.utils.keyboard :as keyb]
   [renderer.utils.system :as system]
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

(defn ^:dev/after-load mount-root! []
  (rf/clear-subscription-cache!)
  (let [root-el (dom/root-element!)]
    (ra.dom/unmount-component-at-node root-el)
    (ra.dom/render [error/boundary [app.v/root]] root-el)))

(defn bootstrap-cb!
  []
  (replumb/run-repl "(in-ns 'user)" identity)
  (print "Welcome to your REPL!")
  (print "")
  (print "You can create or modify shapes using the command line.")
  (print "Type (help) to see a list of commands."))

(defn add-listeners!
  []
  (.addEventListener js/window "focus" (rf/dispatch [::window.e/set-focused true]))
  (.addEventListener js/window "blur" (rf/dispatch [::window.e/set-focused (dom/focused!?)]))
  (.addEventListener js/document "keydown" keyb/event-handler!)
  (.addEventListener js/document "keyup" keyb/event-handler!)
  (.addEventListener js/document "fullscreenchange" #(rf/dispatch [::window.e/set-fullscreen (boolean (.-fullscreenElement js/document))]))

  (rf/dispatch [::document.e/center]))

(defn register-ipc-on-events!
  []
  (doseq
   [[channel f]
    [["window-maximized" #(rf/dispatch [::window.e/set-maximized true])]
     ["window-unmaximized" #(rf/dispatch [::window.e/set-maximized false])]
     ["window-entered-fullscreen" #(rf/dispatch [::window.e/set-fullscreen true])]
     ["window-leaved-fullscreen" #(rf/dispatch [::window.e/set-fullscreen false])]
     ["window-minimized" #(rf/dispatch [::window.e/set-minimized true])]
     ["window-loaded" add-listeners!]]]
    (js/window.api.on channel f)))

(defn ^:export init! []
  (js/console.log (str "%c" easter-egg) "color: #e93976")

  (devtools/set-pref!
   :cljs-land-style
   (str "filter:invert(1);" (:cljs-land-style (devtools/get-prefs))))

  ;; https://code.thheller.com/blog/shadow-cljs/2017/10/14/bootstrap-support.html
  (bootstrap/init repl/st {:path "js/bootstrap" :load-on-init '[user]} bootstrap-cb!)

  (rf/dispatch-sync [::app.e/initialize-db])
  (rf/dispatch-sync [::app.e/set-lang system/language])
  (rf/dispatch-sync [::app.e/load-local-db])
  (rf/dispatch-sync [::document.e/init])
  (rf/dispatch-sync [::theme.e/set-native-mode (theme.fx/native-mode! theme.fx/native-query!)])
  (rf/dispatch-sync [::theme.e/add-native-listener])
  (rf/dispatch-sync [::theme.e/set-document-attr])
  (rf/dispatch-sync [::app.e/load-webref])
  (rf/dispatch-sync [::app.e/load-system-fonts])
  (rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (rf/dispatch-sync [::rp/set-keydown-rules keyb/keydown-rules])

  (.setup paper)

  (if system/electron?
    (register-ipc-on-events!)
    (add-listeners!))

  (mount-root!))
