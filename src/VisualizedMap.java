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

    private int viewportX = 0, viewportY = 0;
    public final int VIEWPORT_WIDTH = 40;
    public final int VIEWPORT_HEIGHT = 20;
    private boolean[][] dirty;


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

    // Constructor initializing a map by reading from a file
    public VisualizedMap(String path){
        player = new Player();
        pairedPortals = new HashMap<>();
        HashMap<Character, Portal> pendingPortals = new HashMap<>();

        try(Scanner scanIn = new Scanner(new File(path))){

            scanIn.nextLine(); // skip descriptor
            int numRow = scanIn.nextInt(); scanIn.nextLine();
            int numCol = scanIn.nextInt(); scanIn.nextLine();

            // Initialize map and dirty, it will be rectangular
            map = new char[numRow][numCol];
            dirty = new boolean[numRow][numCol];
            markAllDirty(); // Initially all tiles are dirty

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
    
    public void right(){
        synchronized(mapLock){
            if(validToMove(icon_x + 1, icon_y)){
                // Mark affected tiles as dirty
                markDirty(icon_x, icon_y);
                markDirty(icon_x + 1, icon_y);

                char nextTile = map[icon_y][icon_x + 1]; // save the tile we're about to move onto

                // Teleport if and only if ctrl is pressed and nextTile is a digit
                if(ctrlPressed && Character.isDigit(nextTile)){
                    handlePortal(icon_x + 1, icon_y);
                    return;
                }
                
                // Restore tile we were standing on
                map[icon_y][icon_x] = prev;
                // Move icon and save new tile
                map[icon_y][++icon_x] = 'x';
                prev = nextTile;
                handleSpecialTile(prev);

                // Check if we need to update viewport dirty area, i.e. if near viewport edge
                if(icon_x >= viewportX + VIEWPORT_WIDTH - 5 || icon_x <= viewportX + 5 ||
                    icon_y >= viewportY + VIEWPORT_HEIGHT - 2 || icon_y <= viewportY + 2)
                    markDirtyViewport();
            }
        }
    }

    public void left(){
        synchronized(mapLock){
            if(validToMove(icon_x - 1, icon_y)){
                // Mark affected tiles as dirty
                markDirty(icon_x, icon_y);
                markDirty(icon_x - 1, icon_y);

                char nextTile = map[icon_y][icon_x - 1]; // save the tile we're about to move onto

                // Teleport if and only if ctrl is pressed and nextTile is a digit
                if(ctrlPressed && Character.isDigit(nextTile)){
                    handlePortal(icon_x - 1, icon_y);
                    return;
                }
                // Restore tile we were standing on
                map[icon_y][icon_x] = prev;
                // Move icon and save new tile
                map[icon_y][--icon_x] = 'x';
                prev = nextTile;
                handleSpecialTile(prev);

                // Check if we need to update viewport dirty area, i.e. if near viewport edge
                if(icon_x >= viewportX + VIEWPORT_WIDTH - 5 || icon_x <= viewportX + 5 ||
                icon_y >= viewportY + VIEWPORT_HEIGHT - 2 || icon_y <= viewportY + 2)
                markDirtyViewport();
            }
        }
    }

    public void up(){
        synchronized(mapLock){
            if(validToMove(icon_x, icon_y - 1)){
                // Mark affected tiles as dirty
                markDirty(icon_x, icon_y);
                markDirty(icon_x, icon_y - 1);

                char nextTile = map[icon_y - 1][icon_x]; // save the tile we're about to move onto

                // Teleport if and only if ctrl is pressed and nextTile is a digit
                if(ctrlPressed && Character.isDigit(nextTile)){
                    handlePortal(icon_x, icon_y - 1);
                    return;
                }
                
                // Restore tile we were standing on
                map[icon_y][icon_x] = prev;
                // Move icon and save new tile
                map[--icon_y][icon_x] = 'x';
                prev = nextTile;
                handleSpecialTile(prev);
                
                // Check if we need to update viewport dirty area, i.e. if near viewport edge
                if(icon_x >= viewportX + VIEWPORT_WIDTH - 5 || icon_x <= viewportX + 5 ||
                    icon_y >= viewportY + VIEWPORT_HEIGHT - 2 || icon_y <= viewportY + 2)
                    markDirtyViewport();
            }
        }
    }

    public void down(){
        synchronized(mapLock){
            if(validToMove(icon_x, icon_y + 1)){
                // Mark affected tiles as dirty
                markDirty(icon_x, icon_y);
                markDirty(icon_x, icon_y + 1);

                char nextTile = map[icon_y + 1][icon_x]; // save the tile we're about to move onto

                // Teleport if and only if ctrl is pressed and nextTile is a digit
                if(ctrlPressed && Character.isDigit(nextTile)){
                    handlePortal(icon_x, icon_y + 1);
                    return;
                }
                // Restore tile we were standing on
                map[icon_y][icon_x] = prev;
                // Move icon and save new tile
                map[++icon_y][icon_x] = 'x';
                prev = nextTile;
                handleSpecialTile(prev);

                // Check if we need to update viewport dirty area, i.e. if near viewport edge
                if(icon_x >= viewportX + VIEWPORT_WIDTH - 5 || icon_x <= viewportX + 5)
                    markDirtyViewport();
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
            // Mark entire viewport as dirty since teleportation changes view significantly
            markDirtyViewport();

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
    
    // ---------------------------------------------------
    // --------- Marking Methods ---------
    private void markDirty(int x, int y){
        if(x >= 0 && x < map[0].length && y >= 0 && y < map.length){
            dirty[y][x] = true;
        }
    }

    private void markAllDirty(){
        for(int i = 0; i < dirty.length; i++){
            Arrays.fill(dirty[i], true);
        }
    }

    private void markDirtyViewport(){
        int startRow = Math.max(0, viewportY);
        int endRow = Math.min(map.length, viewportY + VIEWPORT_HEIGHT);
        int startCol = Math.max(0, viewportX);
        int endCol = Math.min(map[0].length, viewportX + VIEWPORT_WIDTH);

        for(int row = startRow; row < endRow; row++){
            for(int col = startCol; col < endCol; col++){
                dirty[row][col] = true;
            }
        }
    }

    public void setViewport(int x, int y){
        this.viewportX = x;
        this.viewportY = y;
    }
    
    // --------- End of marking methods ---------
    // ---------------------------------------------------

    // --------- Rendering Methods ---------
    public String toString(){
        synchronized(mapLock){
            mapBuffer.setLength(0); // Clear buffer

            // Calculate viewport position centered on player
            viewportX = Math.max(0, Math.min(icon_x - VIEWPORT_WIDTH/2, 
                                            map[0].length - VIEWPORT_WIDTH));
            viewportY = Math.max(0, Math.min(icon_y - VIEWPORT_HEIGHT/2,
                                            map.length - VIEWPORT_WIDTH));

            // Only render viewport area
            for(int row = viewportY; row < viewportY + VIEWPORT_HEIGHT && row < map.length; row++){
                for(int col = viewportX; col < viewportX + VIEWPORT_WIDTH && col < map[0].length; col++){
                    if(dirty[row][col]){
                    renderTile(col, row);
                    dirty[row][col] = false;
                    }
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
                mapBuffer.append("\33[38;5;46m+ \033[0m");
                break;
            default:
                mapBuffer.append("\033[38;5;234m+ \033[0m");
            }
        }
    }
}
