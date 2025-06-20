Jug == 1 .. 2
Capacity == [
  j \in Jug |->
      IF j = 1 THEN 3
      ELSE 5
\*    IF j = 1 THEN 3
\*    ELSE IF j = 2 THEN 5
\*    ELSE IF j = 3 THEN 7
\*    ELSE IF j = 4 THEN 8
\*    ELSE IF j = 5 THEN 9
\*    ELSE 0
]
Goal == 4

VARIABLE contents

Min(m, n) == IF m < n THEN m ELSE n

FillJug(target) ==
  contents' = [
    j \in Jug |->
      IF j = target THEN Capacity[j] ELSE contents[j]
  ]

EmptyJug(target) ==
  contents' = [
    j \in Jug |->
      IF j = target THEN 0 ELSE contents[j]
  ]

JugToJug(from, to) ==
  /\ ~(from = to)
  /\ contents' = [
      j \in Jug |->
        IF j = from THEN
          contents[j] - Min(contents[from], Capacity[to] - contents[to])
        ELSE IF j = to THEN
          contents[j] + Min(contents[from], Capacity[to] - contents[to])
        ELSE
          contents[j]
    ]

TypeOK == \A j \in Jug : contents[j] \in 0 .. Capacity[j]

Init == contents = [j \in Jug |-> 0]

Next ==
  \E j \in Jug :
    \/ FillJug(j)
    \/ EmptyJug(j)
    \/ \E k \in Jug : JugToJug(j, k)

Inv == \A j \in Jug : ~(contents[j] = Goal)

