/**
 * Class for a Player object.
 * Encapsulates all information about a player:
 * - Hitpoints
 */
public class Player {
    private int hp;

    // Empty Constructor
    public Player(){
        this.hp = 0;
    }

    // Constructor
    public Player(int hp){
        this.hp = hp;
    }

    // Getter for hp
    public int getHP(){
        return hp;
    }

    // Setter for hp
    public void setHP(int hp){
        this.hp = hp;
    }
}
