/**
 * Class for a Player object.
 * Encapsulates all information about a player:
 * - Hitpoints
 */
public class Player {
    private int hp = 3; // 3 by default
    private boolean isInvincible = false;
    private long invincibilityStartTime = 0;
    private static final long INVINICIBILITY_DURATION = 3000; // 3000 ms --> 3 seconds

    public int getHP(){
        return hp;
    }

    public void setHP(int hp){
        if(!isInvincible()){
            this.hp = hp;
            // Start invincibility frames after taking damage
            isInvincible = true;
            invincibilityStartTime = System.currentTimeMillis();
        }
    }

    public boolean isInvincible(){
        if(isInvincible){
            long currentTime = System.currentTimeMillis();
            if(currentTime - invincibilityStartTime >= INVINICIBILITY_DURATION){
                isInvincible = false;
            }
        }
        return isInvincible;
    }
}
