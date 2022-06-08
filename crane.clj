 (ns matcher-starter.crane
  (:require [org.clojars.cognesence.breadth-search.core :refer :all]
            [org.clojars.cognesence.matcher.core :refer :all]
            [org.clojars.cognesence.ops-search.core :refer :all]))

(use 'org.clojars.cognesence.ops-search.core)
(use 'org.clojars.cognesence.breadth-search.core)
(use 'org.clojars.cognesence.matcher.core)

;; ============================== Initial Scenario ===============================
;; Barge Platforms : b1, b2, b3
;; Train Platforms : t1, t2, t3, t4
;; Containers : c1, c2, c3, c4, c5
;; Scenario Description : Crane facing the train over platform t3,
;;                        container c1 is on top of container c2 on platform t1

(def world '#{
              (side docks)
              (side tracks)

              (transport docks barge)
              (transport tracks train)

              (opposite docks tracks)
              (opposite tracks docks)

              (next-platform tracks t1 t2)
              (next-platform tracks t2 t3)
              (next-platform tracks t3 t4)

              (next-platform docks b1 b2)
              (next-platform docks b2 b3)

              (container c1)
              (container c2)
              (container c3)
              (container c4)
              (container c5)
              })

(def start '#{
              (crane-facing tracks)
              (crane-empty)
              (current-platform docks b1)
              (current-platform tracks t3)
              (top-container b1 none)
              (top-container b2 c3)
              (top-container b3 c4)
              (on-top-of c3 none)
              (on-top-of c4 c5)     ;; container c4 on top of c5
              (on-top-of c5 none)
              (top-container t1 c1)
              (on-top-of c1 c2)     ;; container c1 on top of c2
              (on-top-of c2 none)
              (top-container t2 none)
              (top-container t3 none)
              (top-container t4 none)
              })

(def ops '{
           ;; Place the container held by the crane onto the current platform of the
           ;; transport that the crane is facing.

           place-container {
                            :pre (
                                  (crane-holding ?c1)
                                  (crane-facing ?side)
                                  (current-platform ?side ?platform)
                                  (top-container ?platform ?c2))
                            :add (
                                  (crane-empty)
                                  (on-top-of ?c1 ?c2)
                                  (top-container ?platform ?c1))
                            :del (
                                  (crane-holding ?c1)
                                  (top-container ?platform ?c2))
                            :txt (crane places container ?c1 on platform ?platform)
                            }

           ;; Move the transport on the side the crane is facing backwards with respect
           ;; to the crane, changing the current platform in the process.

           shunt-backwards {
                            :pre (
                                  (crane-facing ?side)
                                  (current-platform ?side ?p1)
                                  (next-platform ?side ?p2 ?p1)
                                  (transport ?side ?transport))
                            :add (
                                  (current-platform ?side ?p2))
                            :del (
                                  (current-platform ?side ?p1))
                            :txt (shunt ?transport backward making ?p2 current)
                            }

           ;; Move the transport on the side the crane is facing forwards with respect
           ;; to the crane, changing the current platform in the process.

           shunt-forwards {
                           :pre (
                                 (crane-facing ?side)
                                 (current-platform ?side ?p1)
                                 (next-platform ?side ?p1 ?p2)
                                 (transport ?side ?transport))
                           :add (
                                 (current-platform ?side ?p2))
                           :del (
                                 (current-platform ?side ?p1))
                           :txt (shunt ?transport forward making ?p2 current)
                           }

           ;; Pick up the top container from the current platform of the transport that
           ;; the crane is facing.

           lift-container {
                           :pre (
                                 (crane-empty)
                                 (crane-facing ?side)
                                 (current-platform ?side ?platform)
                                 (top-container ?platform ?c1)
                                 (container ?c1)
                                 (on-top-of ?c1 ?c2))
                           :add (
                                 (crane-holding ?c1)
                                 (top-container ?platform ?c2))
                           :del (
                                 (crane-empty)
                                 (on-top-of ?c1 ?c2)
                                 (top-container ?platform ?c1))
                           :txt (crane picks up container ?c1 from platform ?platform)
                           }

           ;; Rotate the crane so as to face the other transport to which it is facing
           ;; when the operator is applied.

           rotate-crane {
                         :pre (
                               (crane-facing ?s1)
                               (opposite ?s1 ?s2))
                         :add (
                               (crane-facing ?s2))
                         :del (
                               (crane-facing ?s1))
                         :txt (crane turns to face ?s2)
                         }
           })

;; ============================== First Scenario ================================
;; The first problem will be to swap the positions of containers c1 and c4 which
;; will force one of them to be moved to a temporary holding area prior to it
;; being moved into place.

(def goal-1 '#{
               (top-container t1 c4)
               (on-top-of c4 c2)       ;; container c4 on top of c2
               (top-container b2 c3)
               (top-container b3 c1)
               (on-top-of c1 c5)       ;; container c1 on top of c5
               })

(println "\n ===== Output of first scenario with cpu execution time =====\n")
(time
  (run! println
        (remove nil? ((ops-search start goal-1 ops :world world) :txt))))
(println "\n")

;; ============================== Second Scenario ===============================
;; Second problem will be to start from the same starting configuration, but
;; this time move the stack of containers c1 and c2 from the train onto the
;; empty platform on the barge whilst preserving their stacking order.

(def goal-2 '#{
               (top-container b1 c1)
               (on-top-of c1 c2)       ;; container c1 on top of c2
               (top-container b2 c3)
               (top-container b3 c4)
               (on-top-of c4 c5)       ;; container c4 on top of c5
               })

(println "\n ===== Output of second scenario with cpu execution time =====\n")
(time
  (run! println
        (remove nil? ((ops-search start goal-2 ops :world world) :txt))))
(println "\n")

;; ============================== Third Scenario ================================
;; Third problem will be to remove container c5 from under c4 on the barge
;; and place it on platform t4 on the train.

(def goal-3 '#{
               (top-container t1 c1)
               (on-top-of c1 c2)       ;; container c1 on top of c2
               (top-container t4 c5)
               (top-container b2 c3)
               (top-container b3 c4)
               })

(println "\n ===== Output of third scenario with cpu execution time =====\n")
(time
  (run! println
        (remove nil? ((ops-search start goal-3 ops :world world) :txt))))
(println "\n")


