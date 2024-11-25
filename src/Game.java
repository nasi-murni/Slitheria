/**
 * A simple program that will create a text-based game.
 * 
 * Dependecies:
 * (JCL) IOException.java, KeyEventDispatcher.java, KeyboardFocusManager.java, KeyEvent.java
 * 
 * -- Notes on synchronization --
 * Any function that modifies any variable that requires synchronization needs to use the
 * synchronization() function, determined by the table below:
 * 
 * Variable    | Game Loop Thread | Keyboard Thread | Needs Sync?
 * ------------|-----------------|-----------------|------------
 * wPressed    | reads           | writes          | YES
 * aPressed    | reads           | writes          | YES
 * sPressed    | reads           | writes          | YES
 * dPressed    | reads           | writes          | YES
 * running     | reads           | writes          | NO (volatile)
 * lastUpdate  | reads/writes    | no access       | NO
 * map         | reads/writes    | no access       | NO
 * player      | reads           | no access       | NO
 * 
 */

import java.util.Scanner;
import java.io.IOException;

// To listen to keyboard presses
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.EventQueue;

// Provides a focused window
import javax.swing.JFrame;

// Paths for OS-independent path handling
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class Game {
    // Composition
    public JFrame frame;
    public VisualizedMap map;
    public Player player;


    // volatile imposed for thread safety (synchronization lock)
    private volatile boolean running = true;
    private volatile boolean wPressed = false;
    private volatile boolean aPressed = false;
    private volatile boolean sPressed = false;
    private volatile boolean dPressed = false;
    private volatile boolean qPressed = false;
    private volatile boolean ctrlPressed = false;

    // Add buffer for double buffering (smoother rendering)
    private StringBuilder screenBuffer = new StringBuilder();
    private static final String CLEAR_SCREEN = "\033[H\033[2J";
    private static final String CURSOR_HOME = "\033[H";

    // Rendering
    private final double ONE_BILLION = 1000000000.00;
    private final int MS_TO_NS_SCALAR = 1000000;
    private long lastMoveTime = 0;
    private static final long MOVE_DELAY = 75; // delay in ms

    // Empty Constructor
    public Game(){
        this.map = null;
        this.player = null;
    }

    // Constructor
    public Game(VisualizedMap map){
        this.map = map;
        this.player = map.player;
        setupKeyboardListener();
    }

    // Setting up all the keyboard presses
    private void setupKeyboardListener(){
        // Create a window to maintain keyboard focus
        frame = new JFrame("Game Window");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        frame.setSize(50, 50);
        frame.setLocationRelativeTo(null);

        // Make sure window can be focused
        frame.setFocusable(true);
        frame.setVisible(true);

        EventQueue.invokeLater(() -> {
            frame.toFront();        // Brings window to front screen
            frame.requestFocus();   // Requests keyboard focus
            frame.setAlwaysOnTop(true);  // Forces window to front
            frame.setAlwaysOnTop(false); // Returns to normal window behavior
        });

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent ke){
                // Requiring synchronization since we are modifying key states that
                // might be read by the game loop thread
                synchronized (Game.class){
                    switch(ke.getID()){
                        case KeyEvent.KEY_PRESSED:
                            switch(ke.getKeyCode()){
                                case KeyEvent.VK_W: wPressed = true; break;
                                case KeyEvent.VK_A: aPressed = true; break;
                                case KeyEvent.VK_S: sPressed = true; break;
                                case KeyEvent.VK_D: dPressed = true; break;
                                case KeyEvent.VK_Q: qPressed = true; break;
                                case KeyEvent.VK_CONTROL: ctrlPressed = true; map.ctrlPressed = true; break;
                            }
                            break;
                        case KeyEvent.KEY_RELEASED:
                            switch (ke.getKeyCode()) {
                                case KeyEvent.VK_W: wPressed = false; break;
                                case KeyEvent.VK_A: aPressed = false; break;
                                case KeyEvent.VK_S: sPressed = false; break;
                                case KeyEvent.VK_D: dPressed = false; break;
                                case KeyEvent.VK_Q: qPressed = false; break;
                                case KeyEvent.VK_CONTROL: ctrlPressed = false; map.ctrlPressed = false; break;
                            }
                            break;
                    }
                    return false;
                }
            }
        });
    }

    // Initializing the thread
    public void gameLoop(){
        long before = System.currentTimeMillis();
        final double NS_PER_UPDATE = ONE_BILLION / 10; // 10 updates per second -- nanoseconds per update
        double delta = 0;

        // Initial render
        System.out.print(CLEAR_SCREEN);
        render();

        // Game continutes while running is true
        while(running){
            long now = System.currentTimeMillis();
            delta += (now - before) * MS_TO_NS_SCALAR / NS_PER_UPDATE;
            before = now;

            if (delta >= 1){
                updateGame();
            }
            
            render();

            // Check game end conditions
            if(!running || (ctrlPressed && qPressed) || player.getHP() <= 0 || map.gameWon) {
                break;  // Exit loop if any end condition is met
            }

            // Small sleep to prevent CPU overuse
            try{
                Thread.sleep(1);
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
                System.out.println();
                break;
            }
        }
        
        System.out.println();

        frame.dispose();
        if(ctrlPressed && qPressed){

            System.out.print("Succesfully quitted. Type 'Help' for more commands.\n");

        }else if(player.getHP() <= 0){

            System.out.print("\033[1;31mOh no");
            for(int i = 0; i < 3; i++){
                try{
                    Thread.sleep(1000);
                    System.out.print(".");
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
            try{Thread.sleep(1000);}catch(InterruptedException e){};
            System.out.print(" You've died.\033[0m\n");
            try{Thread.sleep(1000);}catch(InterruptedException e){};

            System.out.print("Restart? Type 'Help' for more commands.\n");

        }else if(map.gameWon){

            System.out.print("\033[32mCongratulations! ");
            try{Thread.sleep(1000);}catch(InterruptedException e){};
            System.out.print("You've won!\033[0m\n");
            try{Thread.sleep(1000);}catch(InterruptedException e){};

            System.out.print("Try our other levels! Type 'Help' for more commands.\n");
        }
    }

    /*
     * Function to oversee all updates: 
     * keyboard presses, symbol swaps, entities,
     * map rendering
     */
    private void updateGame() {
        // Requiring synchronization since we are reading the key states
        // that may be modified by the keyboard listener thread
        synchronized (Game.class) {
            long currentTime = System.currentTimeMillis();
            boolean moved = false;

            // Check for movement only after delay
            if (currentTime - lastMoveTime >= MOVE_DELAY) {
                if (wPressed && map.validToMove(map.icon_x, map.icon_y - 1)) {
                    map.up();
                    moved = true;
                }
                if (aPressed && map.validToMove(map.icon_x - 1, map.icon_y)) {
                    map.left();
                    moved = true;
                }
                if (sPressed && map.validToMove(map.icon_x, map.icon_y + 1)) {
                    map.down();
                    moved = true;
                }
                if (dPressed && map.validToMove(map.icon_x + 1, map.icon_y)) {
                    map.right();
                    moved = true;
                }

                if (moved) {
                    lastMoveTime = currentTime;
                }
            }
        }
    }

    private void render(){
        // Clear screen using ANSI codes with octal
        // Resources: https://gist.github.com/fnky/458719343aabd01cfb17a3a4f7296797

        // Clear buffer
        screenBuffer.setLength(0);

        // Build the entire screen in memory first
        screenBuffer.append(CURSOR_HOME) // Move cursor to top instead of clearing screen
                    .append("HP: ").append(player.getHP() + "\n")
                    .append(map.toString());

        System.out.print(screenBuffer.toString());
        System.out.flush(); // flush any buffered input
    }

    private void play(){
        printInstructions();

        // Create and start game thread
        Thread gameThread = new Thread(() -> {
            gameLoop();
        });
        gameThread.start();

        // Additional thread for background systems
        Thread systemThread = new Thread(() -> {
            while (running){
                // Can handle things like:
                // - Enemy AI updates
                // - Other features
                try {
                    Thread.sleep(100); // Update every 100 ms
                } catch (InterruptedException e){
                    // catches interrupt()
                    Thread.currentThread().interrupt(); // signals the thread to stop
                    break; // exit the thread
                }
            }
        });
        systemThread.start();
        
        try{
            gameThread.join(); // Wait for game thread to end
            systemThread.interrupt(); // Stop system thread abruptly
            systemThread.join(); // Wait for system thread to end
        }catch(InterruptedException e){
            System.out.println("Game interrupted!\n");
        }
    }

    private void printInstructions(){
        System.out.print("The objective is to get to the goal without dying.\n" + 
            "You are the 'x' icon.\n" + 
            "'+' denotes an empty space.\n" + 
            "'#' denotes a wall.\n" +
            "Use WASD -- W (Move up), A (Move left), S (Move down), D (Move right) to move.\n" + 
            "Good luck!\n");
            System.out.print(map + "\n");
    }

    private static void displayHelp(Scanner helpScanner) {
        boolean helpRunning = true;

            System.out.println("===============================");
            System.out.println("\033[1;32mGame Help\033[0m");
            System.out.println("\033[1;37mCommands:\033[0m");
            System.out.println("  Play <number>   - Start game with specified map");
            System.out.println("  Preview <number> - Show preview of specified map");
            System.out.println("  Help            - Display this help message");
            System.out.println("  Quit            - Exit the game");
            System.out.println("\n\033[1;37mGame Controls:\033[0m");
            System.out.println("  W - Move up");
            System.out.println("  A - Move left");
            System.out.println("  S - Move down");
            System.out.println("  D - Move right");
            System.out.println("  CTRL - Toggle portal teleporting");
            System.out.println("  CTRL+Q - Quit game");
            System.out.println("\n\033[1;37mMap Tiles:\033[0m");
            System.out.println("  x - Player");
            System.out.println("  + - Empty space");
            System.out.println("  # - Wall");
            System.out.println("  : - Goal");
            System.out.println("  * - Spike");
            System.out.println("  | - Portal");
            System.out.println();
            System.out.println("Type -h <map_tile> for additional information.");
            System.out.println("Type 'back' to return to main menu.");

        while(helpRunning) {
            String[] inputs = helpScanner.nextLine().split(" ");
            
            switch(inputs[0]) {
                case "-h":
                    if(inputs.length != 2) {
                        System.out.println("Usage: -h <map_tile>");
                        continue;
                    }
                    
                    switch(inputs[1].charAt(0)) {
                        case 'x':
                            System.out.println("'x' - Player icon - The very creation of your soul. How does it feel being entrapped inside a game?");
                            break;
                        case '+':
                            System.out.println("'+' - Empty space - But is it really empty though? It's a plus sign, but you're able to phase through it...");
                            break;
                        case '#':
                            System.out.println("'#' - Wall - This is an impassable object on the map! Or is it..?");
                            break;
                        case ':':
                            System.out.println("':' - Goal - I'm sure you know what this is.");
                            break;
                        case '|':
                            System.out.println("'|' - Portal - A mysterious object that can teleport oneself from one point to another. As long as the player knows the truth...");
                            break;
                        case '*':
                            System.out.println("'*' - Spike - The spikiest object in the world of printable characters. Don't mess with them, they're temperamental.");
                            break;
                        default:
                            System.out.println("Invalid input. Valid tiles are: 'x', '+', '#', ':', '|', and '*'.");
                    }
                    break;
                    
                case "back":
                    helpRunning = false;
                    break;
                
                default:
                    System.out.println("Invalid input. Use '-h <map_tile>' for tile information or 'back' to return.");
            }
        }
     }

    public static void main(String args[]) throws IOException{
        Path mapsDir = Paths.get("maps");
        Path mapPath;

        // For debugging, you can print the paths
    System.out.println("Maps dir: " + mapsDir);

        Scanner scan = new Scanner(System.in);

        System.out.println("\033[1;32mWelcome to Slitheria!\033[0m");

            System.out.print("\033[1mAbout:\033[0m This is a side-scrolling text-based rogue-like game.\n" + 
                            "       It features slithering your way through the tight abyss of \033[1mSlitheria\033[0m,\n" + 
                            "       where you must utilize your surrounding to get to the goal.\n\n");

        while(true){
            System.out.print("===============================");
            System.out.print("\n\033[1mPlay Map: \033[0mPlay <map_number>\n" +
                            "\033[1mMap Preview: \033[0mPreview <map_number>\n" +
                            "\033[1mHelp Page: \033[0mHelp\n");
            String input = scan.nextLine();
            String[] parsed = input.split(" ");

            switch(parsed[0].toLowerCase()){
                case "play":
                    if(parsed.length != 2){
                        System.out.print("Usage: Play <map_number>\n");
                        continue;
                    }

                    // Construct path
                    mapPath = mapsDir.resolve("map" + parsed[1] + ".txt");
                    if(Files.exists(mapPath)){
                        Game game = new Game(new VisualizedMap(mapPath.toString()));
                        game.play();
                    }else{
                        System.out.println("Map " + parsed[1] + " not found!");
                    }
                    break;

                case "preview":
                    if(parsed.length != 2){
                        System.out.print("Usage: Preview <map_number>\n");
                        continue;
                    }

                    mapPath = mapsDir.resolve("map" + parsed[1] + ".txt");
                    if(Files.exists(mapPath)){
                        VisualizedMap previewMap = new VisualizedMap(mapPath.toString());
                        System.out.print(previewMap + "\n");
                    }else{
                        System.out.println("Map " + parsed[1] + " not found!");
                    }
                    break;

                case "help":
                    displayHelp(scan);
                    break;

                case "quit":
                    System.out.print("Goodbye!\n");
                    scan.close();
                    return;

                default:
                    System.out.print("Invalid input. Type 'Help' for commands.\n");
                
            }
        }
    }
}
