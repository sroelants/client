(ns wh.common.url
  (:require
    #?(:clj [taoensso.timbre :refer [error]])
    #?(:cljs [goog.Uri :as uri])
    [clojure.string :as str]))

(def wh-cookie-names {:auth             "auth_token"
                      :tracking-consent "wh_tracking_consent"
                      :tracking-id      "wh_aid"})

(defn sanitize-url [url]
  (if (str/includes? url "://")
    url
    (str "http://" url)))

(defn websites-domain? [website domain]
  (when domain
    (str/includes? domain website)))

(defn uri->domain [uri]
  (let [uri (str/trim uri)]
    #?(:cljs
       (.getDomain (uri/parse uri)))
    #?(:clj
       (try
         (.getHost (java.net.URI. uri))
         (catch Exception e
           (error "Failed to parse URI:" uri))))))

(defn detect-page-type [url]
  (when url
    (let [url (sanitize-url url)
          [_ _ domain & remaining] (str/split url #"/")
          handle (last remaining)]
      (merge {:url url}
             (condp websites-domain? domain
               "github.com" {:type :github, :display handle}
               "twitter.com" {:type :twitter, :display handle}
               "facebook.com" {:type :facebook, :display handle}
               "linkedin.com" {:type :linkedin, :display handle}
               "stackoverflow.com" {:type :stackoverflow, :display handle}
               {:type :web, :display (uri->domain url)})))))

(defn detect-urls-type [urls]
  (mapv (comp detect-page-type :url) urls))

(defn vertical-homepage-href
  "Server-side callers should consider wh.url/base-url as a more
  robust - but less portable - version of ths fn"
  [env vertical]
  (case (name env)
    "prod"  (str "https://" (name vertical) ".works-hub.com")
    "stage" (str "/?vertical=" (name vertical))
    "dev"   (str "http://" (name vertical) ".localdomain:3449")))
