(ns renderer.core
  (:require
   ["electron-log/renderer"]
   ["paper" :refer [paper]]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [reagent.dom.client :as ra.dom.client]
   [renderer.app.effects]
   [renderer.app.events :as app.e]
   [renderer.app.subs]
   [renderer.app.views :as app.v]
   [renderer.attribute.events]
   [renderer.attribute.impl.core]
   [renderer.dialog.events]
   [renderer.dialog.subs]
   [renderer.document.events :as document.e]
   [renderer.document.subs]
   [renderer.element.effects]
   [renderer.element.events]
   [renderer.element.impl.core]
   [renderer.element.subs]
   [renderer.frame.events]
   [renderer.frame.subs]
   [renderer.history.events]
   [renderer.history.subs]
   [renderer.menubar.events]
   [renderer.notification.events]
   [renderer.notification.subs]
   [renderer.reepl.replumb :as replumb]
   [renderer.ruler.events]
   [renderer.ruler.subs]
   [renderer.snap.events]
   [renderer.snap.subs]
   [renderer.theme.db :as db]
   [renderer.theme.effects :as theme.fx]
   [renderer.theme.events :as theme.e]
   [renderer.theme.subs]
   [renderer.timeline.events]
   [renderer.timeline.subs]
   [renderer.tool.effects]
   [renderer.tool.events]
   [renderer.tool.impl.core]
   [renderer.tool.subs]
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

(defn mount-root! []
  (rf/clear-subscription-cache!)
  (let [container (.getElementById js/document "app")
        root (ra.dom.client/create-root container)]
    (ra.dom.client/render root [error/boundary [app.v/root]])))

(defn bootstrap-cb!
  []
  (replumb/run-repl "(in-ns 'user)" identity)
  (print "Welcome to your REPL!")
  (print "")
  (print "You can create or modify shapes using the command line.")
  (print "Type (help) to see a list of commands."))

(defn set-focused!
  []
  (rf/dispatch [::window.e/set-focused (or (.hasFocus js/document)
                                           (and (dom/frame-document!)
                                                (.hasFocus (dom/frame-document!))))]))

(defn add-listeners!
  []
  (.addEventListener js/document "keydown" keyb/event-handler!)
  (.addEventListener js/document "keyup" keyb/event-handler!)
  (.addEventListener js/document "fullscreenchange" #(rf/dispatch [::window.e/set-fullscreen (boolean (.-fullscreenElement js/document))]))
  (.addEventListener js/window "load" set-focused!)
  (.addEventListener js/window "focus" #(rf/dispatch [::window.e/set-focused true]))
  (.addEventListener js/window "blur" set-focused!)

  (rf/dispatch [::document.e/center]))

(defn register-ipc-on-events!
  []
  (doseq
   [[channel f]
    [["window-maximized" #(rf/dispatch [::window.e/set-maximized true])]
     ["window-unmaximized" #(rf/dispatch [::window.e/set-maximized false])]
     ["window-focused" #(rf/dispatch [::window.e/set-focused true])]
     ["window-blurred" #(rf/dispatch [::window.e/set-focused false])]
     ["window-entered-fullscreen" #(rf/dispatch [::window.e/set-fullscreen true])]
     ["window-leaved-fullscreen" #(rf/dispatch [::window.e/set-fullscreen false])]
     ["window-minimized" #(rf/dispatch [::window.e/set-minimized true])]
     ["window-loaded" add-listeners!]]]
    (js/window.api.on channel f)))

(defn ^:export init! []
  (js/console.log (str "%c" easter-egg) (str "color: " renderer.theme.db/accent))

  ;; https://code.thheller.com/blog/shadow-cljs/2017/10/14/bootstrap-support.html
  (bootstrap/init repl/st {:path "js/bootstrap" :load-on-init '[user]} bootstrap-cb!)

  (rf/dispatch-sync [::app.e/initialize-db])
  (rf/dispatch-sync [::app.e/set-lang system/language])
  (rf/dispatch-sync [::app.e/load-system-fonts])
  (rf/dispatch-sync [::app.e/load-local-db])
  (rf/dispatch-sync [::document.e/init])
  (rf/dispatch-sync [::theme.e/set-native-mode (theme.fx/native-mode! theme.fx/native-query!)])
  (rf/dispatch-sync [::theme.e/add-native-listener])
  (rf/dispatch-sync [::theme.e/set-document-attr])
  (rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (rf/dispatch-sync [::rp/set-keydown-rules keyb/keydown-rules])

  (.setup paper)

  (if system/electron?
    (register-ipc-on-events!)
    (add-listeners!))

  (mount-root!))
