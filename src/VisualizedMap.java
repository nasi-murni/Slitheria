/**
 * Class for a text-based map object.
 * Uses a 2D array that represents an m x n matrix, where
 * m is the number of rows and n is the number of columns
 * 
 * Specifications:
 * - Uses a 2D array that represents an m x n matrix, where
 *   m is the number of rows and n is the number of columns.
 * 
 * - 'x' represents the user's character/icon.
 * - '+' represents an empty point.
 * - '#' represents a wall. 
 * - ':' represents the goal.
 * - '|' represents a portal.
 * 
 * - A variety of distinct symbols that will represent obstacles or power-ups
 * 
 * 
 * Dependencies:
 * (JCL) Scanner.java, File.java, IOException.java
 * Portal.java, Player.java
 */

import java.util.*;
import java.io.File;
import java.io.IOException;

public class VisualizedMap {
    private final Object mapLock = new Object(); // For synchronization
    public char[][] map;
    private HashMap<Portal, Portal> pairedPortals;
    public Player player;

    public int icon_x; // x-coordinate of icon
    public int icon_y; // y-coordinate of icon
    public char prev = '+';  // to restore symbols
    public boolean gameWon = false;
    public boolean ctrlPressed = false;
    private StringBuilder mapBuffer = new StringBuilder();

    public static int viewportX = 0, viewportY = 0;
    public int VIEWPORT_WIDTH = 40;
    public int VIEWPORT_HEIGHT = 20;

    // Add movement accumulator to track sub-grid movement
    private double movementAccumulatorX = 0;
    private double movementAccumulatorY = 0;

    private static final int[] DISTINCT_PORTAL_COLORS = {
        226,  // Bright Yellow
        51,   // Bright Cyan
        21,   // Bright Blue
        93,   // Purple
        183,  // Pink
        154,  // Lime
        214,  // Orange
        147,  // Medium Purple
    };
    
    // Constructor initializing a map by reading from a string
    public VisualizedMap(String mapContent, int vWidth, int vHeight){
        this.VIEWPORT_WIDTH = vWidth;
        this.VIEWPORT_HEIGHT = vHeight;

        player = new Player();
        pairedPortals = new HashMap<>();
        HashMap<Character, Portal> pendingPortals = new HashMap<>();

        String[] lines = mapContent.split("\n");

        int width = Integer.parseInt(lines[1]);
        int height = Integer.parseInt(lines[2]);

        // Initialize map, it will be rectangular
        map = new char[height][width];

        // Iterate through each row
        for(int currRow = 0; currRow < height; currRow++){
            // Iterate through each column of a row
            char[] row = lines[currRow + 3].toCharArray();
            
            // Identify the x and y coordinates of the icon
            for(int currCol = 0; currCol < row.length; currCol++){
                char c = row[currCol];
                if(c == 'x'){
                    icon_x = currCol;
                    icon_y = currRow;
                }

                // Load portal pairs
                if(Character.isDigit(c)){
                    Portal currentPortal = new Portal(currCol, currRow, c);

                    if(!pendingPortals.containsKey(c)){
                        // If there are no established portal of that ID
                        pendingPortals.put(c, currentPortal);
                    }else{
                        // Map portals with the same id
                        Portal oldPortal = pendingPortals.get(c);

                        // Ensure bidirectionality
                        pairedPortals.put(oldPortal, currentPortal);
                        pairedPortals.put(currentPortal, oldPortal);
                        
                        // Remove from pendingPortals HashMap
                        pendingPortals.remove(c);
                    }
                }
            }
            // Initialize each row
            map[currRow] = row;
        }
    }

    // Constructor initializing a map by reading from a file
    public VisualizedMap(File mapContent, int vWidth, int vHeight){
        this.VIEWPORT_WIDTH = vWidth;
        this.VIEWPORT_HEIGHT = vHeight;

        player = new Player();
        pairedPortals = new HashMap<>();
        HashMap<Character, Portal> pendingPortals = new HashMap<>();

        try(Scanner scanIn = new Scanner(mapContent)){

            scanIn.nextLine(); // skip descriptor
            int numCol = scanIn.nextInt(); scanIn.nextLine();
            int numRow = scanIn.nextInt(); scanIn.nextLine();

            // Initialize map and dirty, it will be rectangular
            map = new char[numRow][numCol];

            // Iterate through each line of the file and parse map data
            for(int currRow = 0; scanIn.hasNextLine(); currRow++){
                // Parse each line
                char[] row = scanIn.nextLine().toCharArray(); // O(n)
                
                // Identify the x and y coordinates of the icon
                for(int currCol = 0; currCol < row.length; currCol++){
                    char c = row[currCol];
                    if(c == 'x'){
                        icon_x = currCol;
                        icon_y = currRow;
                    }

                    // Load portal pairs
                    if(Character.isDigit(c)){
                        Portal currentPortal = new Portal(currCol, currRow, c);

                        if(!pendingPortals.containsKey(c)){
                            // If there are no established portal of that ID
                            pendingPortals.put(c, currentPortal);
                        }else{
                            // Map portals with the same id
                            Portal oldPortal = pendingPortals.get(c);

                            // Ensure bidirectionality
                            pairedPortals.put(oldPortal, currentPortal);
                            pairedPortals.put(currentPortal, oldPortal);
                            
                            // Remove from pendingPortals HashMap
                            pendingPortals.remove(c);
                        }
                    }
                }
                // Initialize each row
                map[currRow] = row;
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    // -- Getters -- 
    public int getWidth(){
        return map[0].length;
    }

    public int getHeight(){
        return map.length;
    }
    
    // Update movement methods to use velocity
    public void updatePosition(){
        synchronized(mapLock){
            // Add current velocity to accumulators
            movementAccumulatorX += player.getVelocityX();
            movementAccumulatorY += player.getVelocityY();

            // Handle X movement when accumulator reaches threshold
            while(Math.abs(movementAccumulatorX) >= 1.0){
                int moveX = (movementAccumulatorX > 0) ? 1 : -1;

                if(validToMove(icon_x + moveX, icon_y)){
                    char nextTile = map[icon_y][icon_x + moveX];

                    // Handle portal logic
                    if(ctrlPressed && Character.isDigit(nextTile)){
                        handlePortal(icon_x + moveX, icon_y);
                        return;
                    } else{
                        // Normal movement
                        map[icon_y][icon_x] = prev;
                        icon_x += moveX;
                        map[icon_y][icon_x] = 'x';
                        prev = nextTile;
                        handleSpecialTile(prev);
                    }
                }

                // Subtract the movement we just made
                movementAccumulatorX -= moveX;
            }

            // Handle Y movement when accumulator reaches threshold
            while(Math.abs(movementAccumulatorY) >= 1.0){
                int moveY = (movementAccumulatorY > 0) ? 1 : -1;

                if(validToMove(icon_x, icon_y + moveY)){
                    char nextTile = map[icon_y + moveY][icon_x];

                    // Handle portal logic
                    if(ctrlPressed && Character.isDigit(nextTile)){
                        handlePortal(icon_x, icon_y + moveY);
                        return;
                    } else{
                        // Normal movement
                        map[icon_y][icon_x] = prev;
                        icon_y += moveY;
                        map[icon_y][icon_x] = 'x';
                        prev = nextTile;
                        handleSpecialTile(prev);
                    }
                }

                // Subtract the movement we just made
                movementAccumulatorY -= moveY;
            }
        }
    }

    public boolean validToMove(int newX, int newY){
        return newX >= 1 && newX < map[0].length - 1 &&
               newY >= 1 && newY < map.length - 1 &&
               map[newY][newX] != '#';
    }

    private void handleSpecialTile(char tile){
        // If the previous tile we were at is any of the special tiles,
        // restore the tile back.
        switch(tile){
            case ':':
                gameWon = true;
                break;

            case '*':
                player.setHP(player.getHP() - 1);
                break;
        }
    }

    private void handlePortal(int x, int y){
        Portal inPortal = new Portal(x, y, map[y][x]);
        Portal outPortal = pairedPortals.get(inPortal);
    
        if(outPortal != null){

            // Restore tile we were standing on
            map[icon_y][icon_x] = prev;

            // Update icon's position
            icon_x = outPortal.getX();
            icon_y = outPortal.getY();
        
            
            // Move icon to outPortal graphically
            prev = map[outPortal.getY()][outPortal.getX()]; // save value of previous tile
            map[outPortal.getY()][outPortal.getX()] = 'x';
        }
    }

    // --------- Rendering Methods ---------
    public String toString(){
        synchronized(mapLock){
            mapBuffer.setLength(0); // Clear buffer

            // Calculate viewport position centered on player
            viewportX = Math.max(0, Math.min(icon_x - VIEWPORT_WIDTH/2, 
                                            map[0].length - VIEWPORT_WIDTH));
            viewportY = Math.max(0, Math.min(icon_y - VIEWPORT_HEIGHT/2,
                                            map.length - VIEWPORT_HEIGHT));

            int endRow = Math.min(map.length, viewportY + VIEWPORT_HEIGHT);
            int endCol = Math.min(map[0].length, viewportX + VIEWPORT_WIDTH);
            // Only render viewport area
            for(int row = viewportY; row < endRow; row++){
                for(int col = viewportX; col < endCol; col++){
                    renderTile(col, row);
                }
                if(row < viewportY + VIEWPORT_HEIGHT - 1) mapBuffer.append("\n");
            }
            return mapBuffer.toString();
        }
    }

    public void renderTile(int x, int y){
        char c = map[y][x];
        if(Character.isDigit(c)){
            int colorNd = c - '0'; // parsing char to int with offset of 1 from 0
            int colorCode = DISTINCT_PORTAL_COLORS[colorNd]; 
            mapBuffer.append("\033[38;5;" + colorCode + "m|\033[0m ");
        }else{
            switch(c){
            case 'x':
                if(player.isInvincible()) mapBuffer.append("\033[1;37m" + c + "\033[0m ");
                else mapBuffer.append("\033[1;36m" + c + "\033[0m ");
                break;
            case '#':
                mapBuffer.append("\033[38;5;255m" + c + "\033[0m ");
                break;
            case ':':
                mapBuffer.append("\033[38;5;226m" + c + "\033[0m ");
                break;
            case '*':
                mapBuffer.append("\033[1;31m" + c + "\033[0m ");
                break;
            case 'S':
                mapBuffer.append("\033[38;5;46m+ \033[0m");
                break;
            default:
                mapBuffer.append("\033[38;5;234m+ \033[0m");
            }
        }
    }
}
