(ns wh.logged-in.apply.db
  (:require
    [cljs.spec.alpha :as s]))

(def steps #{:name :cv-upload :thanks :current-location :visa :rejection})

(s/def ::steps-taken (s/coll-of steps))
(s/def ::current-step steps)
(s/def ::id string?)
(s/def ::slug string?)
(s/def ::submit-success? boolean?)
(s/def ::updating? boolean?)
(s/def ::cv-upload-failed? boolean?)
(s/def ::name-update-failed? boolean?)
(s/def ::job (s/or :id   (s/keys :req-un [::id])
                   :slug (s/keys :req-un [::slug])))

(s/def ::sub-db (s/keys :opt-un [::job
                                 ::submit-success?
                                 ::updating?
                                 ::current-step
                                 ::steps-taken
                                 ::name-update-failed?
                                 ::cv-upload-failed?]))

(def default-db {::steps-taken #{}})
