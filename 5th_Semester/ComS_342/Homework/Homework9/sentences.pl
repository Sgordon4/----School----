sentence(S) :- s(S, []).

s --> f.
s --> t, n, t.

f --> [if], b, [then], s.
f --> [if], b, [then], s, [else], s.

b --> t, e, t.

t --> [x].
t --> [y].
t --> [z].
t --> [1].
t --> [0].

e --> [>].
e --> [<].

n --> [+].
n --> [-].
n --> [=].


% Student exercise profile
:- set_prolog_flag(occurs_check, error).        % disallow cyclic terms
:- set_prolog_stack(global, limit(8 000 000)).  % limit term space (8Mb)
:- set_prolog_stack(local,  limit(2 000 000)).  % limit environment space

% Your program goes here

sentence(S) :- s(S, []).

s(L1, L2) :- f(L1, L2).
s(L1, L4) :- t(L1, L2), n(L2, L3), t(L3, L4).

f(L1, L5) :- if(L1, L2), b(L2, L3), 
    			then(L3, L4), s(L4, L5).
f(L1, L7) :- if(L1, L2), b(L2, L3), then(L3, L4), 
    			s(L4, L5), else(L5, L6), s(L6, L7).

b(L1, L4) :- t(L1, L2), e(L2, L3), t(L3, L4).

if([if|Tail], Tail).
then([then|Tail], Tail).
else([else|Tail], Tail).

t([x|Tail], Tail).
t([y|Tail], Tail).
t([z|Tail], Tail).
t([1|Tail], Tail).
t([0|Tail], Tail).

e([<|Tail], Tail).
e([>|Tail], Tail).

n([+|Tail], Tail).
n([-|Tail], Tail).
n([=|Tail], Tail).

/** <examples> Your example queries go here, e.g.
?- member(X, [cat, mouse]).
*/
/*
sentence([if, x, >, 0, then, [x, =, 1]]).
 */
