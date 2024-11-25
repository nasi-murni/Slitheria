import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.io.File;

public class MapGenerator {
    private static final char WALL = '#';
    private static final char EMPTY = '+';
    private static final char SPIKE = '*';
    private static final char PORTAL = '|';
    private static final char GOAL = ':';
    private static final char PLAYER = 'x';

    static class Chamber {
        int x, y, width, height;
        
        Chamber(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean intersects(Chamber other) {
            return x < other.x + other.width + 2 && 
                   x + width + 2 > other.x && 
                   y < other.y + other.height + 2 && 
                   y + height + 2 > other.y;
        }

        int getCenterX() {
            return x + width/2;
        }

        int getCenterY() {
            return y + height/2;
        }
    }

    static class Point {
        int x, y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static String getMapDirectory() {
        File srcMaps = new File("maps");
        if (srcMaps.exists()) {
            return "../maps";
        }
        
        File maps = new File("maps");
        if (!maps.exists()) {
            maps.mkdir();
        }
        return "maps/";
    }

    private static int getNextMapNumber() {
        String mapDir = getMapDirectory();
        File directory = new File(mapDir);
        
        File[] files = directory.listFiles((dir, name) -> 
            name.matches("map\\d+\\.txt"));
        
        if (files == null || files.length == 0) {
            return 1;
        }

        int maxNumber = 0;
        for (File file : files) {
            String fileName = file.getName();
            try {
                int number = Integer.parseInt(
                    fileName.substring(3, fileName.length() - 4));
                maxNumber = Math.max(maxNumber, number);
            } catch (NumberFormatException e) {
                continue;
            }
        }

        return maxNumber + 1;
    }

    public void saveNewMap(String mapContent) {
        int nextNumber = getNextMapNumber();
        String mapName = "map" + nextNumber;
        String mapDir = getMapDirectory();
        String filePath = mapName + ".txt";
        
        try {
            File file = new File(mapDir, filePath);
            FileWriter writer = new FileWriter(file);
            writer.write(mapContent);
            writer.close();
            
            System.out.println("Map saved as: " + file.getName());
        } catch (IOException e) {
            System.err.println("Error saving map: " + e.getMessage());
        }
    }

    private static List<Chamber> createConnectedChambers(char[][] map, int width, int height) {
        List<Chamber> chambers = new ArrayList<>();
        
        // Guaranteed minimum sizes to prevent too-small chambers
        int minChamberWidth = 6;
        int minChamberHeight = 4;
        
        // Calculate number of chambers that can fit
        int gridColumns = Math.min(3, (width - 8) / (minChamberWidth + 4));
        int gridRows = Math.min(2, (height - 8) / (minChamberHeight + 4));
        
        if (gridColumns < 1) gridColumns = 1;
        if (gridRows < 1) gridRows = 1;
        
        // Calculate chamber sizes
        int chamberWidth = Math.max(minChamberWidth, (width - (gridColumns + 1) * 4) / gridColumns);
        int chamberHeight = Math.max(minChamberHeight, (height - (gridRows + 1) * 4) / gridRows);
        
        // Calculate starting positions to center the grid
        int totalChamberWidth = gridColumns * chamberWidth + (gridColumns - 1) * 4;
        int totalChamberHeight = gridRows * chamberHeight + (gridRows - 1) * 4;
        int startX = (width - totalChamberWidth) / 2;
        int startY = (height - totalChamberHeight) / 2;
        
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridColumns; col++) {
                int x = startX + col * (chamberWidth + 4);
                int y = startY + row * (chamberHeight + 4);
                
                // Ensure chamber doesn't exceed map bounds
                if (x + chamberWidth >= width - 1 || y + chamberHeight >= height - 1) {
                    continue;
                }
                
                Chamber chamber = new Chamber(x, y, chamberWidth, chamberHeight);
                chambers.add(chamber);
                
                // Create chamber walls
                for (int i = y; i < y + chamberHeight; i++) {
                    for (int j = x; j < x + chamberWidth; j++) {
                        if (i == y || i == y + chamberHeight - 1 ||
                            j == x || j == x + chamberWidth - 1) {
                            map[i][j] = WALL;
                        }
                    }
                }
                
                // Create doors between chambers
                if (col > 0) {
                    createDoor(map, x, y + chamberHeight/2);
                }
                if (row > 0) {
                    createDoor(map, x + chamberWidth/2, y);
                }
            }
        }
        
        // Don't return empty chamber list
        if (chambers.isEmpty()) {
            Chamber singleChamber = new Chamber(2, 2, Math.min(width-4, minChamberWidth), 
                                              Math.min(height-4, minChamberHeight));
            chambers.add(singleChamber);
            
            // Create walls for single chamber
            for (int i = singleChamber.y; i < singleChamber.y + singleChamber.height; i++) {
                for (int j = singleChamber.x; j < singleChamber.x + singleChamber.width; j++) {
                    if (i == singleChamber.y || i == singleChamber.y + singleChamber.height - 1 ||
                        j == singleChamber.x || j == singleChamber.x + singleChamber.width - 1) {
                        map[i][j] = WALL;
                    }
                }
            }
        }
        
        return chambers;
    }

    // Update generateLargeMap to include size validation and retry limits
    public static String generateLargeMap(int width, int height) {
        StringBuilder mapContent = new StringBuilder();
        try {
            // Ensure minimum size
            width = Math.max(10, width);
            height = Math.max(8, height);
            
            mapContent.append("Standard " + height + "x" + width + " map with obstacles.\n");
            mapContent.append(height + "\n");
            mapContent.append(width + "\n");

            char[][] map;
            List<Chamber> chambers;
            Point goalLocation;
            Point playerStart;
            
            int attempts = 0;
            int maxAttempts = 5;
            
            do {
                attempts++;
                map = new char[height][width];
                for (int i = 0; i < height; i++) {
                    Arrays.fill(map[i], EMPTY);
                }
                
                addBorders(map);
                chambers = createConnectedChambers(map, width, height);
                
                if (chambers.isEmpty()) {
                    continue;
                }
                
                playerStart = new Point(chambers.get(0).x + 2, chambers.get(0).y + 2);
                goalLocation = createGoalPosition(map, chambers);
                
                // Only add portals if we have multiple chambers
                if (chambers.size() > 1) {
                    addPortalNetwork(map, chambers);
                }
                
                addHazardPatterns(map, chambers, goalLocation);
                
                map[playerStart.y][playerStart.x] = PLAYER;
                map[goalLocation.y][goalLocation.x] = GOAL;
                
                if (validateMap(map, playerStart, goalLocation)) {
                    break;
                }
                
                if (attempts >= maxAttempts) {
                    // If we can't generate a valid map, create a simple one
                    System.out.print("Failed.");
                    createSimpleMap(map, width, height);
                    break;
                }
                
            } while (true);

            for (char[] row : map) {
                mapContent.append(row).append('\n');
            }
            
            return mapContent.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void createSimpleMap(char[][] map, int width, int height) {
        // Clear the map
        for (int i = 0; i < height; i++) {
            Arrays.fill(map[i], EMPTY);
        }
        
        // Add borders
        addBorders(map);
        
        // Place player at start
        map[2][2] = PLAYER;
        
        // Place goal near the opposite corner
        map[height-3][width-3] = GOAL;
        
        // Add some basic obstacles
        for (int i = 4; i < height-4; i += 2) {
            for (int j = 4; j < width-4; j += 3) {
                if (map[i][j] == EMPTY) {
                    map[i][j] = WALL;
                }
            }
        }
    }

    private static void addBorders(char[][] map) {
        Arrays.fill(map[0], WALL);
        Arrays.fill(map[map.length - 1], WALL);
        for (int i = 0; i < map.length; i++) {
            map[i][0] = WALL;
            map[i][map[0].length - 1] = WALL;
        }
    }

    private static void createDoor(char[][] map, int x, int y) {
        map[y][x] = EMPTY;
        map[y][x-1] = EMPTY;
    }

    private static Point createGoalPosition(char[][] map, List<Chamber> chambers) {
        Chamber goalChamber = chambers.get(chambers.size() - 1);
        int goalX = goalChamber.x + goalChamber.width - 3;
        int goalY = goalChamber.y + goalChamber.height - 3;
        
        createGoalMaze(map, goalX, goalY, goalChamber);
        return new Point(goalX, goalY);
    }

    private static void createGoalMaze(char[][] map, int goalX, int goalY, Chamber chamber) {
        int[][] pattern = {
            {1,1,1,1,1},
            {1,0,1,0,1},
            {1,0,0,0,1},
            {1,0,1,0,1},
            {1,1,1,1,1}
        };
        
        int startX = goalX - 2;
        int startY = goalY - 2;
        
        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < pattern[0].length; x++) {
                int mapX = startX + x;
                int mapY = startY + y;
                
                if (mapX >= chamber.x && mapX < chamber.x + chamber.width &&
                    mapY >= chamber.y && mapY < chamber.y + chamber.height) {
                    if (pattern[y][x] == 1) {
                        map[mapY][mapX] = WALL;
                    } else if ((x + y) % 2 == 0 && (x != 2 || y != 2)) {
                        map[mapY][mapX] = SPIKE;
                    }
                }
            }
        }
        
        // Create one guaranteed path
        int pathX = startX + 2;
        int pathY = startY + pattern.length - 1;
        for (int y = pathY; y > startY + 2; y--) {
            map[y][pathX] = EMPTY;
        }
    }

    private static void addPortalNetwork(char[][] map, List<Chamber> chambers) {
        for (int i = 0; i < chambers.size() - 1; i++) {
            Chamber source = chambers.get(i);
            Chamber target = chambers.get(i + 1);
            
            Point p1 = findSafePortalLocation(map, source);
            Point p2 = findSafePortalLocation(map, target);
            
            if (p1 != null && p2 != null) {
                map[p1.y][p1.x] = Character.forDigit(i, 10);
                map[p2.y][p2.x] = Character.forDigit(i, 10);
            }
        }
    }

    private static Point findSafePortalLocation(char[][] map, Chamber chamber) {
        for (int y = chamber.y + 2; y < chamber.y + chamber.height - 2; y++) {
            for (int x = chamber.x + 2; x < chamber.x + chamber.width - 2; x++) {
                if (map[y][x] == EMPTY && isSafePortalSpot(map, x, y)) {
                    return new Point(x, y);
                }
            }
        }
        return null;
    }

    private static boolean isSafePortalSpot(char[][] map, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (map[y + dy][x + dx] != EMPTY && map[y + dy][x + dx] != WALL) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void addHazardPatterns(char[][] map, List<Chamber> chambers, Point goalLocation) {
        for (Chamber chamber : chambers) {
            boolean isOnPathToCrypticGoal = isOnCrypticPath(chamber, goalLocation);
            
            if (isOnPathToCrypticGoal) {
                addGuidingHazards(map, chamber);
            } else {
                addMisleadingHazards(map, chamber);
            }
        }
    }

    private static boolean isOnCrypticPath(Chamber chamber, Point goalLocation) {
        int distance = Math.abs(chamber.getCenterX() - goalLocation.x) + 
                      Math.abs(chamber.getCenterY() - goalLocation.y);
        return distance < 20;
    }

    private static void addGuidingHazards(char[][] map, Chamber chamber) {
        for (int y = chamber.y + 1; y < chamber.y + chamber.height - 1; y++) {
            for (int x = chamber.x + 1; x < chamber.x + chamber.width - 1; x++) {
                if (map[y][x] == EMPTY && ((x - chamber.x) + (y - chamber.y)) % 3 == 0) {
                    map[y][x] = SPIKE;
                }
            }
        }
    }

    private static void addMisleadingHazards(char[][] map, Chamber chamber) {
        for (int y = chamber.y + 1; y < chamber.y + chamber.height - 1; y++) {
            for (int x = chamber.x + 1; x < chamber.x + chamber.width - 1; x++) {
                if (map[y][x] == EMPTY && (x * y) % 4 == 0) {
                    map[y][x] = SPIKE;
                }
            }
        }
    }

    private static boolean validateMap(char[][] map, Point start, Point goal) {
        char[][] mapCopy = new char[map.length][map[0].length];
        for (int i = 0; i < map.length; i++) {
            mapCopy[i] = map[i].clone();
        }
        
        Map<Character, List<Point>> portals = new HashMap<>();
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[0].length; x++) {
                char c = map[y][x];
                if (Character.isDigit(c)) {
                    portals.computeIfAbsent(c, k -> new ArrayList<>())
                           .add(new Point(x, y));
                }
            }
        }
        
        for (List<Point> portalPair : portals.values()) {
            if (portalPair.size() != 2) {
                return false;
            }
        }
        
        return isReachable(mapCopy, start, goal, portals);
    }

    private static boolean isReachable(char[][] map, Point start, Point goal, Map<Character, List<Point>> portals) {
        Queue<Point> queue = new LinkedList<>();
        queue.add(start);
        map[start.y][start.x] = 'V';
        
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            
            if (p.x == goal.x && p.y == goal.y) {
                return true;
            }
            
            int[][] dirs = {{0,1}, {1,0}, {0,-1}, {-1,0}};
            for (int[] dir : dirs) {
                int nx = p.x + dir[0];
                int ny = p.y + dir[1];
                
                if (isValidMove(map, nx, ny)) {
                    queue.add(new Point(nx, ny));
                    map[ny][nx] = 'V';
                }
            }
            
            char tile = map[p.y][p.x];
            if (Character.isDigit(tile)) {
                List<Point> pair = portals.get(tile);
                for (Point portalExit : pair) {
                    if (portalExit.x != p.x || portalExit.y != p.y) {
                        if (map[portalExit.y][portalExit.x] != 'V') {
                            queue.add(portalExit);
                            map[portalExit.y][portalExit.x] = 'V';
                        }
                    }
                }
            }
        }
        
        return false;
    }

    private static boolean isValidMove(char[][] map, int x, int y) {
        return x >= 0 && x < map[0].length && 
               y >= 0 && y < map.length && 
               map[y][x] != WALL && 
               map[y][x] != 'V';
    }

    public static void main(String args[]) {
        Random rand = new Random();
        MapGenerator generator = new MapGenerator();
        String mapContent = generateLargeMap(30 + rand.nextInt(70), 10 + rand.nextInt(40));
        if (mapContent != null) {
            generator.saveNewMap(mapContent);
        }
    }
}