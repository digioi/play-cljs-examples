(ns super-koalio.state
  (:require [play-cljs.core :as p]
            [super-koalio.utils :as u]))

(def ^:const url "koalio.png")
(def ^:const tile-width 18)
(def ^:const tile-height 26)

(defn initial-state []
  (let [stand-right (p/sprite url 0 0 {:frame (p/rectangle 0 0 tile-width tile-height)})
        stand-left (-> stand-right (assoc :anchor [1 0]) (assoc :scale [-1 1]))
        jump-right (p/sprite url 0 0 {:frame (p/rectangle tile-width 0 tile-width tile-height)})
        jump-left (-> jump-right (assoc :anchor [1 0]) (assoc :scale [-1 1]))
        walk1-right (p/sprite url 0 0 {:frame (p/rectangle (* 2 tile-width) 0 tile-width tile-height)})
        walk1-left (-> walk1-right (assoc :anchor [1 0]) (assoc :scale [-1 1]))
        walk2-right (p/sprite url 0 0 {:frame (p/rectangle (* 3 tile-width) 0 tile-width tile-height)})
        walk2-left (-> walk2-right (assoc :anchor [1 0]) (assoc :scale [-1 1]))
        walk3-right (p/sprite url 0 0 {:frame (p/rectangle (* 4 tile-width) 0 tile-width tile-height)})
        walk3-left (-> walk3-right (assoc :anchor [1 0]) (assoc :scale [-1 1]))]
    {:stand-right stand-right
     :stand-left stand-left
     :jump-right jump-right
     :jump-left jump-left
     :walk-right [walk1-right walk2-right walk3-right]
     :walk-left [walk1-left walk2-left walk3-left]
     :x-velocity 0
     :y-velocity 0
     :x 100
     :y 0
     :can-jump? false
     :direction :right}))

(defn move
  [{:keys [x y can-jump?] :as state} game delta-time]
  (let [x-velocity (u/get-x-velocity game state)
        y-velocity (+ (u/get-y-velocity game state) u/gravity)
        x-change (* x-velocity delta-time)
        y-change (* y-velocity delta-time)]
    (if (or (not= 0 x-change) (not= 0 y-change))
      (assoc state
             :x-velocity (u/decelerate x-velocity)
             :y-velocity (u/decelerate y-velocity)
             :x-change x-change
             :y-change y-change
             :x (+ x x-change)
             :y (+ y y-change)
             :can-jump? (if (> y-velocity 0) false can-jump?))
      state)))

(defn prevent-move
  [{:keys [x y x-change y-change] :as state} game]
  (let [max-y (- (p/get-height game) tile-height)
        old-y (- y y-change)
        up? (neg? y-change)]
    (merge state
           (when (> y max-y)
             {:y-velocity 0 :y-change 0 :y old-y :can-jump? (not up?)}))))
