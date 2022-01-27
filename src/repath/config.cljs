(ns repath.config)

(goog-define ^js/String version "unknown")

(def debug? ^boolean goog.DEBUG)

(defonce sentry-options
  {:dsn "https://f660d6c723444167842e0a6ee84bbfa0@o378764.ingest.sentry.io/5202851"
   :environment (if debug? "development" "production")})