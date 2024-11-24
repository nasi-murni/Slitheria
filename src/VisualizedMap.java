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
 */

import java.util.Scanner;
import java.io.File;
import java.io.IOException;

public class VisualizedMap {
    private final Object mapLock = new Object();
    public char[][] map;
    public Player player;
    public int icon_x; // x-coordinate of icon
    public int icon_y; // y-coordinate of icon
    public char prev = '+';  // to restore symbols
    public boolean gameWon = false;

    // Constructor for an empty map
    public VisualizedMap(){
        map = null;
        player = null;
    }

    // Constructor initializing a map by reading from a file
    public VisualizedMap(String path) throws IOException{
        player = new Player(3);
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
                    if(row[currCol] == 'x'){
                        icon_x = currCol;
                        icon_y = currRow;
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

    public int getHeight(){
        return map.length;
    }

    public int getWidth(){
        return map[0].length;
    }

    public char[][] getMap(){
        return map;
    }

    public void right(){
        synchronized(mapLock){
            if(validToMove(icon_x + 1, icon_y)){
                char nextTile = map[icon_y][icon_x + 1]; // save the tile we're about to move onto

                // Restore the tile we were standing on
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

                // Restore the tile we were standing on
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
                System.out.println("Congratulations! You've reached the goal!");
                break;
            case '|':
                handlePortal();
                break;
            
            case '*':
                player.setHP(player.getHP() - 1);
                System.out.println("Ouch. HP : " + player.getHP());
                break;
        }
    }

    private void handlePortal(){

    }

    public String toString(){
        synchronized(mapLock){
            StringBuilder str = new StringBuilder();
            for(int row = 0; row < map.length; row++){
                for(int col = 0; col < map[0].length; col++){
                    char c = map[row][col];

                    switch(c){
                    case 'x':
                        if(prev == '*') str.append("\033[1;37m" + c + "\033[0m ");
                        else str.append("\033[1;36m" + c + "\033[0m ");
                        break;
                    case '#':
                        str.append("\033[31m" + c + "\033[0m ");
                        break;
                    case ':':
                        str.append("\033[1;32m" + c + "\033[0m ");
                        break;
                    case '|':
                        str.append("\033[1;35m" + c + "\033[0m ");
                        break;
                    case '*':
                        str.append("\033[1;31m" + c + "\033[0m ");
                        break;
                    default:
                        str.append("+ ");
                    }
                }
                if(row < map.length - 1) str.append("\n");
            }
            return str.toString();
        }
    }
}
