package legend.game.submap;

import legend.game.modding.coremod.CoreMod;
import org.joml.Vector3f;

import static legend.core.GameEngine.CONFIG;

public class LaunchedNote {

  public NoteType noteType;

  public Vector3f position;

  public float rotation;
  public boolean finished;

  private int movementFrames = 80;
  private int frame;
  public LaunchedNote(NoteType type){
    this.noteType = type;
    this.position = new Vector3f(0.0f,0.0f,0.0f);
    this.finished = false;
    this.frame = 0;

    switch(noteType){
      case UP ->    this.rotation = 0 * 1.5708f;
      case RIGHT -> this.rotation = 1 * 1.5708f;
      case DOWN ->  this.rotation = 2 * 1.5708f;
      case LEFT ->  this.rotation = 3 * 1.5708f;
    }
    this.updatePosition();
  }
  public void frameStep(){
      this.frame += 1;
      if(this.frame == this.movementFrames){
        this.finished = true;
      }
    this.updatePosition();
  }
  private void updatePosition(){

    float updatedY = CONFIG.getConfig(CoreMod.INVERSE_ARROW_MOVEMENT_CONFIG.get()) ? 40.0f + (this.frame * 160/movementFrames) : 200.0f - (this.frame * 160/movementFrames);
    switch(noteType){
      case UP ->    this.position.set(204.0f, updatedY, 1.0f);
      case RIGHT -> this.position.set(244.0f, updatedY, 1.0f);
      case DOWN ->  this.position.set(164.0f, updatedY, 1.0f);
      case LEFT ->  this.position.set(124.0f, updatedY, 1.0f);
    }
  }
}
