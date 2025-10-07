(ns config)

(goog-define ^js/String version "unknown")

(def debug? ^boolean goog.DEBUG)

(def ext "rps")

(def app-name "Repath Studio")

(def mime-type "application/x-repath-studio")

(def default-path "documents")

(def save-info-keys [:id :title :path :file-handle])

(def sentry {:dsn "https://4098ce3035c6f04b92b636bda55790ac@o4510040121933824.ingest.de.sentry.io/4510040141201488"
             :environment (if debug? "development" "production")
             :release version
             :debug debug?})
