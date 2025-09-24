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
   [renderer.document.events]
   [renderer.document.subs]
   [renderer.element.effects]
   [renderer.element.events]
   [renderer.element.impl.core]
   [renderer.element.subs]
   [renderer.error.effects]
   [renderer.error.events]
   [renderer.error.subs]
   [renderer.error.views :as error.views]
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
   [renderer.theme.effects]
   [renderer.theme.events]
   [renderer.theme.subs]
   [renderer.timeline.events]
   [renderer.timeline.subs]
   [renderer.tool.events]
   [renderer.tool.impl.core]
   [renderer.tool.subs]
   [renderer.tree.events]
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

(defonce root (delay (-> (.getElementById js/document "app")
                         (ra.dom.client/create-root))))

(defn ^:dev/after-load mount-root! []
  (rf/clear-subscription-cache!)
  (ra.dom.client/render @root [error.views/boundary [app.views/root]]))

(defn bootstrap-cb! []
  (replumb/run-repl "(in-ns 'user)" identity)
  (print "Welcome to your REPL!")
  (print "")
  (print "You can create or modify shapes using the command line.")
  (print "Type (help) to see a list of commands."))

(defn ^:export init! []
  (js/console.log (str "%c" easter-egg) (str "color: " "pink"))

  ;; https://code.thheller.com/blog/shadow-cljs/2017/10/14/bootstrap-support.html
  (bootstrap/init replumb.repl/st {:path "js/bootstrap"
                                   :load-on-init '[user]} bootstrap-cb!)

  (rf/dispatch-sync [::app.events/initialize])
  (rf/dispatch-sync [::re-pressed/add-keyboard-event-listener "keydown"])
  (rf/dispatch-sync [::re-pressed/set-keydown-rules event.impl.keyboard/keydown-rules])

  (.setup paper)

  (mount-root!))
