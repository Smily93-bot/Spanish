"""
Reference rules and solver for the ship levels (ShipLevels.kt mirrors the level text and
ShipGame.kt mirrors these rules). Run: python3 tools/ship_puzzles.py [levels.json]

Tiles (one character each):
  #  wall                 .  floor              P  start
  0-6 learning rooms (pads, played in order; answering gives the reward key, see "rewards")
  E  exit (opens when all 7 rooms are done)
  r b g y  keys lying on the floor          R B G Y  doors of that colour (a key is used up)
  c  battery (push it)    o  charger         H  gate: open while every charger has a battery
  T  teleporter pair      U  second teleporter pair (step on one -> arrive on the other)
  x  switch: toggles every force field     =  field that starts ON    -  field that starts OFF
  m  robot moving left/right               w  robot moving up/down   (they move once per step)
  f  spot for a fetch item (the fetch room wants one of them)
  *  diamond (bonus)

Robots walk on floor and diamonds only, turn around when blocked, and a robot touching Lía sends
her back to the last checkpoint (the start or the last room she finished).
"""
import json, sys
from collections import deque

DIRS = {'U': (0, -1), 'D': (0, 1), 'L': (-1, 0), 'R': (1, 0)}
DOORS = 'RBGY'
KEYS = 'rbgy'
CRATE_OK = set('.oP')       # tiles a battery may be pushed onto
ROBOT_OK = set('.*')        # tiles robots walk on


class Level:
    def __init__(self, spec):
        self.rows = spec['rows']
        self.h = len(self.rows)
        self.w = len(self.rows[0])
        assert all(len(r) == self.w for r in self.rows), 'ragged rows'
        self.rewards = spec.get('rewards', '.......')
        assert len(self.rewards) == 7
        self.fetch_pad = spec.get('fetchPad', -1)
        self.grid = {}
        self.start = None
        self.crates = []
        self.robots = []
        self.chargers = []
        self.tele = {}
        self.fetch_spots = []
        for y, row in enumerate(self.rows):
            for x, c in enumerate(row):
                p = (x, y)
                if c == 'P':
                    self.start = p
                if c == 'c':
                    self.crates.append(p); c = '.'
                if c in 'mw':
                    self.robots.append((p, (1, 0) if c == 'm' else (0, 1))); c = '.'
                if c == 'o':
                    self.chargers.append(p)
                if c in 'TU':
                    self.tele.setdefault(c, []).append(p)
                if c == 'f':
                    self.fetch_spots.append(p)
                self.grid[p] = c
        for k, v in self.tele.items():
            assert len(v) == 2, 'teleporter %s needs exactly 2 ends' % k
        for d in '0123456':
            assert sum(r.count(d) for r in self.rows) == 1, 'room %s missing or doubled' % d
        assert sum(r.count('E') for r in self.rows) == 1 and self.start

    def at(self, p):
        return self.grid.get(p, '#')


class State:
    __slots__ = ('pos', 'keys', 'opened', 'taken', 'crates', 'toggled', 'robots', 'solved', 'carry', 'items')

    def __init__(self, **kw):
        for k in self.__slots__:
            setattr(self, k, kw[k])

    def key(self):
        return (self.pos, self.keys, self.opened, self.taken, self.crates, self.toggled, self.robots, self.solved, self.carry, self.items)

    def copy(self, **kw):
        d = {k: getattr(self, k) for k in self.__slots__}
        d.update(kw)
        return State(**d)


def initial(level):
    return State(pos=level.start, keys=(0, 0, 0, 0), opened=frozenset(), taken=frozenset(),
                 crates=tuple(level.crates), toggled=False, robots=tuple(level.robots), solved=0,
                 carry=-1, items=tuple(range(len(level.fetch_spots))))


def field_on(level, s, p):
    c = level.at(p)
    return (c == '=') != s.toggled if c in '=-' else False


def gate_open(level, s):
    return all(ch in s.crates for ch in level.chargers)


def blocked_for_walk(level, s, p):
    c = level.at(p)
    if c == '#':
        return True
    if c in DOORS and p not in s.opened:
        return s.keys[DOORS.index(c)] == 0
    if c == 'H' and not gate_open(level, s):
        return True
    if field_on(level, s, p):
        return True
    return False


def step(level, s, d, wanted=0):
    """Returns (new_state, event) or (None, reason). event: None, 'hit', 'win'."""
    dx, dy = DIRS[d]
    n = (s.pos[0] + dx, s.pos[1] + dy)
    if blocked_for_walk(level, s, n):
        return None, 'blocked'
    keys, opened, taken, crates = list(s.keys), s.opened, s.taken, list(s.crates)
    c = level.at(n)
    if c in DOORS and n not in opened:
        keys[DOORS.index(c)] -= 1
        opened = opened | {n}
    if n in crates:
        b = (n[0] + dx, n[1] + dy)
        robot_cells = {r[0] for r in s.robots}
        if level.at(b) not in CRATE_OK or b in crates or b in robot_cells:
            return None, 'crate'
        crates[crates.index(n)] = b
    if any(r[0] == n for r in s.robots):
        return None, 'hit'
    pos = n
    toggled = s.toggled
    carry, items = s.carry, s.items
    solved = s.solved
    if c in KEYS and n not in taken:
        keys[KEYS.index(c)] += 1
        taken = taken | {n}
    if c == 'x':
        toggled = not toggled
    if c in 'TU':
        a, b2 = level.tele[c]
        other = b2 if n == a else a
        if other not in crates and all(r[0] != other for r in s.robots):
            pos = other
    if c == 'f':
        i = level.fetch_spots.index(n)
        if i in items:
            items = tuple(x for x in items if x != i) + ((carry,) if carry >= 0 else ())
            carry = i
    if c.isdigit() and int(c) == solved:
        room = int(c)
        if room == level.fetch_pad:
            if carry == wanted:
                carry = -1
                solved += 1
        else:
            solved += 1
        if solved > s.solved:
            r = level.rewards[room]
            if r in KEYS:
                keys[KEYS.index(r)] += 1
    ns = s.copy(pos=pos, keys=tuple(keys), opened=opened, taken=taken, crates=tuple(crates),
                toggled=toggled, carry=carry, items=items, solved=solved)
    # Robots take their step.
    robots = []
    taken_cells = set(crates)
    for i, (rp, rd) in enumerate(ns.robots):
        moved = None
        for dd in (rd, (-rd[0], -rd[1])):
            t = (rp[0] + dd[0], rp[1] + dd[1])
            others = {r[0] for j, r in enumerate(robots)} | {r[0] for j, r in enumerate(ns.robots) if j > i}
            if level.at(t) in ROBOT_OK and not field_on(level, ns, t) and t not in taken_cells and t not in others:
                moved = (t, dd)
                break
        robots.append(moved or (rp, rd))
    ns = ns.copy(robots=tuple(robots))
    old_robot = {r[0] for r in s.robots}
    for (rp, _), (op, _) in zip(robots, s.robots):
        if rp == pos or (rp == s.pos and op == pos):
            return ns, 'hit'
    if level.at(pos) == 'E' and solved == 7:
        return ns, 'win'
    return ns, None


def solve(level, wanted=0, limit=3_000_000):
    s0 = initial(level)
    seen = {s0.key(): None}
    q = deque([s0])
    while q:
        s = q.popleft()
        for d in 'UDLR':
            ns, ev = step(level, s, d, wanted)
            if ns is None or ev == 'hit':
                continue
            k = ns.key()
            if k in seen:
                continue
            seen[k] = (s.key(), d)
            if ev == 'win':
                path = []
                while seen[k] is not None:
                    pk, dd = seen[k]
                    path.append(dd)
                    k = pk
                return ''.join(reversed(path)), len(seen)
            q.append(ns)
            if len(seen) > limit:
                return None, len(seen)
    return None, len(seen)


def replay(level, moves, wanted=0):
    s = initial(level)
    for d in moves:
        s, ev = step(level, s, d, wanted)
        assert s is not None and ev != 'hit', 'bad move %s' % d
        if ev == 'win':
            return True
    return False


def check(spec, name=''):
    level = Level(spec)
    wanted_options = range(len(level.fetch_spots)) if level.fetch_pad >= 0 else [0]
    best = None
    for w in wanted_options:
        sol, n = solve(level, w)
        assert sol, '%s: unsolvable (wanted=%d, %d states)' % (name, w, n)
        best = sol if best is None or len(sol) > len(best) else best
    return best


if __name__ == '__main__':
    data = json.load(open(sys.argv[1] if len(sys.argv) > 1 else 'tools/ship_levels.json'))
    for i, spec in enumerate(data):
        sol = check(spec, spec.get('name', str(i)))
        print(i + 1, spec.get('name'), len(sol), sol)
