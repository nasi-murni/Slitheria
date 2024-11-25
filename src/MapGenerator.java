import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.File;

public class MapGenerator {
    private static String getMapDirectory() {
        // First try to find the maps directory in src (development environment)
        File srcMaps = new File("maps");
        if (srcMaps.exists()) {
            return "../maps";
        }
        
        // If not found, use the maps directory next to the JAR (production environment)
        File maps = new File("maps");
        if (!maps.exists()) {
            maps.mkdir();
        }
        return "maps/";
    }

    private static int getNextMapNumber() {
        String mapDir = getMapDirectory();
        File directory = new File(mapDir);
        
        // Get all map files
        File[] files = directory.listFiles((dir, name) -> 
            name.matches("map\\d+\\.txt"));
        
        if (files == null || files.length == 0) {
            return 1; // Start with map1.txt if no maps exist
        }

        // Find the highest number
        int maxNumber = 0;
        for (File file : files) {
            String fileName = file.getName();
            // Extract the number from "mapX.txt"
            try {
                int number = Integer.parseInt(
                    fileName.substring(3, fileName.length() - 4));
                maxNumber = Math.max(maxNumber, number);
            } catch (NumberFormatException e) {
                // Skip files that don't match our format
                continue;
            }
        }

        return maxNumber + 1; // Next number after the highest
    }

    public void saveNewMap(String mapContent) {
        int nextNumber = getNextMapNumber();
        String mapName = "map" + nextNumber;
        String mapDir = getMapDirectory();
        String filePath = mapName + ".txt";
        
        try {
            File file = new File(mapDir, filePath);
            
            // Write the map content
            FileWriter writer = new FileWriter(file);
            writer.write(mapContent);
            writer.close();
            
            System.out.println("Map saved as: " + file.getName());
        } catch (IOException e) {
            System.err.println("Error saving map: " + e.getMessage());
        }
    }

    public static String generateLargeMap(int width, int height) {
        StringBuilder mapContent = new StringBuilder();
        try{
            mapContent.append("Standard " + height + "x" + width + " map with obstacles.\n");
            mapContent.append(height + "\n");
            mapContent.append(width + "\n");

            char[][] map = new char[height][width];
            Random rand = new Random();

            // Fill with walls initially
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    map[i][j] = '#';
                }
            }

            // Generate maze using recursive backtracking
            generateMaze(map, 1, 1);

            // Add rooms
            int numRooms = 10;
            List<Rectangle> rooms = new ArrayList<>();
            for (int i = 0; i < numRooms; i++) {
                int roomWidth = rand.nextInt(8) + 5;  // 5-12 width
                int roomHeight = rand.nextInt(8) + 5; // 5-12 height
                int x = rand.nextInt((width-2) - roomWidth) + 1;
                int y = rand.nextInt((height-2) - roomHeight) + 1;

                Rectangle newRoom = new Rectangle(x, y, roomWidth, roomHeight);
                boolean overlaps = false;
                for (Rectangle room : rooms) {
                    if (room.intersects(newRoom)) {
                        overlaps = true;
                        break;
                    }
                }

                if (!overlaps) {
                    rooms.add(newRoom);
                    // Create room
                    for (int ry = y; ry < y + roomHeight; ry++) {
                        for (int rx = x; rx < x + roomWidth; rx++) {
                            map[ry][rx] = '+';
                        }
                    }
                }
            }

            // Connect rooms with corridors
            for (int i = 0; i < rooms.size() - 1; i++) {
                Rectangle r1 = rooms.get(i);
                Rectangle r2 = rooms.get(i + 1);
                createCorridor(map, r1.x + r1.width/2, r1.y + r1.height/2, 
                                r2.x + r2.width/2, r2.y + r2.height/2);
            }

            // Add safe zones (marked with 'S')
            for (Rectangle room : rooms) {
                if (rand.nextBoolean()) {
                    int sx = room.x + rand.nextInt(room.width);
                    int sy = room.y + rand.nextInt(room.height);
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (map[sy+dy][sx+dx] == '+') {
                                map[sy+dy][sx+dx] = 'S';
                            }
                        }
                    }
                }
            }

            // Add spikes in maze areas
            for (int i = 1; i < height-1; i++) {
                for (int j = 1; j < width-1; j++) {
                    if (map[i][j] == '+' && rand.nextDouble() < 0.1) {
                        map[i][j] = '*';
                    }
                }
            }

            // Add portal network (requiring specific path)
            int[][] portalPoints = new int[10][2];  // 5 pairs of portals
            int portalCount = 0;

            // Place portals strategically between rooms
            for (int i = 0; i < rooms.size() - 1 && portalCount < 10; i += 2) {
                Rectangle r1 = rooms.get(i);
                Rectangle r2 = rooms.get(i + 1);

                // Place portal pair
                portalPoints[portalCount] = new int[]{r1.x + r1.width/2, r1.y + r1.height/2};
                portalPoints[portalCount + 1] = new int[]{r2.x + r2.width/2, r2.y + r2.height/2};

                map[portalPoints[portalCount][1]][portalPoints[portalCount][0]] = 
                    Character.forDigit(portalCount/2, 10);
                map[portalPoints[portalCount + 1][1]][portalPoints[portalCount + 1][0]] = 
                    Character.forDigit(portalCount/2, 10);

                portalCount += 2;
            }

            // Place player at start (in first room)
            Rectangle startRoom = rooms.get(0);
            map[startRoom.y + 1][startRoom.x + 1] = 'x';

            // Place goal in last room
            Rectangle endRoom = rooms.get(rooms.size() - 1);
            map[endRoom.y + endRoom.height - 2][endRoom.x + endRoom.width - 2] = ':';

            // Write map to file
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    mapContent.append(map[i][j]);
                }
                mapContent.append("\n");
            }
            return mapContent.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Helper class for room generation
    static class Rectangle {
        int x, y, width, height;

        Rectangle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean intersects(Rectangle other) {
            return x < other.x + other.width + 2 && 
                x + width + 2 > other.x && 
                y < other.y + other.height + 2 && 
                y + height + 2 > other.y;
        }
    }

    // Maze generation using recursive backtracking
    private static void generateMaze(char[][] map, int x, int y) {
        map[y][x] = '+';
        
        int[] directions = {0, 1, 2, 3};
        shuffleArray(directions);
        
        for (int dir : directions) {
            int dx = (dir == 0 ? 2 : (dir == 1 ? -2 : 0));
            int dy = (dir == 2 ? 2 : (dir == 3 ? -2 : 0));
            int newX = x + dx;
            int newY = y + dy;
            
            if (newX > 0 && newX < map[0].length-1 && 
                newY > 0 && newY < map.length-1 && 
                map[newY][newX] == '#') {
                map[y + dy/2][x + dx/2] = '+';
                generateMaze(map, newX, newY);
            }
        }
    }

    // Create corridor between two points
    private static void createCorridor(char[][] map, int x1, int y1, int x2, int y2) {
        int x = x1;
        int y = y1;
        
        while (x != x2 || y != y2) {
            map[y][x] = '+';
            if (x < x2) x++;
            else if (x > x2) x--;
            if (y < y2) y++;
            else if (y > y2) y--;
        }
    }

    // Fisher-Yates shuffle
    private static void shuffleArray(int[] array) {
        Random rand = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    public static void main(String args[]){
        MapGenerator generator = new MapGenerator();
        String mapContent = generateLargeMap(50, 30);
        if (mapContent != null)
            generator.saveNewMap(mapContent);
    }
}
