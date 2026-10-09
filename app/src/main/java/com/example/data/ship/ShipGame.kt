package com.example.data.ship

import org.json.JSONArray

/**
 * The ship levels: hand-made puzzle maps in the style of Chip's Challenge, turn based (robots move
 * only when Lía moves). The rules mirror tools/ship_puzzles.py, which checks every level is solvable.
 *
 * Tiles: # wall · . floor · P start · 0–6 learning rooms (in order) · E exit ·
 * r b g y keys · R B G Y doors (a key is used up) · c battery · o charger · H gate (open while every
 * charger holds a battery) · T/U teleporter pairs · x switch · = field on · - field off ·
 * m/w robots (left-right / up-down) · f fetch spot · * diamond.
 */
data class Cell(val x: Int, val y: Int) {
    operator fun plus(o: Cell) = Cell(x + o.x, y + o.y)
    operator fun unaryMinus() = Cell(-x, -y)
}

val UP = Cell(0, -1)
val DOWN = Cell(0, 1)
val LEFT = Cell(-1, 0)
val RIGHT = Cell(1, 0)

const val DOOR_COLORS = "RBGY"
const val KEY_COLORS = "rbgy"

data class ShipRobot(val at: Cell, val dir: Cell)

class ShipLevel(
    val name: String,
    val rows: List<String>,
    /** Key colour (r/b/g/y or '.') each learning room gives when its game is won. */
    val rewards: String,
    /** The room that asks for a fetched thing, or -1. */
    val fetchPad: Int,
    val newEn: String,
    val newAr: String,
    val solution: String
) {
    val width = rows.first().length
    val height = rows.size
    private val grid: Array<CharArray>
    val start: Cell
    val crates = mutableListOf<Cell>()
    val robots = mutableListOf<ShipRobot>()
    val chargers = mutableListOf<Cell>()
    val fetchSpots = mutableListOf<Cell>()
    private val teleporters = HashMap<Char, MutableList<Cell>>()
    val pads: List<Cell>
    val exit: Cell

    init {
        var s: Cell? = null
        grid = Array(height) { y ->
            CharArray(width) { x ->
                val p = Cell(x, y)
                when (val c = rows[y].getOrElse(x) { '#' }) {
                    'P' -> { s = p; c }
                    'c' -> { crates += p; '.' }
                    'm' -> { robots += ShipRobot(p, RIGHT); '.' }
                    'w' -> { robots += ShipRobot(p, DOWN); '.' }
                    'o' -> { chargers += p; c }
                    'T', 'U' -> { teleporters.getOrPut(c) { mutableListOf() } += p; c }
                    'f' -> { fetchSpots += p; c }
                    else -> c
                }
            }
        }
        start = s ?: Cell(1, 1)
        pads = (0..6).map { find('0' + it) }
        exit = find('E')
    }

    private fun find(c: Char): Cell {
        grid.forEachIndexed { y, row -> val x = row.indexOf(c); if (x >= 0) return Cell(x, y) }
        return Cell(1, 1)
    }

    fun at(p: Cell): Char = grid.getOrNull(p.y)?.getOrNull(p.x) ?: '#'

    /** The other end of the teleporter at [p], if [p] is one. */
    fun otherEnd(p: Cell): Cell? = teleporters[at(p)]?.firstOrNull { it != p }

    fun cells(): Sequence<Cell> = sequence { for (y in 0 until height) for (x in 0 until width) yield(Cell(x, y)) }

    companion object {
        fun parseAll(json: String): List<ShipLevel> {
            val arr = JSONArray(json)
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val rows = o.getJSONArray("rows")
                ShipLevel(
                    name = o.optString("name"),
                    rows = (0 until rows.length()).map { rows.getString(it) },
                    rewards = o.optString("rewards", ".......").padEnd(7, '.'),
                    fetchPad = o.optInt("fetchPad", -1),
                    newEn = o.optString("newEn"),
                    newAr = o.optString("newAr"),
                    solution = o.optString("solution")
                )
            }
        }
    }
}

/** Everything that changes while playing a level (immutable, so undo is just a list of states). */
data class ShipState(
    val pos: Cell,
    val keys: List<Int> = listOf(0, 0, 0, 0),
    val opened: Set<Cell> = emptySet(),
    val takenKeys: Set<Cell> = emptySet(),
    val crates: List<Cell>,
    val toggled: Boolean = false,
    val robots: List<ShipRobot>,
    /** Learning rooms finished (they are done in order 0, 1, 2…). */
    val solved: Int = 0,
    /** Index of the fetch spot whose thing Lía carries, or -1. */
    val carry: Int = -1,
    /** Fetch spots that still have their thing lying on them. */
    val items: List<Int>,
    val diamonds: Set<Cell> = emptySet(),
    val facing: Cell = RIGHT
)

sealed interface ShipEvent {
    data object Moved : ShipEvent
    data class Blocked(val tile: Char) : ShipEvent
    data object Hit : ShipEvent
    data object Win : ShipEvent
    /** Lía reached the next learning room: open its game. */
    data class RoomOpen(val room: Int) : ShipEvent
    /** The fetch room needs a thing Lía isn't carrying. */
    data class NeedThing(val room: Int, val carrying: Int) : ShipEvent
}

/** One step result: the new state (unchanged when blocked), what happened, and small extras for sounds. */
data class ShipStep(
    val state: ShipState,
    val event: ShipEvent,
    val gotKey: Char? = null,
    val pushed: Boolean = false,
    val switched: Boolean = false,
    val teleported: Boolean = false,
    val gotDiamond: Boolean = false,
    val pickedThing: Boolean = false,
    val openedDoor: Boolean = false
)

object ShipRules {
    private const val CRATE_OK = ".oP"
    private const val ROBOT_OK = ".*"

    fun start(level: ShipLevel) = ShipState(
        pos = level.start,
        crates = level.crates.toList(),
        robots = level.robots.toList(),
        items = level.fetchSpots.indices.toList()
    )

    fun fieldOn(level: ShipLevel, s: ShipState, p: Cell): Boolean {
        val c = level.at(p)
        return if (c == '=' || c == '-') (c == '=') != s.toggled else false
    }

    fun gateOpen(level: ShipLevel, s: ShipState) = level.chargers.all { it in s.crates }

    fun doorOpen(level: ShipLevel, s: ShipState, p: Cell) = p in s.opened

    private fun blocked(level: ShipLevel, s: ShipState, p: Cell): Boolean {
        val c = level.at(p)
        return when {
            c == '#' -> true
            c in DOOR_COLORS && p !in s.opened -> s.keys[DOOR_COLORS.indexOf(c)] == 0
            c == 'H' && !gateOpen(level, s) -> true
            fieldOn(level, s, p) -> true
            else -> false
        }
    }

    /** Lía tries to walk one tile in [dir]. [wanted] is the fetch spot whose thing the fetch room wants. */
    fun step(level: ShipLevel, s: ShipState, dir: Cell, wanted: Int): ShipStep {
        val n = s.pos + dir
        val turned = s.copy(facing = if (dir.x != 0) dir else s.facing)
        if (blocked(level, s, n)) return ShipStep(turned, ShipEvent.Blocked(level.at(n)))
        val keys = s.keys.toMutableList()
        var opened = s.opened
        var taken = s.takenKeys
        val crates = s.crates.toMutableList()
        val c = level.at(n)
        var openedDoor = false
        if (c in DOOR_COLORS && n !in opened) {
            keys[DOOR_COLORS.indexOf(c)]--
            opened = opened + n
            openedDoor = true
        }
        var pushed = false
        val crateIndex = crates.indexOf(n)
        if (crateIndex >= 0) {
            val beyond = n + dir
            if (level.at(beyond) !in CRATE_OK || beyond in crates || s.robots.any { it.at == beyond }) {
                return ShipStep(turned, ShipEvent.Blocked('c'))
            }
            crates[crateIndex] = beyond
            pushed = true
        }
        if (s.robots.any { it.at == n }) return ShipStep(turned, ShipEvent.Hit)

        var pos = n
        var toggled = s.toggled
        var carry = s.carry
        var items = s.items
        var diamonds = s.diamonds
        var gotKey: Char? = null
        var switched = false
        var teleported = false
        var gotDiamond = false
        var picked = false
        var event: ShipEvent = ShipEvent.Moved
        if (c in KEY_COLORS && n !in taken) {
            keys[KEY_COLORS.indexOf(c)]++
            taken = taken + n
            gotKey = c
        }
        if (c == '*' && n !in diamonds) {
            diamonds = diamonds + n
            gotDiamond = true
        }
        if (c == 'x') {
            toggled = !toggled
            switched = true
        }
        if (c == 'T' || c == 'U') {
            val other = level.otherEnd(n)
            if (other != null && other !in crates && s.robots.none { it.at == other }) {
                pos = other
                teleported = true
            }
        }
        if (c == 'f') {
            val i = level.fetchSpots.indexOf(n)
            if (i in items) {
                items = items.filter { it != i } + (if (carry >= 0) listOf(carry) else emptyList())
                carry = i
                picked = true
            }
        }
        if (c.isDigit() && c - '0' == s.solved) {
            val room = c - '0'
            event = if (room == level.fetchPad && carry != wanted) {
                ShipEvent.NeedThing(room, carry)
            } else {
                if (room == level.fetchPad) carry = -1
                ShipEvent.RoomOpen(room)
            }
        }
        var ns = turned.copy(
            pos = pos, keys = keys, opened = opened, takenKeys = taken, crates = crates,
            toggled = toggled, carry = carry, items = items, diamonds = diamonds
        )
        // Robots take one step each (turning round when blocked).
        val moved = mutableListOf<ShipRobot>()
        ns.robots.forEachIndexed { i, r ->
            var next: ShipRobot? = null
            for (d in listOf(r.dir, -r.dir)) {
                val t = r.at + d
                val others = moved.map { it.at } + ns.robots.drop(i + 1).map { it.at }
                if (level.at(t) in ROBOT_OK && !fieldOn(level, ns, t) && t !in crates && t !in others) {
                    next = ShipRobot(t, d)
                    break
                }
            }
            moved += next ?: r
        }
        ns = ns.copy(robots = moved)
        val hit = moved.zip(s.robots).any { (now, before) -> now.at == pos || (now.at == s.pos && before.at == pos) }
        if (hit) return ShipStep(ns, ShipEvent.Hit)
        if (pos == level.exit && ns.solved == 7) event = ShipEvent.Win
        return ShipStep(ns, event, gotKey, pushed, switched, teleported, gotDiamond, picked, openedDoor)
    }

    /**
     * Can Lía still reach the next learning room (or the exit) from [s]? A breadth-first search over
     * her moves; used to notice a dead end (e.g. a key spent on the wrong door) and offer a restart.
     * Returns true when the search gets too big to be sure.
     */
    fun canProgress(level: ShipLevel, s: ShipState, wanted: Int, limit: Int = 60_000): Boolean {
        val start = s.copy(facing = RIGHT)
        val seen = HashSet<ShipState>()
        seen += start
        val queue = ArrayDeque(listOf(start))
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            for (d in listOf(UP, DOWN, LEFT, RIGHT)) {
                val r = step(level, cur, d, wanted)
                when (r.event) {
                    is ShipEvent.RoomOpen, ShipEvent.Win -> return true
                    is ShipEvent.Blocked, ShipEvent.Hit -> continue
                    else -> {
                        val next = r.state.copy(facing = RIGHT)
                        if (seen.add(next)) {
                            if (seen.size > limit) return true
                            queue.addLast(next)
                        }
                    }
                }
            }
        }
        return false
    }

    /** A learning room's game was won: count it and hand over its reward key. */
    fun solveRoom(level: ShipLevel, s: ShipState, room: Int): ShipState {
        if (room != s.solved) return s
        val keys = s.keys.toMutableList()
        val reward = level.rewards.getOrElse(room) { '.' }
        if (reward in KEY_COLORS) keys[KEY_COLORS.indexOf(reward)]++
        return s.copy(solved = s.solved + 1, keys = keys)
    }
}
