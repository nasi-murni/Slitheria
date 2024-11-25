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
    private HashMap<Portal, Portal> portals;
    public Player player;

    public int icon_x; // x-coordinate of icon
    public int icon_y; // y-coordinate of icon
    public char prev = '+';  // to restore symbols
    public boolean gameWon = false;
    public boolean ctrlPressed = false;

    private StringBuilder mapBuffer = new StringBuilder();

    // CONSTANTS
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

    // Constructor for an empty map
    public VisualizedMap(){
        map = null;
        player = null;
    }

    // Constructor initializing a map by reading from a file
    public VisualizedMap(String path){
        Portal tempPortal = null;
        player = new Player(3);
        portals = new HashMap<>();

        try(Scanner scanIn = new Scanner(new File(path))){

            scanIn.nextLine(); // skip descriptor
            int numRow = scanIn.nextInt(); scanIn.nextLine();
            int numCol = scanIn.nextInt(); scanIn.nextLine();

            // Initialize map, it will be rectangular
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

                        if(tempPortal == null){
                            // If there are no established portal pairs
                            tempPortal = currentPortal;
                        }else{
                            // Map portals with the same id
                            if(tempPortal.getID() == c){
                                Portal firstPortal = tempPortal;

                                // Ensure bidirectionality
                                portals.put(firstPortal, currentPortal);
                                portals.put(currentPortal, firstPortal);
                                tempPortal = null;
                            }
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

    // Constructor for an initialized map
    public VisualizedMap(int row_size, int col_size){
        map = new char[row_size][col_size];
    }

    public void right(){
        synchronized(mapLock){
            if(validToMove(icon_x + 1, icon_y)){
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
            }
        }
    }

    public void left(){
        synchronized(mapLock){
            if(validToMove(icon_x - 1, icon_y)){
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
            }
        }
    }

    public void up(){
        synchronized(mapLock){
            if(validToMove(icon_x, icon_y - 1)){
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
            }
        }
    }

    public void down(){
        synchronized(mapLock){
            if(validToMove(icon_x, icon_y + 1)){
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
        Portal outPortal = portals.get(inPortal);
    
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

    public String toString(){
        synchronized(mapLock){
            mapBuffer.setLength(0); // Clear buffer
            for(int row = 0; row < map.length; row++){
                for(int col = 0; col < map[0].length; col++){
                    char c = map[row][col];

                    if(Character.isDigit(c)){
                        int colorNd = c - '0'; // parsing char to int with offset of 1 from 0
                        int colorCode = DISTINCT_PORTAL_COLORS[colorNd]; 
                        mapBuffer.append("\033[38;5;" + colorCode + "m|\033[0m ");
                    }else{
                        switch(c){
                        case 'x':
                            if(prev == '*') mapBuffer.append("\033[1;37m" + c + "\033[0m ");
                            else mapBuffer.append("\033[1;36m" + c + "\033[0m ");
                            break;
                        case '#':
                            mapBuffer.append("\033[38;5;255m" + c + "\033[0m ");
                            break;
                        case ':':
                            mapBuffer.append("\033[38;5;46m" + c + "\033[0m ");
                            break;
                        case '*':
                            mapBuffer.append("\033[1;31m" + c + "\033[0m ");
                            break;
                        default:
                            mapBuffer.append("\033[38;5;234m+ \033[0m");
                        }
                    }
                }
                if(row < map.length - 1) mapBuffer.append("\n");
            }
            return mapBuffer.toString();
        }
    }
}
