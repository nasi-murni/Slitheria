import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.io.File;

public class MapGenerator {
    private static final char WALL = '#';
    private static final char EMPTY = '+';
    private static final char SPIKE = '*';
    private static final char GOAL = ':';
    private static final char PLAYER = 'x';
    
    static class Cell {
        Set<Character> possibilities;
        char value;
        boolean collapsed;
        
        Cell() {
            // Initialize with all possible values
            possibilities = new HashSet<>(Arrays.asList(WALL, EMPTY, SPIKE));
            collapsed = false;
        }
        
        int entropy() {
            return collapsed ? 0 : possibilities.size();
        }
    }

    static class Position {
        int x, y;
        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // Define valid neighbor combinations
    private static final Map<Character, Map<String, Set<Character>>> VALID_NEIGHBORS = new HashMap<>();
    
    static {
        // Initialize valid neighbors for each tile type
        Map<String, Set<Character>> wallNeighbors = new HashMap<>();
        wallNeighbors.put("up", new HashSet<>(Arrays.asList(WALL, EMPTY)));
        wallNeighbors.put("down", new HashSet<>(Arrays.asList(WALL, EMPTY)));
        wallNeighbors.put("left", new HashSet<>(Arrays.asList(WALL, EMPTY)));
        wallNeighbors.put("right", new HashSet<>(Arrays.asList(WALL, EMPTY)));
        VALID_NEIGHBORS.put(WALL, wallNeighbors);

        Map<String, Set<Character>> emptyNeighbors = new HashMap<>();
        emptyNeighbors.put("up", new HashSet<>(Arrays.asList(WALL, EMPTY, SPIKE)));
        emptyNeighbors.put("down", new HashSet<>(Arrays.asList(WALL, EMPTY, SPIKE)));
        emptyNeighbors.put("left", new HashSet<>(Arrays.asList(WALL, EMPTY, SPIKE)));
        emptyNeighbors.put("right", new HashSet<>(Arrays.asList(WALL, EMPTY, SPIKE)));
        VALID_NEIGHBORS.put(EMPTY, emptyNeighbors);

        Map<String, Set<Character>> spikeNeighbors = new HashMap<>();
        spikeNeighbors.put("up", new HashSet<>(Arrays.asList(EMPTY, SPIKE)));
        spikeNeighbors.put("down", new HashSet<>(Arrays.asList(EMPTY, SPIKE)));
        spikeNeighbors.put("left", new HashSet<>(Arrays.asList(EMPTY, SPIKE)));
        spikeNeighbors.put("right", new HashSet<>(Arrays.asList(EMPTY, SPIKE)));
        VALID_NEIGHBORS.put(SPIKE, spikeNeighbors);
    }

    private static void collapse(Cell[][] grid, int x, int y, char value) {
        Cell cell = grid[y][x];
        cell.possibilities.clear();
        cell.possibilities.add(value);
        cell.value = value;
        cell.collapsed = true;
    }

    private static void propagateConstraints(Cell[][] grid, Position pos) {
        Queue<Position> queue = new LinkedList<>();
        queue.add(pos);
        
        while (!queue.isEmpty()) {
            Position current = queue.poll();
            Cell currentCell = grid[current.y][current.x];
            
            // Check all neighbors
            int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
            String[] dirNames = {"down", "right", "up", "left"};
            
            for (int i = 0; i < directions.length; i++) {
                int nx = current.x + directions[i][0];
                int ny = current.y + directions[i][1];
                
                if (nx >= 0 && nx < grid[0].length && ny >= 0 && ny < grid.length) {
                    Cell neighbor = grid[ny][nx];
                    int originalSize = neighbor.possibilities.size();
                    
                    // Update neighbor's possibilities based on current cell's value
                    if (currentCell.collapsed) {
                        Set<Character> validNeighbors = VALID_NEIGHBORS.get(currentCell.value)
                                                                     .get(dirNames[i]);
                        neighbor.possibilities.retainAll(validNeighbors);
                        
                        // If possibilities changed, add to queue
                        if (!neighbor.collapsed && neighbor.possibilities.size() != originalSize) {
                            queue.add(new Position(nx, ny));
                            
                            // If only one possibility remains, collapse it
                            if (neighbor.possibilities.size() == 1) {
                                collapse(grid, nx, ny, neighbor.possibilities.iterator().next());
                            }
                        }
                    }
                }
            }
        }
    }

    private static Position findMinEntropyPosition(Cell[][] grid, Random rand) {
        List<Position> minPositions = new ArrayList<>();
        int minEntropy = Integer.MAX_VALUE;
        
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[0].length; x++) {
                Cell cell = grid[y][x];
                if (!cell.collapsed) {
                    int entropy = cell.entropy();
                    if (entropy > 0) {
                        if (entropy < minEntropy) {
                            minEntropy = entropy;
                            minPositions.clear();
                            minPositions.add(new Position(x, y));
                        } else if (entropy == minEntropy) {
                            minPositions.add(new Position(x, y));
                        }
                    }
                }
            }
        }
        
        return minPositions.isEmpty() ? null : minPositions.get(rand.nextInt(minPositions.size()));
    }

    private static boolean isFullyCollapsed(Cell[][] grid) {
        for (Cell[] row : grid) {
            for (Cell cell : row) {
                if (!cell.collapsed) return false;
            }
        }
        return true;
    }

    private static void resetGrid(Cell[][] grid) {
        for (int y = 1; y < grid.length - 1; y++) {
            for (int x = 1; x < grid[0].length - 1; x++) {
                grid[y][x] = new Cell();
            }
        }
    }

    private static void placePlayerAndGoal(char[][] map) {
        List<Position> openSpaces = findOpenSpaces(map);
        if (openSpaces.isEmpty()) return;

        // Find a position with good clearance for player
        Position playerPos = null;
        int bestClearance = 0;
        
        for (Position pos : openSpaces) {
            int clearance = calculateClearance(map, pos);
            if (clearance > bestClearance) {
                bestClearance = clearance;
                playerPos = pos;
            }
        }

        if (playerPos == null) return;
        map[playerPos.y][playerPos.x] = PLAYER;
        openSpaces.remove(playerPos);

        // Find furthest position with good path for goal
        Position goalPos = null;
        int maxPathDistance = 0;

        for (Position pos : openSpaces) {
            if (calculateClearance(map, pos) >= 2) {  // Ensure some clearance for goal
                int distance = calculatePathDistance(map, playerPos, pos);
                if (distance > maxPathDistance) {
                    maxPathDistance = distance;
                    goalPos = pos;
                }
            }
        }

        if (goalPos != null) {
            map[goalPos.y][goalPos.x] = GOAL;
            clearObstructivePaths(map, playerPos, goalPos);
        }
    }

    private static int calculateClearance(char[][] map, Position pos) {
        int clearance = 0;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int ny = pos.y + dy;
                int nx = pos.x + dx;
                if (ny >= 0 && ny < map.length && nx >= 0 && nx < map[0].length) {
                    if (map[ny][nx] == EMPTY) clearance++;
                }
            }
        }
        return clearance;
    }

    private static void clearObstructivePaths(char[][] map, Position start, Position end) {
        // Create a distance map using BFS
        int[][] distances = new int[map.length][map[0].length];
        for (int[] row : distances) Arrays.fill(row, Integer.MAX_VALUE);
        
        Queue<Position> queue = new LinkedList<>();
        queue.add(start);
        distances[start.y][start.x] = 0;
        
        int[][] dirs = {{0,1}, {1,0}, {0,-1}, {-1,0}};
        boolean[][] visited = new boolean[map.length][map[0].length];
        
        // Find all reachable positions
        while (!queue.isEmpty()) {
            Position current = queue.poll();
            if (visited[current.y][current.x]) continue;
            visited[current.y][current.x] = true;
            
            for (int[] dir : dirs) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];
                
                if (nx >= 0 && nx < map[0].length && ny >= 0 && ny < map.length) {
                    if (map[ny][nx] != WALL && !visited[ny][nx]) {
                        distances[ny][nx] = distances[current.y][current.x] + 1;
                        queue.add(new Position(nx, ny));
                    }
                }
            }
        }
        
        // If end is not reachable or path is too convoluted, clear some walls
        if (distances[end.y][end.x] == Integer.MAX_VALUE || 
            distances[end.y][end.x] > Math.abs(end.x - start.x) + Math.abs(end.y - start.y) * 2) {
            
            // Clear path using A* pathfinding
            clearDirectPath(map, start, end);
        }
        
        // Clear random obstructive walls near the path
        clearNearbyObstructions(map, start, end);
    }

    private static void clearDirectPath(char[][] map, Position start, Position end) {
        int dx = Integer.compare(end.x - start.x, 0);
        int dy = Integer.compare(end.y - start.y, 0);
        
        int x = start.x;
        int y = start.y;
        
        while (x != end.x || y != end.y) {
            // Clear walls in a 2-tile radius
            for (int cy = -1; cy <= 1; cy++) {
                for (int cx = -1; cx <= 1; cx++) {
                    int nx = x + cx;
                    int ny = y + cy;
                    if (nx >= 0 && nx < map[0].length && ny >= 0 && ny < map.length) {
                        if (map[ny][nx] == WALL) {
                            map[ny][nx] = EMPTY;
                        }
                    }
                }
            }
            
            // Move towards goal
            if (Math.abs(end.x - x) > Math.abs(end.y - y)) {
                x += dx;
            } else {
                y += dy;
            }
        }
    }

    private static void clearNearbyObstructions(char[][] map, Position start, Position end) {
        Random rand = new Random();
        int clearRadius = 3;
        
        // Get points along the approximate path
        List<Position> pathPoints = new ArrayList<>();
        int steps = Math.max(Math.abs(end.x - start.x), Math.abs(end.y - start.y));
        
        for (int i = 0; i <= steps; i++) {
            int x = start.x + (end.x - start.x) * i / steps;
            int y = start.y + (end.y - start.y) * i / steps;
            pathPoints.add(new Position(x, y));
        }
        
        // Clear obstructions near path points
        for (Position point : pathPoints) {
            for (int dy = -clearRadius; dy <= clearRadius; dy++) {
                for (int dx = -clearRadius; dx <= clearRadius; dx++) {
                    int nx = point.x + dx;
                    int ny = point.y + dy;
                    
                    if (nx >= 1 && nx < map[0].length - 1 && 
                        ny >= 1 && ny < map.length - 1) {
                        if (map[ny][nx] == WALL && rand.nextDouble() < 0.4) {
                            map[ny][nx] = EMPTY;
                        }
                    }
                }
            }
        }
    }

    private static List<Position> findOpenSpaces(char[][] map) {
        List<Position> spaces = new ArrayList<>();
        for (int y = 1; y < map.length - 1; y++) {
            for (int x = 1; x < map[0].length - 1; x++) {
                if (map[y][x] == EMPTY && calculateClearance(map, new Position(x, y)) >= 5) {
                    spaces.add(new Position(x, y));
                }
            }
        }
        return spaces;
    }

    private static int calculatePathDistance(char[][] map, Position start, Position end) {
        Queue<Position> queue = new LinkedList<>();
        boolean[][] visited = new boolean[map.length][map[0].length];
        int[][] distance = new int[map.length][map[0].length];
        
        queue.add(start);
        visited[start.y][start.x] = true;
        
        int[][] dirs = {{0,1}, {1,0}, {0,-1}, {-1,0}};
        
        while (!queue.isEmpty()) {
            Position current = queue.poll();
            
            if (current.x == end.x && current.y == end.y) {
                return distance[current.y][current.x];
            }
            
            for (int[] dir : dirs) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];
                
                if (nx >= 0 && nx < map[0].length && ny >= 0 && ny < map.length &&
                    !visited[ny][nx] && (map[ny][nx] == EMPTY || map[ny][nx] == GOAL)) {
                    visited[ny][nx] = true;
                    distance[ny][nx] = distance[current.y][current.x] + 1;
                    queue.add(new Position(nx, ny));
                }
            }
        }
        
        return Integer.MAX_VALUE;
    }

    private static void addPortals(char[][] map) {
        Random rand = new Random();
        List<Position> validPositions = new ArrayList<>();
        
        // Find valid portal locations
        for (int y = 1; y < map.length - 1; y++) {
            for (int x = 1; x < map[0].length - 1; x++) {
                if (map[y][x] == EMPTY && hasEmptyNeighbors(map, x, y)) {
                    validPositions.add(new Position(x, y));
                }
            }
        }

        // Place portal pairs
        int numPairs = Math.min(4, validPositions.size() / 2);
        for (int i = 0; i < numPairs && validPositions.size() >= 2; i++) {
            // Place first portal
            int idx1 = rand.nextInt(validPositions.size());
            Position pos1 = validPositions.remove(idx1);
            
            // Find distant position for second portal
            int maxDist = 0;
            int bestIdx = -1;
            
            for (int j = 0; j < validPositions.size(); j++) {
                Position pos2 = validPositions.get(j);
                int dist = Math.abs(pos1.x - pos2.x) + Math.abs(pos1.y - pos2.y);
                if (dist > maxDist) {
                    maxDist = dist;
                    bestIdx = j;
                }
            }
            
            if (bestIdx != -1) {
                Position pos2 = validPositions.remove(bestIdx);
                map[pos1.y][pos1.x] = Character.forDigit(i, 10);
                map[pos2.y][pos2.x] = Character.forDigit(i, 10);
            }
        }
    }

    private static boolean hasEmptyNeighbors(char[][] map, int x, int y) {
        int emptyCount = 0;
        int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};
        
        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (map[ny][nx] == EMPTY) emptyCount++;
        }
        
        return emptyCount >= 2;
    }

    public static String generateMap(int width, int height) {
        StringBuilder mapContent = new StringBuilder();
        mapContent.append("Standard " + height + "x" + width + " map with obstacles.\n");
        mapContent.append(height + "\n");
        mapContent.append(width + "\n");

        Cell[][] grid = new Cell[height][width];
        Random rand = new Random();
        
        // Initialize grid
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = new Cell();
            }
        }

        // Force borders to be walls
        for (int x = 0; x < width; x++) {
            collapse(grid, x, 0, WALL);
            collapse(grid, x, height-1, WALL);
        }
        for (int y = 0; y < height; y++) {
            collapse(grid, 0, y, WALL);
            collapse(grid, width-1, y, WALL);
        }

        // Run wave function collapse
        while (!isFullyCollapsed(grid)) {
            Position minEntropyPos = findMinEntropyPosition(grid, rand);
            if (minEntropyPos == null) break;
            
            Cell cell = grid[minEntropyPos.y][minEntropyPos.x];
            if (cell.possibilities.isEmpty()) {
                // Backtrack or restart if necessary
                resetGrid(grid);
                continue;
            }
            
            // Randomly choose from possible values
            List<Character> possible = new ArrayList<>(cell.possibilities);
            char chosenValue = possible.get(rand.nextInt(possible.size()));
            collapse(grid, minEntropyPos.x, minEntropyPos.y, chosenValue);
            
            // Propagate constraints
            propagateConstraints(grid, minEntropyPos);
        }

        // Convert to final map
        char[][] map = new char[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                map[y][x] = grid[y][x].value;
            }
        }

        // Place player and goal
        placePlayerAndGoal(map);
        
        // Add portals
        addPortals(map);

        // Convert to string
        for (char[] row : map) {
            mapContent.append(row).append('\n');
        }
        
        // Clear spike groups
        clearSpikes(mapContent);
        return mapContent.toString();
    }

    private static void clearSpikes(StringBuilder mapContent) {
        // Convert StringBuilder to lines
        String[] lines = mapContent.toString().split("\n");
        
        // First line is description, next two lines are height and width
        String description = lines[0];
        int height = Integer.parseInt(lines[1]);
        int width = Integer.parseInt(lines[2]);
        
        // Create map from subsequent lines
        char[][] map = new char[height][width];
        for (int y = 0; y < height; y++) {
            map[y] = lines[y + 3].toCharArray();
        }
        
        // Spike reduction parameters
        int MAX_CLUSTER_SIZE = 0;  // Maximum allowed spikes in a cluster
        double SPIKE_REDUCTION_PROBABILITY = 1.0; // Probability of removing excess spikes
        
        // Identify and process spike clusters
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (map[y][x] == '*') {
                    // Check surrounding area for spike clusters
                    int[][] directions = {{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};
                    List<Position> spikeCluster = new ArrayList<>();
                    spikeCluster.add(new Position(x, y));
                    
                    // Find adjacent spikes
                    for (int[] dir : directions) {
                        int nx = x + dir[0];
                        int ny = y + dir[1];
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height && map[ny][nx] == '*') {
                            spikeCluster.add(new Position(nx, ny));
                        }
                    }
                    
                    // Reduce spike clusters that are too large
                    if (spikeCluster.size() > MAX_CLUSTER_SIZE) {
                        Random rand = new Random();
                        Collections.shuffle(spikeCluster);
                        
                        // Remove spikes beyond MAX_CLUSTER_SIZE probabilistically
                        for (int i = MAX_CLUSTER_SIZE; i < spikeCluster.size(); i++) {
                            if (rand.nextDouble() < SPIKE_REDUCTION_PROBABILITY) {
                                Position spike = spikeCluster.get(i);
                                map[spike.y][spike.x] = '+';  // Replace with empty space
                            }
                        }
                    }
                }
            }
        }
        
        // Rebuild mapContent with modified map
        mapContent.setLength(0);  // Clear existing content
        mapContent.append(description).append("\n");
        mapContent.append(height).append("\n");
        mapContent.append(width).append("\n");
        
        for (char[] row : map) {
            mapContent.append(row).append('\n');
        }
    }

    private static String getMapDirectory() {
        // Get the current working directory
        String currentDir = System.getProperty("user.dir");
        
        // Go up one level if we're in src directory
        if (currentDir.endsWith("src")) {
            currentDir = new File(currentDir).getParent();
        }
        
        // Create maps directory in project root
        File mapsDir = new File(currentDir + File.separator + "maps");
        if (!mapsDir.exists()) {
            mapsDir.mkdir();
        }
        return mapsDir.getAbsolutePath();
    }

    public static void saveNewMap(String mapContent) {
        String mapDir = getMapDirectory();
        File directory = new File(mapDir);
        
        int nextNumber = 1;
        File[] files = directory.listFiles((dir, name) -> name.matches("map\\d+\\.txt"));
        if (files != null && files.length > 0) {
            for (File file : files) {
                try {
                    int num = Integer.parseInt(file.getName().substring(3, file.getName().length() - 4));
                    nextNumber = Math.max(nextNumber, num + 1);
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }

        try {
            File file = new File(directory, "map" + nextNumber + ".txt");
            FileWriter writer = new FileWriter(file);
            writer.write(mapContent);
            writer.close();
            System.out.println("Map saved as: " + file.getName());
        } catch (IOException e) {
            System.err.println("Error saving map: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Random rand = new Random();
        
        // Define dimension bounds
        final int MIN_WIDTH = 75;
        final int MAX_WIDTH = 100;
        final int MIN_HEIGHT = 75;
        final int MAX_HEIGHT = 100;
        
        // Generate multiple maps
        for (int i = 0; i < 2; i++) {
            // Generate uniform dimensions
            int width = MIN_WIDTH + (int)(rand.nextDouble() * (MAX_WIDTH - MIN_WIDTH));
            int height = MIN_HEIGHT + (int)(rand.nextDouble() * (MAX_HEIGHT - MIN_HEIGHT));
            
            // Ensure dimensions are even for better symmetry (optional)
            width = width - (width % 2);
            height = height - (height % 2);
            
            String mapContent = generateMap(width, height);
            if (mapContent != null) {
                saveNewMap(mapContent);  // Call static method directly
            }
            
            System.out.println("Generated map with dimensions: " + width + "x" + height);
        }
    }
}