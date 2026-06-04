# CA-C (California Class C / Car) Sub-topic Catalog (16 sub-topics)

> **Status**: APPROVED & FROZEN (2026-06-03). Seeded by `V27__seed_ca_c_exam.sql`.
> User locked parameters to M1 ("考试应该是一样的") and approved the 16-row catalog as-is;
> parking stays folded into `CAC_LANE_USE_PARKING` to preserve M1's 8×2 parity.
> **Exam**: `CA` × `C` — California Class C (non-commercial car) knowledge test.
> **Parity rule**: mirror the M1 catalog 1:1 — 8 parent topics × 2 sub-topics, same
> `sort_order` slots, same `is_key_topic`/`risk_level` flags. The M1 exam parameters
> are reused as-is (pass threshold **85%**, mock **30 questions**, target bank **~120**).
> **Source authority**: California Driver Handbook (DL 600), §5–§10. Section/subsection
> titles only are used as factual anchors. Per the project's copyright stance the
> handbook text is **NOT vendored** into the repo — the AI Q-gen pipeline fetches the
> relevant section excerpt **transiently** at generation time and discards it; only the
> original generated questions are persisted. Anchors are section names (not line
> numbers) so they survive periodic handbook refreshes.
> **Code prefix**: all CA-C topic/sub-topic codes use the `CAC_` prefix because
> `topics.code` / `sub_topics.code` are globally UNIQUE (cannot reuse the M1 codes).

---

## Catalog

| # | Code | Parent Topic (code / key / risk / sort) | Name EN | Name ZH | Handbook Anchor (DL 600) |
|---|---|---|---|---|---|
| 1 | `CAC_TRAFFIC_SIGNALS` | CAC_TRAFFIC_CONTROLS · key · high · 10 | Traffic Signals & Light Colors | 交通信号灯与灯色 | §7 Traffic Control → Traffic Signals; Pedestrian Signals and Signs |
| 2 | `CAC_TRAFFIC_SIGNS` | CAC_TRAFFIC_CONTROLS · key · high · 10 | Regulatory & Warning Signs | 管制与警告标志 | §7 Traffic Control → Signs (shapes & colors) |
| 3 | `CAC_ROW_INTERSECTIONS` | CAC_RIGHT_OF_WAY · key · high · 20 | Intersections, Roundabouts & Crossings | 路口、环岛与交叉口 | §7 Right-of-Way Rules → Intersections, Roundabouts, Mountain Roads |
| 4 | `CAC_ROW_VULNERABLE_USERS` | CAC_RIGHT_OF_WAY · key · high · 20 | Pedestrians, Bicyclists & Emergency Vehicles | 行人、自行车与应急车辆 | §7 Right-of-Way → Pedestrians; §7 (cont.) Sharing the Road → Bicycles, Emergency Vehicles, Move Over |
| 5 | `CAC_SPEED_LIMITS` | CAC_SPEED_DISTANCE · key · high · 30 | Speed Limits & Basic Speed Law | 限速与基本速度法 | §8 Safe Driving → Manage Your Speed; §7 (cont.) Business/Residential Districts, Fines & Double-Fine Zones |
| 6 | `CAC_FOLLOWING_DISTANCE` | CAC_SPEED_DISTANCE · key · high · 30 | Following Distance, Scanning & Blind Spots | 跟车距离、观察与盲区 | §8 Safe Driving → Be Aware of Your Surroundings (scan, tailgating, blind spots) |
| 7 | `CAC_LANE_MARKINGS` | CAC_LANE_POSITION · — · medium · 40 | Pavement Markings & Line Colors | 路面标线与线色 | §6 Navigating → Traffic Lanes → Lane Markings |
| 8 | `CAC_LANE_USE_PARKING` | CAC_LANE_POSITION · — · medium · 40 | Lane Selection, Special Lanes & Parking | 车道选择、专用道与停车 | §6 Navigating → Choosing/Changing Lanes, Types of Lanes (HOV/bike/center-turn/turnout); Parking rules (hills, colored curbs) |
| 9 | `CAC_TURNS_UTURNS` | CAC_TURNING_MANEUVERS · — · medium · 50 | Turns & U-Turns | 转弯与掉头 | §6 Navigating → Turns (right/left, against red light/arrow, U-turn) |
| 10 | `CAC_MERGE_PASS` | CAC_TURNING_MANEUVERS · — · medium · 50 | Merging, Exiting & Passing | 汇入、驶出与超车 | §6 Navigating → Merging and Exiting; Passing (how to pass / being passed) |
| 11 | `CAC_ALCOHOL_BAC` | CAC_ALCOHOL_DRUGS · key · high · 60 | BAC Limits & DUI Consequences | BAC 限值与酒驾后果 | §9 Alcohol and Drugs → BAC Limits, DUI Arrests, DUI Convictions, Use/Possession in a Vehicle |
| 12 | `CAC_DRUGS_IMPAIRMENT` | CAC_ALCOHOL_DRUGS · key · high · 60 | Drugs, Cannabis & Under-21 Zero Tolerance | 药物、大麻与 21 岁以下零容忍 | §9 Alcohol and Drugs → Drivers Under 21 (possessing/consuming); cannabis & drug impairment |
| 13 | `CAC_CONDITIONS_ADVERSE` | CAC_SPECIAL_CONDITIONS · — · medium · 70 | Night, Weather, Skids & Visibility | 夜间、天气、打滑与视线 | §8 Safe Driving → Understand the Road Conditions (darkness, glare, skids, hydroplaning, fog, wind, snow, flooded roads) |
| 14 | `CAC_HAZARDS_RAILROAD_LARGE` | CAC_SPECIAL_CONDITIONS · — · medium · 70 | Railroad Crossings, Work Zones & Large Vehicles | 铁道口、施工区与大型车辆 | §7 (cont.) Sharing the Road → Buses/Streetcars, Light Rail, Slow-moving Vehicles, Near Railroad Tracks, Road Workers and Work Zones |
| 15 | `CAC_OCCUPANT_PROTECTION` | CAC_DRIVER_SAFETY · key · high · 80 | Seat Belts, Child Restraints & Air Bags | 安全带、儿童约束与气囊 | §8 (cont.) Protect Yourself and Your Passengers (seat belts, child restraint systems, air bags, unattended children/pets) |
| 16 | `CAC_EMERGENCIES_DISTRACTION` | CAC_DRIVER_SAFETY · key · high · 80 | Vehicle Emergencies & Distracted Driving | 车辆应急与分心驾驶 | §8 (cont.) Know How to Handle Emergencies; Do Not Drive Distracted; §10 Collisions (what to do / reporting) |

---

## Parent Topics (8)

| Code | Name EN | Name ZH | is_key_topic | risk_level | sort_order |
|---|---|---|---|---|---|
| `CAC_TRAFFIC_CONTROLS` | Traffic Signs & Signals | 交通标志与信号 | TRUE | high | 10 |
| `CAC_RIGHT_OF_WAY` | Right of Way | 通行权 | TRUE | high | 20 |
| `CAC_SPEED_DISTANCE` | Speed & Following Distance | 速度与跟车距离 | TRUE | high | 30 |
| `CAC_LANE_POSITION` | Lane Use & Markings | 车道使用与标线 | FALSE | medium | 40 |
| `CAC_TURNING_MANEUVERS` | Turns, Merging & Passing | 转弯、汇入与超车 | FALSE | medium | 50 |
| `CAC_ALCOHOL_DRUGS` | Alcohol & Drugs | 酒精与药物 | TRUE | high | 60 |
| `CAC_SPECIAL_CONDITIONS` | Special Driving Conditions | 特殊驾驶条件 | FALSE | medium | 70 |
| `CAC_DRIVER_SAFETY` | Driver & Occupant Safety | 驾驶人与乘员安全 | TRUE | high | 80 |

(Mirrors M1's flag/sort layout exactly: 5 key/high topics + 3 medium topics. The only
slot substitution vs M1 is `MOTORCYCLE_BASICS` → `CAC_DRIVER_SAFETY`, since cars test
occupant protection + emergencies rather than gear/pre-ride inspection.)

---

## Per-Sub-Topic Description

### 1. `CAC_TRAFFIC_SIGNALS` — Traffic Signals & Light Colors
Solid/arrow/flashing red, yellow, and green lights; what to do when a signal is not
working (treat as all-way stop); red/green/yellow arrows; pedestrian signals (WALK /
DON'T WALK / flashing hand, countdown numbers, pedestrian push button).

### 2. `CAC_TRAFFIC_SIGNS` — Regulatory & Warning Signs
STOP and YIELD signs, WRONG WAY, red-and-white regulatory signs, prohibitory "red
circle with line", 5-sided school sign, diamond warning signs, white rectangular
regulatory signs, and the meaning carried by sign shape and color.

### 3. `CAC_ROW_INTERSECTIONS` — Intersections, Roundabouts & Crossings
Who proceeds first at four-way stops, uncontrolled and T-intersections, left turns
against oncoming traffic, roundabout entry/exit, blind intersections, and the
narrow-mountain-road downhill-yields rule.

### 4. `CAC_ROW_VULNERABLE_USERS` — Pedestrians, Bicyclists & Emergency Vehicles
Yielding to pedestrians at marked/unmarked crosswalks, pedestrians who are blind,
caution around children; bicyclists' equal rights, the 3-foot passing rule, bike
lanes; yielding to emergency vehicles and the "Move Over and Slow Down" law.

### 5. `CAC_SPEED_LIMITS` — Speed Limits & Basic Speed Law
The basic speed law (never faster than safe for conditions), maximum posted limits
(65 mph freeways / 55 mph two-lane undivided & towing), school-zone and blind/
business/residential limits, and double-fine / work-zone speed penalties.

### 6. `CAC_FOLLOWING_DISTANCE` — Following Distance, Scanning & Blind Spots
The 3-second following rule and extending it in poor conditions, tailgating risks,
scanning surroundings, knowing what is at your sides and behind you, and checking
blind spots before moving over.

### 7. `CAC_LANE_MARKINGS` — Pavement Markings & Line Colors
Single/double solid and broken yellow lines, single/double solid and broken white
lines, yield lines, end-of-lane markings, and what passing/crossing each line type
permits, including two-way left-turn center lanes.

### 8. `CAC_LANE_USE_PARKING` — Lane Selection, Special Lanes & Parking
Choosing and changing lanes safely; carpool/HOV, center left-turn, bicycle, passing,
and turnout lanes; plus parking rules — parking on hills (wheel direction), colored
curb meanings, and where parking is prohibited.

### 9. `CAC_TURNS_UTURNS` — Turns & U-Turns
Right and left turns from/into the correct lane, right turn against a red light/arrow,
left turn against a red light, hand signals, and where U-turns are legal vs prohibited
(business districts, near rail, no-U-turn signs).

### 10. `CAC_MERGE_PASS` — Merging, Exiting & Passing
Merging onto and with freeway traffic, using acceleration/deceleration lanes, exiting,
crossing or entering traffic, how to pass legally and safely, and being passed
(maintain speed, do not accelerate).

### 11. `CAC_ALCOHOL_BAC` — BAC Limits & DUI Consequences
California BAC limits (0.08% adults, 0.04% commercial, 0.01%/zero for under-21),
implied consent, the open-container / cannabis-in-vehicle rules, and DUI arrest and
conviction penalties (license suspension, fines, jail).

### 12. `CAC_DRUGS_IMPAIRMENT` — Drugs, Cannabis & Under-21 Zero Tolerance
Impairment from cannabis, prescription, and OTC drugs as well as alcohol; the
zero-tolerance law for drivers under 21 possessing or consuming alcohol; how even
small amounts of any impairing substance degrade driving.

### 13. `CAC_CONDITIONS_ADVERSE` — Night, Weather, Skids & Visibility
Driving in darkness and sun glare, slippery roads, hydroplaning, slippery-surface and
locked-wheel skids and how to recover, fog/heavy smoke, high winds, snow/mud, and
flooded roads.

### 14. `CAC_HAZARDS_RAILROAD_LARGE` — Railroad Crossings, Work Zones & Large Vehicles
Railroad and light-rail crossings, road workers and work zones (and double-fine
zones), sharing the road with large trucks and buses (the "No Zone" blind spots),
school-bus flashing-red-light stopping rules, and slow-moving vehicles.

### 15. `CAC_OCCUPANT_PROTECTION` — Seat Belts, Child Restraints & Air Bags
Seat belt law for driver and passengers, child restraint system / safety seat
requirements (age, height, weight, rear-facing), how air bags work with belts and
seating position, and never leaving children or pets unattended in a vehicle.

### 16. `CAC_EMERGENCIES_DISTRACTION` — Vehicle Emergencies & Distracted Driving
Handling a tire blowout, brake failure, a stuck accelerator, overheating, and a
vehicle disabled on the freeway; what to do if you are in a collision (stop, exchange
info, report); and distracted driving — cell phones, texting, and the stricter rule
for minors.

---

## Coverage Audit

| Parent Topic | Sub-topics | Handbook section coverage |
|---|---|---|
| CAC_TRAFFIC_CONTROLS | 2 | §7 Traffic Control (signals, pedestrian signals, signs) — full |
| CAC_RIGHT_OF_WAY | 2 | §7 Right-of-Way + §7 cont. Sharing the Road (vulnerable users) — full |
| CAC_SPEED_DISTANCE | 2 | §8 Manage Your Speed + Be Aware of Surroundings — full |
| CAC_LANE_POSITION | 2 | §6 Traffic Lanes + parking rules — full |
| CAC_TURNING_MANEUVERS | 2 | §6 Turns, Merging/Exiting, Passing — full |
| CAC_ALCOHOL_DRUGS | 2 | §9 Alcohol and Drugs — full (dedicated section) |
| CAC_SPECIAL_CONDITIONS | 2 | §8 Road Conditions + §7 cont. large vehicles/rail/work zones — full |
| CAC_DRIVER_SAFETY | 2 | §8 cont. Protect Passengers + Emergencies + Distraction + §10 Collisions — full |

All 16 sub-topics are backed by the California Driver Handbook (DL 600). Unlike M1
(which leaned partly on base-handbook references for traffic-control content), CA-C
content is natively and fully covered by the regular driver handbook.

---

## Resolved Decisions (2026-06-03)

1. **16 codes / names / EN / ZH** — approved as-is.
2. **Parameters** — locked to M1: pass threshold **85%**, mock **30 questions**, target
   bank **~120** (all AI-generated, no human seed).
3. **Parking** — kept folded into `CAC_LANE_USE_PARKING` to preserve M1's 8×2 parity.

Landed by **`V27__seed_ca_c_exam.sql`**: `exams` CA-C row + 8 `CAC_*` parent topics +
16 `CAC_*` sub-topics, all scoped to the CA-C exam_id. Still to come (later steps):
question generation (transient handbook fetch → DeepSeek → 4 gates, after cost
approval) + a CA-C mock-exam template (30 questions).
