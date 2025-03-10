(ns wh.components.cards
  (:require
    #?(:cljs [goog.string :as gstring])
    #?(:cljs [goog.string.format :as gformat])
    #?(:cljs [wh.components.cards.subs])
    #?(:cljs [wh.subs :refer [<sub]])
    [clojure.string :as str]
    [wh.common.text :refer [pluralize]]
    [wh.components.common :refer [link wrap-img blog-card-hero-img]]
    [wh.routes :as routes]
    [wh.slug :as slug]
    [wh.util :as util]))

(def PI
  #?(:cljs js/Math.PI
     :clj Math/PI))

(defn cos [a]
  #?(:cljs (js/Math.cos a)
     :clj (Math/cos a)))

(defn sin [a]
  #?(:cljs (js/Math.sin a)
     :clj (Math/sin a)))

(defn polar->cartesian [center-x center-y radius angle-in-degrees]
  (let [angle-in-radians (/ (* (- angle-in-degrees 90)
                               PI)
                            180)]
    {:x (+ center-x (* radius (cos angle-in-radians)))
     :y (+ center-y (* radius (sin angle-in-radians)))}))

(defn draw-shape [score]
  (let [x 9
        y 9
        radius 9
        start-angle 0
        end-angle (* 360 score)
        start (polar->cartesian x y radius end-angle)
        end (polar->cartesian x y radius start-angle)
        arc-sweep (if (<= (- end-angle start-angle) 180)
                    "0"
                    "1")]
    (->> ["M" (:x start) (:y start)
          "A" radius radius 0 arc-sweep 0 (:x end) (:y end)
          "L" x y
          "L" (:x start) (:y start)]
         (str/join " "))))

(defn match-circle
  ([score]
   (match-circle score false))
  ([score text?]
   [:div.match-circle-container
    (if (= score 1.0)
      [:div.match-circle
       [:div.foreground]]
      [:div.match-circle
       [:svg.circle-value
        [:path.circle {:d (draw-shape score)}]]
       [:div.background]])
    (when text?
      [:div.text #?(:cljs (gstring/format "%d%% Match" (* score 100))
                    :clj (format "%s%% Match" (int (* score 100))))])]))

(defn blog-card
  [{:keys [id title feature author formatted-creation-date reading-time upvote-count tags creator published] :as blog}]
  (let [skeleton? (and blog (empty? (dissoc blog :id)))]
    [:div {:class (util/merge-classes "card"
                                      "card--blog"
                                      (str "i-cur-" (rand-int 9))
                                      (when skeleton? "blog-card--skeleton"))}
     (if skeleton?
       [:div.hero]
       (link [:div.hero
              (wrap-img blog-card-hero-img feature {:alt "Blog hero image"})] :blog :id id))
     (if skeleton?
       [:div.blog-info
        [:div.title]
        [:ul.tags
         [:li] [:li] [:li]]]
       [:div.blog-info
        (link [:div.title title] :blog :id id)
        (link [:div.author author] :blog :id id)
        (link [:div.datetime formatted-creation-date " | " reading-time " min read | " upvote-count " " (pluralize upvote-count "boost")] :blog :id id)
        (into [:ul.tags]
              (for [tag tags]
                [:li
                 [:a {:href (routes/path :learn-by-tag :params {:tag (slug/slug+encode tag)})}
                  tag]]))
        #?(:cljs
           [:div
            (when (<sub [:blog-card/can-edit? creator published])
              [link [:button.button.button--edit-blog "Edit"] :contribute-edit :id id])
            (when (<sub [:blog-card/show-unpublished? creator published])
              [:span.card__label.card__label--unpublished.card__label--blog "unpublished"])])])]))
