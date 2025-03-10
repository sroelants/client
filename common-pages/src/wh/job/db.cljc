(ns wh.job.db
  (:require
    [#?(:cljs cljs.spec.alpha :clj clojure.spec.alpha) :as s]
    [wh.common.data :as data]))

(defn company-permissions
  [db]
  (or (get-in db [::sub-db ::company :permissions])
      (get-in db [:wh.user.db/sub-db :wh.user.db/company :permissions])))

;; The :no-matching-job is also used in wh.response.handler.job
(s/def ::error (s/nilable #{:no-matching-job :unknown-error :unauthorised}))

(s/def ::title string?)
;;
(s/def ::description-html (s/nilable string?))
(s/def ::name (s/nilable string?))
(s/def ::logo (s/nilable string?))
(s/def ::benefits :wh/tags)
(s/def ::company (s/keys :opt-un [::logo
                                  ::name
                                  ::description-html
                                  ::benefits]))
(s/def ::location-description (s/nilable string?))
(s/def ::display-location string?)
(s/def ::tags (s/coll-of (s/nilable string?)))
(s/def ::applied (s/nilable boolean?))
(s/def ::manager (s/nilable string?))


(s/def ::sponsorship-offered boolean?)
(s/def ::remote boolean?)
(s/def ::id string?)
(s/def ::company-id string?)
(s/def ::slug string?)
(s/def ::published (s/nilable boolean?))
(s/def ::street (s/nilable string?))
(s/def ::post-code (s/nilable string?))
(s/def ::city (s/nilable string?))
(s/def ::state (s/nilable string?))
(s/def ::country string?)
(s/def ::country-code (s/nilable (set (map first data/country-code-and-country))))
(s/def ::sub-region (s/nilable string?))
(s/def ::region (s/nilable string?))

(s/def ::publishing? boolean?)

(def role-types ["Full time" "Contract" "Intern"])
(s/def ::role-type (set role-types))

(s/def ::latitude (s/or :double (s/double-in :min -90.0 :max 90 :NaN false :infinite? false)
                        :int (s/int-in -90 90)
                        :empty nil?))
(s/def ::longitude (s/or :double (s/double-in :min -180.0 :max 180 :NaN false :infinite? false)
                         :int (s/int-in -180 180)
                         :empty? nil?))
(s/def ::location (s/nilable (s/keys :opt-un [::street
                                              ::city
                                              ::country
                                              ::country-code
                                              ::state
                                              ::latitude
                                              ::longitude
                                              ::sub-region
                                              ::region
                                              ::post-code])))

(s/def ::competitive boolean?)
(s/def ::currency (set data/currencies))
(s/def ::time-period (set data/time-periods))
(s/def ::min nat-int?)
(s/def ::max (s/nilable nat-int?))
(s/def ::equity boolean?)

(s/def ::remuneration
  (s/or :salary (s/nilable (s/keys :req-un [::competitive
                                            ::currency
                                            ::time-period
                                            ::equity
                                            ::max]
                                   :opt-un [::min]))
        :competitive (s/nilable (s/keys :req-un [::competitive]))))

(s/def ::show-apply-sticky? boolean?)
(s/def ::show-admin-publish-prompt? boolean?)
(s/def ::admin-publish-prompt-loading? boolean?)
(s/def ::recommended-jobs (s/coll-of (s/map-of keyword? any?)))
(s/def ::preset-slug ::slug)

(s/def ::sub-db (s/keys :opt [::company
                              ::description-html
                              ::location-description
                              ::manager
                              ::title
                              ::tags
                              ::remote
                              ::sponsorship-offered
                              ::id
                              ::applied
                              ::error
                              ::published
                              ::location
                              ::role-type
                              ::remuneration
                              ;;
                              ::publishing?
                              ::show-apply-sticky?
                              ::show-admin-publish-prompt?
                              ::admin-publish-prompt-loading?
                              ::recommended-jobs
                              ::preset-slug]))

(def default-db {::error nil
                 ::publishing? false
                 ::show-apply-sticky? false
                 ::show-admin-publish-prompt? false
                 ::admin-publish-prompt-loading? false})

(defn show-unpublished? [admin? owner? published?]
  (and (false? published?) (or admin? owner?)))
