/**
 * Class for a Player object.
 * Encapsulates all information about a player:
 * - Hitpoints
 */
public class Player {
    private int hp = 5; // 5 by default

    // Invincibility properties
    private boolean isInvincible = false;
    private long invincibilityStartTime = 0;
    private static final long INVINICIBILITY_DURATION = 1500; // 1500 ms --> 1.5 seconds


    /*
     * HOW ACCELERATION AND DECELERATION WORK:
     * Acceleration: When a movement key is pressed, the velocity increases by a fixed amount (ACCELERATION) each frame, 
     * simulating the player gaining speed.
     * 
     * Deceleration: When no movement key is pressed, the velocity decreases by a fixed amount (DECELERATION) each frame,
     * simulating the player losing speed until they stop.
     * 
     * In the VisualizedMap class, we handle the velocity by tracking a movement accumulator.
     * Movement Accumulator: Tracks the total accumulated velocity (up to a max of MAX_VELOCITY), and correspondingly
     * moves the player icon however many tiles that it corresponds to, while subtracting the movement from the accumulator
     * for every tile moved.
     */
    private double velocityX = 0;
    private double velocityY = 0;
    private final double ACCELERATION = 0.2;
    private final double DECELERATION = 1.0;
    private final double MAX_VELOCITY = 2.0;

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

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

    public double getVelocityX(){
        return velocityX;
    }

    public double getVelocityY(){
        return velocityY;
    }

    public void accelerate(Direction direction){
        switch(direction){
            case LEFT:
                velocityX = Math.max(velocityX - ACCELERATION, -MAX_VELOCITY);
                break;
            case RIGHT:
                velocityX = Math.min(velocityX + ACCELERATION, MAX_VELOCITY);
                break;
            case UP:
                velocityY = Math.max(velocityY - ACCELERATION, -MAX_VELOCITY);
                break;
            case DOWN:
                velocityY = Math.min(velocityY + ACCELERATION, MAX_VELOCITY);
                break;
        }
    }

    public void decelerate(){
        // Decelerate X
        if(velocityX > 0){ 
            // Moving right
            velocityX = Math.max(velocityX - DECELERATION, 0);
        } else if(velocityX < 0){ 
            // Moving left
            velocityX = Math.min(velocityX + DECELERATION, 0);
        }

        // Decelerate Y
        if(velocityY > 0){ 
            // Moving down
            velocityY = Math.max(velocityY - DECELERATION, 0);
        } else if(velocityY < 0){ 
            // Moving up
            velocityY = Math.min(velocityY + DECELERATION, 0);
        }

    }
}
