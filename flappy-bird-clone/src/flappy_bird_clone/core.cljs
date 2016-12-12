(ns flappy-bird-clone.core
  (:require [play-cljs.core :as p]
            [goog.events :as events]))

(defonce game (p/create-game 500 500))
(defonce state (atom {:timeoutid 0
                      :splash (p/load-image game "splash.png")
                      :sky (p/load-image game "sky.png")
                      :land (p/load-image game "land.png")
                      :bird (p/load-image game "Flappy_Bird.png")
                      :pipe (p/load-image game "pipe.png")
                      :pipedwn (p/load-image game "pipedwn.png")
                      :bird-y 200
                      :pipes []}))

(declare title-screen)
(declare main-screen)

;If we are on the title screen a mouse click takes us to the next screen,
;otherwise we minus the bird's y position to make it jump.
(events/listen js/window "mousedown"
               (fn [_]
                 (let [gme (p/get-screen game)]
                   (cond
                     (= gme title-screen) (p/set-screen game main-screen)
                     (= gme main-screen) (swap! state update-in [:bird-y] #(- % 40))))))

;Top and bottom pipes are generated together as the gap between them should
;always be the same.
(defn pipe-gen []
  (let [rnd (rand 350)]
    [[:image {:value (:pipedwn @state) :width 50 :height 400 :x 550 :y (+ -400 rnd)}]
     [:image {:value (:pipe @state) :width 50 :height 400 :x 550 :y (+ 200 rnd)}]]))

;For two rectangles, if we know the top left and bottom right co-ordinates,
;there are four cases which if true mean those two rectangles are not
;overlapping, otherwise they must be. Details here:
;http://stackoverflow.com/questions/23302698/java-check-if-two-rectangles-overlap-at-any-point
(defn collision-detection [images [_ {:keys [x y width height] :as bird}]]
  (let [diags (map
                (fn [[_ {:keys [x y width height] :as image}]]
                  {:x1 x :y1 y :x2 (+ x width) :y2 (+ y height)})
                images)
        overlap-check (fn [{:keys [x1 y1 x2 y2]}]
                        (let [birdx1 x birdy1 y birdx2 (+ x 60) birdy2 (+ y 60)]
                          (cond
                            (< birdx2 x1) false
                            (> birdx1 x2) false
                            (> birdy1 y2) false
                            (> y1 birdy2) false
                            :overlapping true)))
        overlaps (map overlap-check diags)]
    (some #(= true %) overlaps)))

(def main-screen
  (reify p/Screen

    (on-show [this]
      ;Every four seconds we add two new pipes to a filtered list of the old pipes,
      ;where we remove pipes that have gone off the screen to the left.
      ;We also need to record the id of our call to setInterval so we can
      ;destroy it when we leave this screen.
      (swap! state update-in [:timeoutid]
             (fn [_] (js/setInterval
                       (fn []
                         (swap! state update-in [:pipes]
                                (fn [pipes]
                                  (apply conj (filter
                                                (fn [pipe]
                                                  (< 0 (get-in pipe [1 :x]))) pipes)
                                         (pipe-gen)))))
                       4000))))

    (on-hide [this]
      (js/clearInterval (:timeoutid @state)))

    (on-render [this]
      (let [{:keys [sky land bird bird-y pipe pipes]} @state
            bird-img [:image {:value bird :width 60 :height 60 :x 200 :y bird-y}]]

        ;If the bird hits the ground or a pipe, return to the title screen and
        ;reset its position.
        (when (or (< 400 bird-y) (collision-detection pipes bird-img))
          (do
            (swap! state update-in [:pipes] (fn [_] []))
            (swap! state update-in [:bird-y] (fn [_] 0))
            (p/set-screen game title-screen)))

        ;A poor person's gravity
        (swap! state update-in [:bird-y] #(+ % 3))

        ;Move all of our pipes to the left, to the left.
        (swap! state update-in [:pipes] (fn [pipes] (map
                                                      (fn [pipe]
                                                        (update-in pipe [1 :x] dec))
                                                      pipes)))

        (p/render game
                  [[:image {:value sky :width 500 :height 500 :x 0 :y 0}]
                   [:image {:value land :width 500 :height 100 :x 0 :y 450}]
                   bird-img])

        (p/render game pipes)))))

(def title-screen
  (reify p/Screen
    (on-show [this])
    (on-hide [this])
    (on-render [this]
      (p/render game
                [[:image {:value (:sky @state) :width 500 :height 500 :x 0 :y 0}]
                 [:image {:value (:splash @state) :width 300 :height 300 :x 100 :y 100}]
                 [:image {:value (:land @state) :width 500 :height 100 :x 0 :y 450}]]))))

(doto game
  (p/stop)
  (p/start)
  (p/set-screen title-screen))

