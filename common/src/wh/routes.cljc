(ns wh.routes
  (:require
    #?(:clj [clojure.spec.alpha :as s])
    #?(:clj [ring.util.codec :as codec])
    #?(:clj [taoensso.timbre :refer [warn]])
    #?(:cljs [cljs.spec.alpha :as s])
    #?(:cljs [goog.Uri.QueryData :as query-data])
    [bidi.bidi :as bidi]
    [wh.common.specs.primitives]
    [wh.common.text :as text]))

(def company-landing-page "https://www.works-hub.com")
(def pages-with-loader #{:homepage :learn :blog :github-callback :liked :recommended :profile :pre-set-search :jobsboard :contribute-edit})
(def no-menu-pages #{:register :register-company :payment-setup :get-started})
(def no-footer-pages (set (concat no-menu-pages #{:blog})))

;; Here we overwrite the behavior of Bidi's wrt Pattern matching with sets.
;; The matching is actually left unchanged from the original implementation
;; and we only deal with the unmatching. In Bidi's original behavior, when
;; multiple Alternates are provided, unmatching happens only on the first
;; alternative. We change this behavior to try and see if any of the supplied
;; alternatives has exactly the params that we supply. If we found some, we use
;; the first. If none are found, we revert to the original behavior.
(extend-protocol bidi/Pattern
  #?(:clj  clojure.lang.APersistentSet
     :cljs cljs.core.PersistentHashSet)
  (match-pattern [this s]
    (some #(bidi/match-pattern % s)
          ;; We try to match on the longest string first, so that the
          ;; empty string will be matched last, after all other cases
          (sort-by count > this)))
  (unmatch-pattern [this {:keys [params] :as s}]
    (if-let [match (first (filter (fn [pattern] (= (set (keys params))
                                                   (set (filter keyword? pattern))))
                                  this))]
      (bidi/unmatch-pattern match s)
      (bidi/unmatch-pattern (first this) s))))

;; Bidi route processing works by returning a map that contains
;; :handler and :params.  In WorksHub app, handlers are keywords that
;; denote the page to navigate to, and :params are arbitrary
;; parameters of the page that can be taken from the URL.  However,
;; sometimes there is a need to supply some predefined param right in
;; the route definition.  This isn't allowed out of the box by bidi,
;; so we define our own.

(defn with-params
  "Returns a Matched that adds the specified params
  to the handler."
  [handler & params]
  (let [params-map (apply hash-map params)]
    (reify
      #?(:clj  bidi.bidi.Matched
         :cljs bidi/Matched)
      (resolve-handler [this m]
        (when-let [res (bidi/succeed handler m)]
          (assoc res :params params-map)))
      (unresolve-handler [this m]
        (when (and (= handler (:handler m))
                   (= params-map (select-keys (:params m) (keys params-map))))
          "")))))

;; The collection routes defined here are supposed to have trailing
;; slashes. If a URL without the trailing slash is requested,
;; there will be a server-side redirect to the correct one.

(def routes ["/"
             ;;Public SSR Pages - no app.js required
             [["" :homepage]
              ["hire-" {[:template] :homepage}]
              ["issues/" {""            :issues
                          [:company-id] (bidi/tag :issues :issues-for-company-id)}]
              ["issue/" {[:id] :issue}]
              ["how-it-works" :how-it-works]
              [#{[:tag "-jobs-in-" :location]
                 [:tag "-jobs"]
                 ["jobs-in-" :location]} :pre-set-search]
              [[[#".+" :tag] "-articles"] :learn-by-tag]
              ["privacy-policy" :privacy-policy]
              ["terms-of-service" :terms-of-service]
              ["pricing" :pricing]
              ["sitemap" :sitemap]
              ["invalid-magic-link" :invalid-magic-link]

              ;; Mixed routes
              ["learn/" {""            :learn               ;;Public SSR
                         "create"      :contribute
                         [:id]         :blog                ;;Public yet to be SSR CH2655
                         [:id "/edit"] :contribute-edit}]
              ["companies/" {""                    :companies    ;;Public SSR
                             "new"                 :create-company
                             "applications"        :company-applications
                             [:slug]               :company      ;;Public SSR
                             [:slug "/jobs"]       :company-jobs ;;Public SSR
                             [:id "/edit"]         :admin-edit-company
                             [:id "/dashboard"]    :company-dashboard
                             [:id "/applications"] :admin-company-applications
                             [:id "/offer"]        :create-company-offer}]
              ["jobs/" {""            :jobsboard            ;;Public SSR
                        "new"         :create-job
                        [:slug]       :job                  ;;Public SSR
                        [:id "/edit"] :edit-job}]

              ;; Public pages - app.js required
              ["register/" {"name"         (with-params :register :step :name)
                            "thanks"       (with-params :register :step :thanks)
                            "skills"       (with-params :register :step :skills)
                            "company-info" (with-params :register :step :company-info)
                            "company"      (with-params :register :step :company)
                            "location"     (with-params :register :step :location)
                            "verify"       (with-params :register :step :verify)
                            "test"         (with-params :register :step :test)
                            "email"        (with-params :register :step :email)}]
              ["company-registration" :register-company]
              ["login" {""        (with-params :login :step :root)
                        "/"       (with-params :login :step :root)
                        "/email"  (with-params :login :step :email)
                        "/github" (with-params :login :step :github)}]
              ["get-started" :get-started]
              ["github-callback" :github-callback]

              ;;Private pages - app.js required
              ["admin/" {"companies" :admin-companies}]
              ["candidates/" {""                    :candidates
                              "new"                 :create-candidate
                              [:id]                 :candidate
                              [:id "/edit/header"]  :candidate-edit-header
                              [:id "/edit/cv"]      :candidate-edit-cv
                              [:id "/edit/private"] :candidate-edit-private}]
              ["company-settings/" :edit-company]
              ["company-issues/" {""                                      :company-issues
                                  "repositories"                          :manage-issues
                                  ["repositories/" :owner "/" :repo-name] :manage-repository-issues}]
              ["dashboard" :homepage-dashboard]
              ["liked" :liked]
              ["recommended" :recommended]
              ["applied" :applied]
              ["profile/" {""             :profile
                           "edit/header"  :profile-edit-header
                           "edit/cv"      :profile-edit-cv
                           "edit/private" :profile-edit-private
                           "edit"         :profile-edit-company-user}]
              ["notifications/" {"settings" :notifications-settings}]
              ["improve-recommendations" :improve-recommendations]
              ["payment/" {"package"  (with-params :payment-setup :step :select-package)
                           "confirm"  (with-params :payment-setup :step :pay-confirm)
                           "complete" (with-params :payment-setup :step :pay-success)}]
              ["tags" {"/edit" :tags-edit}]
              ["data" :data-page]                           ;; Does not require app.js, separate template - fully SSR

              ;; Non UI routes - redirects, webhooks, API, xml
              ["sitemap.xml" :sitemapxml]
              ["rss.xml" :rss]
              ["magic-link/" {[:token] :magic-link}]
              ["github-dispatch/" {[:board] :github-dispatch}]
              ["github-app-connect" :connect-github-app]
              ["oauth/" {"greenhouse"          :oauth-greenhouse
                         "greenhouse-callback" :oauth-greenhouse-callback
                         "workable"            :oauth-workable
                         "workable-callback"   :oauth-workable-callback
                         "slack"               :oauth-slack
                         "slack-callback"      :oauth-slack-callback
                         "process"             :oauth-process}]
              ["api/" [["graphql" :graphql]
                       ["graphql-schema" :graphql-schema]
                       ["webhook/" {"github-app" :github-app-webhook}]
                       ["analytics" :analytics]
                       ["login-as" :login-as]
                       ["logout" :logout]
                       ["image" :image-upload]
                       ["cv" {""                           :cv-upload
                              ["/" :filename]              :cv-file
                              ["/" :user-id "/" :filename] :cv-file-legacy}]
                       ["reset-fixtures" :reset-fixtures]
                       ["admin/" {[:command] :admin-command}]]]]])

;;TODO this config should pulled partially from wh.response.ssr/page-content map
(def server-side-only-pages #{:invalid-magic-link
                              :not-found
                              :privacy-policy
                              :sitemap
                              :terms-of-service
                              :oauth-greenhouse
                              :oauth-slack
                              :oauth-workable})
;;TODO this config should be added to wh.response.ssr/page-content map
(def pages-without-app-js-when-not-logged-in #{:company
                                               :companies
                                               :company-jobs
                                               :homepage
                                               :how-it-works
                                               ;;:issue CH3610
                                               ;;:issues CH3615
                                               :job
                                               :jobsboard
                                               :learn
                                               :learn-by-tag
                                               :pre-set-search
                                               ;;:pricing CH3618
                                               })
(def server-side-only-paths (set (map #(bidi/path-for routes %) server-side-only-pages)))

(defn serialize-query-params
  "Serializes a map as query params string."
  [m]
  #?(:clj
     (codec/form-encode m))
  #?(:cljs
     (query-data/createFromKeysValues (clj->js (keys m))
                                      (clj->js (vals m)))))
(s/fdef serialize-query-params
  :args (s/cat :m :http/query-params)
  :ret string?)

(defn path [handler & {:keys [params query-params anchor]}]
  (try
    (cond->
      (apply bidi/path-for routes handler (flatten (seq params)))
      (seq query-params) (str "?" (serialize-query-params query-params))
      (text/not-blank anchor) (str "#" anchor))
    (catch #?(:clj Exception) #?(:cljs js/Object) _
           (let [message (str "Unable to construct link: " (pr-str (assoc params :handler handler)))]
             #?(:clj (warn message))
             #?(:cljs (js/console.warn message)))
           "")))

(s/fdef path
  :args (s/cat :handler keyword?
               :kwargs (s/keys* :opt-un [:http.path/params
                                         :http/query-params]))
  :ret string?)

(s/fdef bidi/path-for
  :args (s/cat :routes vector?
               :handler keyword?)
  :ret string?)
