(define (domain scrisp-give1)
        (:requirements :strips :equality :typing :conditional-effects :quantified-preconditions)
        (:types  category - object  rolename - object  individual - object  orientationtype - object  predicate - object  stepindex - object  syntaxnode - object  positiontype - object)
       (:constants
         b1_dist1 - individual
         chair1 - individual
         np - category
         b1_dist2 - individual
         p1 - individual
         b1_dist3 - individual
         pos_11_7 - individual
         pos_11_6 - individual
         pos_11_5 - individual
         pos_11_4 - individual
         pos_11_8 - individual
         bt1 - individual
         imp-push - predicate
         b5_dist1 - individual
         pred-button - predicate
         pos_2_0 - individual
         pos_2_1 - individual
         pos_2_2 - individual
         pos_2_3 - individual
         pos_2_4 - individual
         pos_0_1 - individual
         imp-movetwosteps - predicate
         pos_2_5 - individual
         pos_0_0 - individual
         pos_2_6 - individual
         b3_dist1 - individual
         pos_2_7 - individual
         pos_0_5 - individual
         bt2 - individual
         pos_0_4 - individual
         bt3 - individual
         pos_0_3 - individual
         b4_dist2 - individual
         pos_0_2 - individual
         b4_dist1 - individual
         pos_0_8 - individual
         pos_0_7 - individual
         pos_0_6 - individual
         pred-alarm - predicate
         pos_11_0 - individual
         pos_11_1 - individual
         pos_11_2 - individual
         pos_11_3 - individual
         imp-turnaround - predicate
         pred-safe - predicate
         imp-movethreesteps - predicate
         conj - category
         imp-turnleft - predicate
         pos_8_1 - individual
         pos_8_0 - individual
         vp - category
         pos_4_0 - individual
         a1 - individual
         pos_4_1 - individual
         pos_4_4 - individual
         pos_4_5 - individual
         pos_4_2 - individual
         pos_4_3 - individual
         pred-picture - predicate
         d - category
         a - category
         n - category
         pos_6_1 - individual
         v - category
         pos_6_0 - individual
         pos_6_3 - individual
         pos_6_2 - individual
         s - category
         south - individual
         pred-to-do - predicate
         pred-flower - predicate
         pred-door - predicate
         imp-turnright - predicate
         pos_10_5 - individual
         b6_dist1 - individual
         pos_10_6 - individual
         b6_dist2 - individual
         pos_10_7 - individual
         pos_10_8 - individual
         pos_10_4 - individual
         pos_3_7 - individual
         pos_3_8 - individual
         pos_10_3 - individual
         pos_10_2 - individual
         pos_10_1 - individual
         pos_10_0 - individual
         pred-red - predicate
         pos_9_7 - individual
         pos_9_8 - individual
         pos_9_5 - individual
         pos_9_6 - individual
         pos_9_3 - individual
         pos_4_6 - individual
         pred-chair - predicate
         pos_9_4 - individual
         pos_4_7 - individual
         pos_4_8 - individual
         pos_9_1 - individual
         pos_9_2 - individual
         pos_1_8 - individual
         pos_6_8 - individual
         pos_1_7 - individual
         pos_7_8 - individual
         pos_1_6 - individual
         pos_7_7 - individual
         pos_1_5 - individual
         pos_7_6 - individual
         pos_6_5 - individual
         pos_1_4 - individual
         pos_7_5 - individual
         pos_1_3 - individual
         pos_6_4 - individual
         pos_6_7 - individual
         pos_7_4 - individual
         pos_1_2 - individual
         pos_7_3 - individual
         pos_1_1 - individual
         pos_6_6 - individual
         pos_8_5 - individual
         pos_1_0 - individual
         pos_5_6 - individual
         pos_8_4 - individual
         pos_5_5 - individual
         pos_5_8 - individual
         pos_8_3 - individual
         pos_8_2 - individual
         pos_5_7 - individual
         pos_8_8 - individual
         pos_8_7 - individual
         west - individual
         pos_8_6 - individual
         pos_5_3 - individual
         b2_dist1 - individual
         pos_5_4 - individual
         b2_dist2 - individual
         pos_5_1 - individual
         t1 - individual
         pos_5_2 - individual
         pos_2_8 - individual
         pos_5_0 - individual
         b3 - individual
         b2 - individual
         pred-trophy - predicate
         b5 - individual
         north - individual
         b4 - individual
         b6 - individual
         pos_7_0 - individual
         pos_7_1 - individual
         pos_7_2 - individual
         pred-blue - predicate
         pos_9_0 - individual
         flower1 - individual
         b1 - individual
         pos_3_6 - individual
         pos_3_5 - individual
         pos_3_4 - individual
         pos_3_3 - individual
         east - individual
         pos_3_2 - individual
         pos_3_1 - individual
         pos_3_0 - individual
         lamp1 - individual
         pred-lamp - predicate
         s1 - individual
         imp-moveonestep - predicate
         pred-green - predicate
         dt1 - individual
         pt1 - individual
       )
       (:predicates
         (alarmed ?x1 - positiontype  )
         (visible ?x1 - individual  ?x2 - individual  ?x3 - individual  )
         (left-of ?x1 - individual  ?x2 - individual  )
         (next ?x1 - syntaxnode  ?x2 - syntaxnode  )
         (movetwosteps ?x1 - individual  )
         (door ?x1 - individual  )
         (next-referent ?x1 - individual  ?x2 - individual  )
         (push ?x1 - individual  ?x2 - individual  )
         (object-position ?x1 - individual  ?x2 - positiontype  )
         (needtoexpress-2 ?x1 - predicate  ?x2 - individual  ?x3 - individual  )
         (needtoexpress-1 ?x1 - predicate  ?x2 - individual  )
         (subst ?x1 - category  ?x2 - syntaxnode  )
         (button ?x1 - individual  )
         (next-orientation-left ?x1 - orientationtype  ?x2 - orientationtype  )
         (current ?x1 - syntaxnode  )
         (above ?x1 - individual  ?x2 - individual  )
         (blocked ?x1 - positiontype  ?x2 - positiontype  )
         (flower ?x1 - individual  )
         (player-position ?x1 - positiontype  )
         (safe ?x1 - individual  )
         (trophy ?x1 - individual  )
         (referent ?x1 - syntaxnode  ?x2 - individual  )
         (object-orientation ?x1 - individual  ?x2 - orientationtype  )
         (green ?x1 - individual  )
         (turnright ?x1 - individual  )
         (adjacent ?x1 - positiontype  ?x2 - positiontype  ?x3 - orientationtype  )
         (turnaround ?x1 - individual  )
         (picture ?x1 - individual  )
         (chair ?x1 - individual  )
         (moveonestep ?x1 - individual  )
         (alarm ?x1 - individual  )
         (red ?x1 - individual  )
         (mustadjoin ?x1 - category  ?x2 - syntaxnode  )
         (movethreesteps ?x1 - individual  )
         (blue ?x1 - individual  )
         (lamp ?x1 - individual  )
         (target ?x1 - individual  )
         (player-orientation ?x1 - orientationtype  )
         (turnleft ?x1 - individual  )
         (canadjoin ?x1 - category  ?x2 - syntaxnode  )
         (todo-2 ?x1 - predicate  ?x2 - individual  ?x3 - individual  )
         (distractor ?x1 - syntaxnode  ?x2 - individual  )
         (todo-1 ?x1 - predicate  ?x2 - individual  )
        )

   (:action aux-An-lower
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (canadjoin n ?u) (forall y (not (and (distractor ?u y) (above ?x y)))))
      :effect (and (not (mustadjoin n ?u)) (forall y (when (above y ?x) (not (distractor ?u y)))) (canadjoin n ?u))
   )


   (:action init-intransImperative-movetwosteps
      :parameters (?x - individual  ?u - syntaxnode  ?p3 - positiontype  ?p2 - positiontype  ?p1 - positiontype  ?o - orientationtype  )
      :precondition (and (referent ?u ?x) (subst s ?u) (movetwosteps ?x) (player-position ?p1) (player-orientation ?o) (adjacent ?p1 ?p2 ?o) (not (blocked ?p1 ?p2)) (not (alarmed ?p2)) (adjacent ?p2 ?p3 ?o) (not (blocked ?p2 ?p3)) (not (alarmed ?p3)))
      :effect (and (not (subst s ?u)) (not (needtoexpress-1 imp-movetwosteps ?x)) (todo-1 imp-movetwosteps ?x) (not (player-position ?p1)) (player-position ?p3) (forall (?y - individual  ) (when (not (and (movetwosteps ?y))) (not (distractor ?u ?y)))) (canadjoin s ?u) (canadjoin vp ?u))
   )


   (:action init-N-door
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (door ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-door ?x)) (forall (?y - individual  ) (when (not (and (door ?y))) (not (distractor ?u ?y)))))
   )


   (:action init-transImperative-push
      :parameters (?x - individual  ?u - syntaxnode  ?x1 - individual  ?u1 - syntaxnode  ?un - syntaxnode  ?o2 - orientationtype  ?o3 - orientationtype  ?p - positiontype  ?o - orientationtype  )
      :precondition (and (current ?u1) (next ?u1 ?un) (referent ?u ?x) (subst s ?u) (push ?x ?x1) (button ?x1) (visible ?p ?o ?x1) (target ?x1) (player-position ?p) (player-orientation ?o) (object-orientation ?x1 ?o3) (next-orientation-left ?o ?o2) (next-orientation-left ?o2 ?o3))
      :effect (and (not (current ?u1)) (current ?un) (not (subst s ?u)) (not (needtoexpress-2 imp-push ?x ?x1)) (todo-2 imp-push ?x ?x1) (forall (?y - individual  ) (when (not (and (push ?y ?x1))) (not (distractor ?u ?y)))) (subst np ?u1) (referent ?u1 ?x1) (forall (?y - individual  ) (when (and (not (= ?y ?x1)) (button ?y) (visible p o ?y)) (distractor ?u1 ?y))) (canadjoin s ?u) (canadjoin vp ?u))
   )


   (:action aux-An-upper
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (canadjoin n ?u) (forall y (not (and (distractor ?u y) (above y ?x)))))
      :effect (and (not (mustadjoin n ?u)) (forall y (when (above ?x y) (not (distractor ?u y)))) (canadjoin n ?u))
   )


   (:action init-N-button
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (button ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-button ?x)) (forall (?y - individual  ) (when (not (and (button ?y))) (not (distractor ?u ?y)))))
   )


   (:action aux-An-left
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (canadjoin n ?u) (forall y (not (and (distractor ?u y) (left-of y ?x)))))
      :effect (and (not (mustadjoin n ?u)) (forall y (when (left-of ?x y) (not (distractor ?u y)))) (canadjoin n ?u))
   )


   (:action aux-An-right
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (canadjoin n ?u) (forall y (not (and (distractor ?u y) (left-of ?x y)))))
      :effect (and (not (mustadjoin n ?u)) (forall y (when (left-of y ?x) (not (distractor ?u y)))) (canadjoin n ?u))
   )


   (:action init-N-flower
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (flower ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-flower ?x)) (forall (?y - individual  ) (when (not (and (flower ?y))) (not (distractor ?u ?y)))))
   )


   (:action init-N-trophy
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (trophy ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-trophy ?x)) (forall (?y - individual  ) (when (not (and (trophy ?y))) (not (distractor ?u ?y)))))
   )


   (:action init-N-safe
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (safe ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-safe ?x)) (forall (?y - individual  ) (when (not (and (safe ?y))) (not (distractor ?u ?y)))))
   )


   (:action aux-An-green
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (canadjoin n ?u) (green ?x))
      :effect (and (not (mustadjoin n ?u)) (not (needtoexpress-1 pred-green ?x)) (forall (?y - individual  ) (when (not (and (green ?y))) (not (distractor ?u ?y)))) (canadjoin n ?u))
   )


   (:action init-intransImperative-turnright
      :parameters (?x - individual  ?u - syntaxnode  ?o2 - orientationtype  ?o1 - orientationtype  )
      :precondition (and (referent ?u ?x) (subst s ?u) (turnright ?x) (player-orientation ?o1) (next-orientation-left ?o2 ?o1))
      :effect (and (not (subst s ?u)) (not (needtoexpress-1 imp-turnright ?x)) (todo-1 imp-turnright ?x) (not (player-orientation ?o1)) (player-orientation ?o2) (forall (?y - individual  ) (when (not (and (turnright ?y))) (not (distractor ?u ?y)))) (canadjoin s ?u) (canadjoin vp ?u))
   )


   (:action init-Dn-the
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst np ?u))
      :effect (and (not (subst np ?u)) (subst n ?u) (referent ?u ?x) (canadjoin np ?u))
   )


   (:action aux-sentConjunction-and
      :parameters (?x - individual  ?u - syntaxnode  ?x1 - individual  ?u1 - syntaxnode  ?un - syntaxnode  )
      :precondition (and (current ?u1) (next ?u1 ?un) (referent ?u ?x) (canadjoin s ?u) (next-referent ?x ?x1) (conj-node ?u1))
      :effect (and (not (current ?u1)) (current ?un) (not (mustadjoin s ?u)) (not (conj-node ?un)) (subst s ?u1) (referent ?u1 ?x1) (canadjoin s ?u))
   )


   (:action init-intransImperative-turnaround
      :parameters (?x - individual  ?u - syntaxnode  ?o2 - orientationtype  ?o1 - orientationtype  ?o3 - orientationtype  )
      :precondition (and (referent ?u ?x) (subst s ?u) (turnaround ?x) (player-orientation ?o1) (next-orientation-left ?o1 ?o2) (next-orientation-left ?o2 ?o3))
      :effect (and (not (subst s ?u)) (not (needtoexpress-1 imp-turnaround ?x)) (todo-1 imp-turnaround ?x) (not (player-orientation ?o1)) (player-orientation ?o3) (forall (?y - individual  ) (when (not (and (turnaround ?y))) (not (distractor ?u ?y)))) (canadjoin s ?u) (canadjoin vp ?u))
   )


   (:action init-N-picture
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (picture ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-picture ?x)) (forall (?y - individual  ) (when (not (and (picture ?y))) (not (distractor ?u ?y)))))
   )


   (:action init-N-chair
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (chair ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-chair ?x)) (forall (?y - individual  ) (when (not (and (chair ?y))) (not (distractor ?u ?y)))))
   )


   (:action init-intransImperative-moveonestep
      :parameters (?x - individual  ?u - syntaxnode  ?p2 - positiontype  ?p1 - positiontype  ?o - orientationtype  )
      :precondition (and (referent ?u ?x) (subst s ?u) (moveonestep ?x) (player-position ?p1) (player-orientation ?o) (adjacent ?p1 ?p2 ?o) (not (blocked ?p1 ?p2)) (not (alarmed ?p2)))
      :effect (and (not (subst s ?u)) (not (needtoexpress-1 imp-moveonestep ?x)) (todo-1 imp-moveonestep ?x) (not (player-position ?p1)) (player-position ?p2) (forall (?y - individual  ) (when (not (and (moveonestep ?y))) (not (distractor ?u ?y)))) (canadjoin s ?u) (canadjoin vp ?u))
   )


   (:action init-N-alarm
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (alarm ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-alarm ?x)) (forall (?y - individual  ) (when (not (and (alarm ?y))) (not (distractor ?u ?y)))))
   )


   (:action aux-An-red
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (canadjoin n ?u) (red ?x))
      :effect (and (not (mustadjoin n ?u)) (not (needtoexpress-1 pred-red ?x)) (forall (?y - individual  ) (when (not (and (red ?y))) (not (distractor ?u ?y)))) (canadjoin n ?u))
   )


   (:action init-intransImperative-movethreesteps
      :parameters (?x - individual  ?u - syntaxnode  ?p4 - positiontype  ?p3 - positiontype  ?p2 - positiontype  ?p1 - positiontype  ?o - orientationtype  )
      :precondition (and (referent ?u ?x) (subst s ?u) (movethreesteps ?x) (player-position ?p1) (player-orientation ?o) (adjacent ?p1 ?p2 ?o) (not (blocked ?p1 ?p2)) (not (alarmed ?p2)) (adjacent ?p2 ?p3 ?o) (not (blocked ?p2 ?p3)) (not (alarmed ?p3)) (adjacent ?p3 ?p4 ?o) (not (blocked ?p3 ?p4)) (not (alarmed ?p4)))
      :effect (and (not (subst s ?u)) (not (needtoexpress-1 imp-movethreesteps ?x)) (todo-1 imp-movethreesteps ?x) (not (player-position ?p1)) (player-position ?p4) (forall (?y - individual  ) (when (not (and (movethreesteps ?y))) (not (distractor ?u ?y)))) (canadjoin s ?u) (canadjoin vp ?u))
   )


   (:action aux-An-blue
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (canadjoin n ?u) (blue ?x))
      :effect (and (not (mustadjoin n ?u)) (not (needtoexpress-1 pred-blue ?x)) (forall (?y - individual  ) (when (not (and (blue ?y))) (not (distractor ?u ?y)))) (canadjoin n ?u))
   )


   (:action aux-sentConjunction-then
      :parameters (?x - individual  ?u - syntaxnode  ?x1 - individual  ?u1 - syntaxnode  ?un - syntaxnode  )
      :precondition (and (current ?u1) (next ?u1 ?un) (referent ?u ?x) (canadjoin s ?u) (next-referent ?x ?x1) (not (conj-node ?u1)))
      :effect (and (not (current ?u1)) (current ?un) (not (mustadjoin s ?u)) (conj-node ?un) (subst s ?u1) (referent ?u1 ?x1) (canadjoin s ?u))
   )


   (:action init-N-lamp
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (lamp ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-lamp ?x)) (forall (?y - individual  ) (when (not (and (lamp ?y))) (not (distractor ?u ?y)))))
   )


   (:action init-intransImperative-turnleft
      :parameters (?x - individual  ?u - syntaxnode  ?o2 - orientationtype  ?o1 - orientationtype  )
      :precondition (and (referent ?u ?x) (subst s ?u) (turnleft ?x) (player-orientation ?o1) (next-orientation-left ?o1 ?o2))
      :effect (and (not (subst s ?u)) (not (needtoexpress-1 imp-turnleft ?x)) (todo-1 imp-turnleft ?x) (not (player-orientation ?o1)) (player-orientation ?o2) (forall (?y - individual  ) (when (not (and (turnleft ?y))) (not (distractor ?u ?y)))) (canadjoin s ?u) (canadjoin vp ?u))
   )

)
