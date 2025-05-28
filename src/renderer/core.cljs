(ns renderer.core
  (:require
   ["electron-log/renderer"]
   ["paper" :refer [paper]]
   [re-frame.core :as rf]
   [re-pressed.core :as re-pressed]
   [reagent.dom.client :as ra.dom.client]
   [renderer.app.effects]
   [renderer.app.events :as app.events]
   [renderer.app.subs]
   [renderer.app.views :as app.views]
   [renderer.attribute.impl.core]
   [renderer.dialog.events]
   [renderer.dialog.subs]
   [renderer.document.events :as document.events]
   [renderer.document.subs]
   [renderer.element.effects]
   [renderer.element.events]
   [renderer.element.impl.core]
   [renderer.element.subs]
   [renderer.event.effects]
   [renderer.event.impl.keyboard :as event.impl.keyboard]
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
   [renderer.theme.db :as theme.db]
   [renderer.theme.effects]
   [renderer.theme.events :as theme.events]
   [renderer.theme.subs]
   [renderer.timeline.events]
   [renderer.timeline.subs]
   [renderer.tool.events]
   [renderer.tool.impl.core]
   [renderer.tool.subs]
   [renderer.tree.events]
   [renderer.utils.error :as utils.error]
   [renderer.window.effects]
   [renderer.window.events]
   [renderer.window.subs]
   [renderer.worker.events]
   [renderer.worker.subs]
   [replumb.repl :as replumb.repl]
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

(defonce root-el (atom nil))

(defn ^:dev/after-load mount-root! []
  (let [container (.getElementById js/document "app")]
    (rf/clear-subscription-cache!)
    (when @root-el (ra.dom.client/unmount @root-el))
    (reset! root-el (ra.dom.client/create-root container))
    (ra.dom.client/render @root-el [utils.error/boundary [app.views/root]])))

(defn bootstrap-cb!
  []
  (replumb/run-repl "(in-ns 'user)" identity)
  (print "Welcome to your REPL!")
  (print "")
  (print "You can create or modify shapes using the command line.")
  (print "Type (help) to see a list of commands."))

(defn ^:export init! []
  (js/console.log (str "%c" easter-egg) (str "color: " theme.db/accent))

  ;; https://code.thheller.com/blog/shadow-cljs/2017/10/14/bootstrap-support.html
  (bootstrap/init replumb.repl/st {:path "js/bootstrap"
                                   :load-on-init '[user]} bootstrap-cb!)

  (rf/dispatch-sync [::app.events/initialize-db])
  (rf/dispatch-sync [::app.events/load-local-db])
  (rf/dispatch-sync [::app.events/init-lang])
  (rf/dispatch-sync [::theme.events/set-document-mode])
  (rf/dispatch-sync [::document.events/init])
  (rf/dispatch-sync [::theme.events/add-native-listener])
  (rf/dispatch-sync [::re-pressed/add-keyboard-event-listener "keydown"])
  (rf/dispatch-sync [::re-pressed/set-keydown-rules event.impl.keyboard/keydown-rules])
  (rf/dispatch-sync [::app.events/register-listeners])

  (.setup paper)

  (mount-root!))
