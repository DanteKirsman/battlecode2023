package danteplayer;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;
    static MapLocation headquarters;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6147);

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
        System.out.println("I'm a " + rc.getType() + " and I just got created! I have health " + rc.getHealth());

        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the RobotType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                switch (rc.getType()) {
                    case HEADQUARTERS:     runHeadquarters(rc);  break;
                    case CARRIER:      runCarrier(rc);   break;
                    case LAUNCHER: runLauncher(rc); break;
                    case BOOSTER: // Examplefuncsplayer doesn't use any of these robot types below.
                    case DESTABILIZER: // You might want to give them a try!
                    case AMPLIFIER:       break;
                }

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    /**
     * Run a single turn for a Headquarters.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runHeadquarters(RobotController rc) throws GameActionException {
        // Pick a direction to build in.
        MapLocation hqLocation = rc.getLocation();
        WellInfo[] wells = rc.senseNearbyWells(-1);
        for (WellInfo well : wells) {
            Direction closestToWell = hqLocation.directionTo(well.getMapLocation());
            MapLocation spawnLocation = rc.getLocation().add(closestToWell);
            if (rc.canBuildRobot(RobotType.CARRIER, spawnLocation)) {
                rc.buildRobot(RobotType.CARRIER, spawnLocation);
            }
        }

        for (Direction direction : directions) {
            MapLocation spawnLocation = rc.getLocation().add(direction);
            if(rc.canBuildRobot(RobotType.LAUNCHER, spawnLocation)){
                rc.buildRobot(RobotType.LAUNCHER, spawnLocation);
            }
        }
    }

    /**
     * Run a single turn for a Carrier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runCarrier(RobotController rc) throws GameActionException {
        // Get the location of the HQ
        if (turnCount == 1) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HEADQUARTERS) {
                    headquarters = robot.location;
                }
            }
        }
        if (rc.getAnchor() != null) {
            // If I have an anchor singularly focus on getting it to the first island I see
            int[] islands = rc.senseNearbyIslands();
            Set<MapLocation> islandLocs = new HashSet<>();
            for (int id : islands) {
                MapLocation[] thisIslandLocs = rc.senseNearbyIslandLocations(id);
                islandLocs.addAll(Arrays.asList(thisIslandLocs));
            }
            if (islandLocs.size() > 0) {
                MapLocation islandLocation = islandLocs.iterator().next();
                rc.setIndicatorString("Moving my anchor towards " + islandLocation);
                while (!rc.getLocation().equals(islandLocation)) {
                    Direction dir = rc.getLocation().directionTo(islandLocation);
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }
                }
                if (rc.canPlaceAnchor()) {
                    rc.setIndicatorString("Huzzah, placed anchor!");
                    rc.placeAnchor();
                }
            }
        }
        int amountOfAdamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
        int amountOfMana = rc.getResourceAmount(ResourceType.MANA);
        MapLocation me = rc.getLocation();
        boolean dontMove = false;
        if ((amountOfAdamantium + amountOfMana) < 40) {
            WellInfo[] wells = rc.senseNearbyWells();
            if (wells.length > 0) {
                MapLocation closestWellLocation = wells[0].getMapLocation();
                for (WellInfo well : wells) {
                    if (me.distanceSquaredTo(well.getMapLocation()) < me.distanceSquaredTo(closestWellLocation)) {
                        closestWellLocation = well.getMapLocation();
                    }
                }
                if (me.isAdjacentTo(closestWellLocation)) {
                    dontMove = true;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            MapLocation wellLocation = new MapLocation(me.x + dx, me.y + dy);
                            if (rc.canCollectResource(wellLocation, -1)) {
                                rc.collectResource(wellLocation, -1);
                                rc.setIndicatorString("Collecting, now have, AD:" +
                                        rc.getResourceAmount(ResourceType.ADAMANTIUM) +
                                        " MN: " + rc.getResourceAmount(ResourceType.MANA) +
                                        " EX: " + rc.getResourceAmount(ResourceType.ELIXIR));
                            }
                        }
                    }
                } else {
                    Direction dir = me.directionTo(closestWellLocation);
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                        rc.setIndicatorString("Closest well at " + closestWellLocation + " , im omw by moving " + dir);
                    }
                    if (amountOfAdamantium + amountOfMana == 0) {
                        Direction dir2 = me.directionTo(closestWellLocation);
                        if (rc.canMove(dir2)) {
                            rc.move(dir2);
                            rc.setIndicatorString("Closest well at " + closestWellLocation + " , im omw by moving " + dir2);
                        }
                    }
                }
            }
        } else {
            // Take the resources back to HQ
            Direction dirToHq = me.directionTo(headquarters);
            if (rc.getLocation().isAdjacentTo(headquarters)) {
                for (ResourceType resource : ResourceType.values()) {
                    if (rc.canTransferResource(headquarters, resource, rc.getResourceAmount(resource))) {
                        rc.transferResource(headquarters, resource, rc.getResourceAmount(resource));
                        rc.setIndicatorString("Transferred " + resource + " to " + headquarters);
                    }
                }
            } else if (rc.canMove(dirToHq)) {
                rc.move(dirToHq);
            }
        }
        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir) && !dontMove) {
            rc.move(dir);
        }
    }

    /**
     * Run a single turn for a Launcher.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runLauncher(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
                rc.setIndicatorString("Attacking");
            }
        }
        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
}
