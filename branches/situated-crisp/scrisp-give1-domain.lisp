(define (domain scrisp-give1)
        (:requirements :strips :equality :typing :conditional-effects :quantified-preconditions)
        (:types  imperative - predicate  category - object  individual - object  orientationtype - object  predicate - object  syntaxnode - object  positiontype - object)
       (:constants
         pred-safe - predicate
         imp-movethreesteps - imperative
         np - category
         conj - category
         imp-turnleft - imperative
         pred-trophy - predicate
         pred-flower - predicate
         pred-door - predicate
         vp - category
         imp-turnright - imperative
         imp-push - imperative
         pred-blue - predicate
         pred-button - predicate
         pred-lamp - predicate
         d - category
         pred-picture - predicate
         imp-moveonestep - imperative
         imp-movetwosteps - imperative
         pred-red - predicate
         pred-green - predicate
         a - category
         n - category
         pred-chair - predicate
         v - category
         pred-alarm - predicate
         s - category
         imp-turnaround - imperative
       )
       (:predicates
         (alarmed ?x1 - positiontype  )
         (visible ?x1 - positiontype  ?x2 - orientationtype  ?x3 - individual  )
         (left-of ?x1 - individual  ?x2 - individual  )
         (next ?x1 - syntaxnode  ?x2 - syntaxnode  )
         (movetwosteps )
         (door ?x1 - individual  )
         (next-referent ?x1 - individual  ?x2 - individual  )
         (push ?x1 - individual  )
         (object-position ?x1 - individual  ?x2 - positiontype  )
         (needtoexpress-1 ?x1 - predicate  ?x2 - individual  )
         (needtoexpress-0 ?x1 - predicate  )
         (subst ?x1 - category  ?x2 - syntaxnode  )
         (button ?x1 - individual  )
         (next-orientation-left ?x1 - orientationtype  ?x2 - orientationtype  )
         (current ?x1 - syntaxnode  )
         (above ?x1 - individual  ?x2 - individual  )
         (blocked ?x1 - positiontype  ?x2 - positiontype  )
         (conj-node ?x1 - individual  )
         (flower ?x1 - individual  )
         (player-position ?x1 - positiontype  )
         (safe ?x1 - individual  )
         (trophy ?x1 - individual  )
         (referent ?x1 - syntaxnode  ?x2 - individual  )
         (object-orientation ?x1 - individual  ?x2 - orientationtype  )
         (green ?x1 - individual  )
         (turnright )
         (adjacent ?x1 - positiontype  ?x2 - positiontype  ?x3 - orientationtype  )
         (turnaround )
         (picture ?x1 - individual  )
         (chair ?x1 - individual  )
         (moveonestep )
         (alarm ?x1 - individual  )
         (red ?x1 - individual  )
         (mustadjoin ?x1 - category  ?x2 - syntaxnode  )
         (movethreesteps )
         (blue ?x1 - individual  )
         (lamp ?x1 - individual  )
         (target ?x1 - individual  )
         (player-orientation ?x1 - orientationtype  )
         (turnleft )
         (todo-0 ?x1 - predicate  )
         (canadjoin ?x1 - category  ?x2 - syntaxnode  )
         (distractor ?x1 - syntaxnode  ?x2 - individual  )
         (todo-1 ?x1 - predicate  ?x2 - individual  )
        )

   (:action aux-An-lower
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (canadjoin n ?u) (forall (?y - individual) (not (and (distractor ?u ?y) (above ?x ?y)))))
      :effect (and (not (mustadjoin n ?u)) (forall (?y - individual) (when (above ?y ?x) (not (distractor ?u ?y)))) (canadjoin n ?u) (canadjoin a ?u))
   )


   (:action init-intransImperative-movetwosteps
      :parameters (?x - individual  ?u - syntaxnode  ?p3 - positiontype  ?p2 - positiontype  ?p1 - positiontype  ?o - orientationtype  )
      :precondition (and (referent ?u ?x) (subst s ?u) (player-position ?p1) (player-orientation ?o) (adjacent ?p1 ?p2 ?o) (not (blocked ?p1 ?p2)) (not (alarmed ?p2)) (adjacent ?p2 ?p3 ?o) (not (blocked ?p2 ?p3)) (not (alarmed ?p3)))
      :effect (and (not (subst s ?u)) (not (needtoexpress-0 imp-movetwosteps)) (todo-0 imp-movetwosteps) (not (player-position ?p1)) (player-position ?p3) (canadjoin s ?u) (canadjoin v ?u) (canadjoin vp ?u))
   )


   (:action init-N-door
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (door ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-door ?x)) (forall (?y - individual  ) (when (not (and (door ?y))) (not (distractor ?u ?y)))) (canadjoin n ?u))
   )


   (:action init-transImperative-push
      :parameters (?x - individual  ?u - syntaxnode  ?x1 - individual  ?u1 - syntaxnode  ?un - syntaxnode  ?o2 - orientationtype  ?o3 - orientationtype  ?p - positiontype  ?o - orientationtype  )
      :precondition (and (current ?u1) (next ?u1 ?un) (referent ?u ?x) (subst s ?u) (button ?x1) (visible ?p ?o ?x1) (target ?x1) (player-position ?p) (player-orientation ?o) (object-orientation ?x1 ?o3) (next-orientation-left ?o ?o2) (next-orientation-left ?o2 ?o3))
      :effect (and (not (current ?u1)) (current ?un) (not (subst s ?u)) (not (needtoexpress-1 imp-push ?x1)) (todo-1 imp-push ?x1) (subst np ?u1) (referent ?u1 ?x1) (canadjoin s ?u) (canadjoin v ?u) (canadjoin vp ?u))
   )


   (:action aux-An-upper
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (canadjoin n ?u) (forall (?y - individual) (not (and (distractor ?u ?y) (above ?y ?x)))))
      :effect (and (not (mustadjoin n ?u)) (forall (?y - individual) (when (above ?x ?y) (not (distractor ?u ?y)))) (canadjoin n ?u) (canadjoin a ?u))
   )


   (:action init-N-button
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (button ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-button ?x)) (forall (?y - individual  ) (when (not (and (button ?y))) (not (distractor ?u ?y)))) (canadjoin n ?u))
   )


   (:action aux-An-left
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (canadjoin n ?u) (forall (?y - individual) (not (and (distractor ?u ?y) (left-of ?y ?x)))))
      :effect (and (not (mustadjoin n ?u)) (forall (?y - individual) (when (left-of ?x ?y) (not (distractor ?u ?y)))) (canadjoin n ?u) (canadjoin a ?u))
   )


   (:action aux-An-right
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (canadjoin n ?u) (forall (?y - individual) (not (and (distractor ?u ?y) (left-of ?x ?y)))))
      :effect (and (not (mustadjoin n ?u)) (forall (?y - individual) (when (left-of ?y ?x) (not (distractor ?u ?y)))) (canadjoin n ?u) (canadjoin a ?u))
   )


   (:action init-N-flower
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (flower ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-flower ?x)) (forall (?y - individual  ) (when (not (and (flower ?y))) (not (distractor ?u ?y)))) (canadjoin n ?u))
   )


   (:action init-N-trophy
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (trophy ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-trophy ?x)) (forall (?y - individual  ) (when (not (and (trophy ?y))) (not (distractor ?u ?y)))) (canadjoin n ?u))
   )


   (:action init-N-safe
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (safe ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-safe ?x)) (forall (?y - individual  ) (when (not (and (safe ?y))) (not (distractor ?u ?y)))) (canadjoin n ?u))
   )


   (:action aux-An-green
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (canadjoin n ?u) (green ?x))
      :effect (and (not (mustadjoin n ?u)) (not (needtoexpress-1 pred-green ?x)) (forall (?y - individual  ) (when (not (and (green ?y))) (not (distractor ?u ?y)))) (canadjoin n ?u) (canadjoin a ?u))
   )


   (:action init-intransImperative-turnright
      :parameters (?x - individual  ?u - syntaxnode  ?o2 - orientationtype  ?o1 - orientationtype  )
      :precondition (and (referent ?u ?x) (subst s ?u) (player-orientation ?o1) (next-orientation-left ?o2 ?o1))
      :effect (and (not (subst s ?u)) (not (needtoexpress-0 imp-turnright)) (todo-0 imp-turnright) (not (player-orientation ?o1)) (player-orientation ?o2) (canadjoin s ?u) (canadjoin v ?u) (canadjoin vp ?u))
   )


   (:action init-Dn-the
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst np ?u))
      :effect (and (not (subst np ?u)) (subst n ?u) (referent ?u ?x) (canadjoin np ?u) (canadjoin d ?u))
   )


   (:action aux-sentConjunction-and
      :parameters (?x - individual  ?u - syntaxnode  ?x1 - individual  ?u1 - syntaxnode  ?un - syntaxnode  )
      :precondition (and (current ?u1) (next ?u1 ?un) (referent ?u ?x) (canadjoin s ?u) (next-referent ?x ?x1) (conj-node foo))
      :effect (and (not (current ?u1)) (current ?un) (not (mustadjoin s ?u)) (not (conj-node foo)) (subst s ?u1) (referent ?u1 ?x1) (canadjoin s ?u) (canadjoin conj ?u))
   )


   (:action init-intransImperative-turnaround
      :parameters (?x - individual  ?u - syntaxnode  ?o2 - orientationtype  ?o1 - orientationtype  ?o3 - orientationtype  )
      :precondition (and (referent ?u ?x) (subst s ?u) (player-orientation ?o1) (next-orientation-left ?o1 ?o2) (next-orientation-left ?o2 ?o3))
      :effect (and (not (subst s ?u)) (not (needtoexpress-0 imp-turnaround)) (todo-0 imp-turnaround) (not (player-orientation ?o1)) (player-orientation ?o3) (canadjoin s ?u) (canadjoin v ?u) (canadjoin vp ?u))
   )


   (:action init-N-picture
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (picture ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-picture ?x)) (forall (?y - individual  ) (when (not (and (picture ?y))) (not (distractor ?u ?y)))) (canadjoin n ?u))
   )


   (:action init-N-chair
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (chair ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-chair ?x)) (forall (?y - individual  ) (when (not (and (chair ?y))) (not (distractor ?u ?y)))) (canadjoin n ?u))
   )


   (:action init-intransImperative-moveonestep
      :parameters (?x - individual  ?u - syntaxnode  ?p2 - positiontype  ?p1 - positiontype  ?o - orientationtype  )
      :precondition (and (referent ?u ?x) (subst s ?u) (player-position ?p1) (player-orientation ?o) (adjacent ?p1 ?p2 ?o) (not (blocked ?p1 ?p2)) (not (alarmed ?p2)))
      :effect (and (not (subst s ?u)) (not (needtoexpress-0 imp-moveonestep)) (todo-0 imp-moveonestep) (not (player-position ?p1)) (player-position ?p2) (canadjoin s ?u) (canadjoin v ?u) (canadjoin vp ?u))
   )


   (:action init-N-alarm
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (alarm ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-alarm ?x)) (forall (?y - individual  ) (when (not (and (alarm ?y))) (not (distractor ?u ?y)))) (canadjoin n ?u))
   )


   (:action aux-An-red
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (canadjoin n ?u) (red ?x))
      :effect (and (not (mustadjoin n ?u)) (not (needtoexpress-1 pred-red ?x)) (forall (?y - individual  ) (when (not (and (red ?y))) (not (distractor ?u ?y)))) (canadjoin n ?u) (canadjoin a ?u))
   )


   (:action init-intransImperative-movethreesteps
      :parameters (?x - individual  ?u - syntaxnode  ?p4 - positiontype  ?p3 - positiontype  ?p2 - positiontype  ?p1 - positiontype  ?o - orientationtype  )
      :precondition (and (referent ?u ?x) (subst s ?u) (player-position ?p1) (player-orientation ?o) (adjacent ?p1 ?p2 ?o) (not (blocked ?p1 ?p2)) (not (alarmed ?p2)) (adjacent ?p2 ?p3 ?o) (not (blocked ?p2 ?p3)) (not (alarmed ?p3)) (adjacent ?p3 ?p4 ?o) (not (blocked ?p3 ?p4)) (not (alarmed ?p4)))
      :effect (and (not (subst s ?u)) (not (needtoexpress-0 imp-movethreesteps)) (todo-0 imp-movethreesteps) (not (player-position ?p1)) (player-position ?p4) (canadjoin s ?u) (canadjoin v ?u) (canadjoin vp ?u))
   )


   (:action aux-An-blue
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (canadjoin n ?u) (blue ?x))
      :effect (and (not (mustadjoin n ?u)) (not (needtoexpress-1 pred-blue ?x)) (forall (?y - individual  ) (when (not (and (blue ?y))) (not (distractor ?u ?y)))) (canadjoin n ?u) (canadjoin a ?u))
   )


   (:action aux-sentConjunction-then
      :parameters (?x - individual  ?u - syntaxnode  ?x1 - individual  ?u1 - syntaxnode  ?un - syntaxnode  )
      :precondition (and (current ?u1) (next ?u1 ?un) (referent ?u ?x) (canadjoin s ?u) (next-referent ?x ?x1) (not (conj-node foo)))
      :effect (and (not (current ?u1)) (current ?un) (not (mustadjoin s ?u)) (conj-node foo) (subst s ?u1) (referent ?u1 ?x1) (canadjoin s ?u) (canadjoin conj ?u))
   )


   (:action init-N-lamp
      :parameters (?x - individual  ?u - syntaxnode  )
      :precondition (and (referent ?u ?x) (subst n ?u) (lamp ?x))
      :effect (and (not (subst n ?u)) (not (needtoexpress-1 pred-lamp ?x)) (forall (?y - individual  ) (when (not (and (lamp ?y))) (not (distractor ?u ?y)))) (canadjoin n ?u))
   )


   (:action init-intransImperative-turnleft
      :parameters (?x - individual  ?u - syntaxnode  ?o2 - orientationtype  ?o1 - orientationtype  )
      :precondition (and (referent ?u ?x) (subst s ?u) (player-orientation ?o1) (next-orientation-left ?o1 ?o2))
      :effect (and (not (subst s ?u)) (not (needtoexpress-0 imp-turnleft)) (todo-0 imp-turnleft) (not (player-orientation ?o1)) (player-orientation ?o2) (canadjoin s ?u) (canadjoin v ?u) (canadjoin vp ?u))
   )

)
