Simply a Voldemort Scala driver (client).

Main feature is some wrapping using Option, and an init and applyDelta
function that are hand.

applyDelta -> allows you to run a function against a versioned value and
specify uodate behavior that way. I use it mostly to e.g. add an item to a
sequence. As in turning List (1,2) -> List (1,2,3) as value for a key.

init -> initializes only when a key doesn't exist, otherwise leave it be.

To compile, run ./sbt
Then type compile

Enjoy!
