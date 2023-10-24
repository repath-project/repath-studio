(ns renderer.db)

(def default
  {:tool :select
   :mouse-pos [0 0]
   :zoom-factor 0.8
   :state :default
   :documents {}
   :document-tabs []
   :system-fonts []
   :debug-info? false
   :pen-mode? false
   :repl/mode :cljs
   :window {:maximized? true
            :minimized? false
            :fullscreen? false
            :header? true
            :history? false
            :timeline? true
            :xml? false
            :tree {:size 300
                   :visible? true}
            :properties {:size 300
                         :visible? true}
            :elements-collapsed? false
            :pages-collapsed? false
            :command-palette? false
            :defs-collapsed? true
            :symbols-collapsed? true
            :repl-history? false
            :theme-mode :dark}})
