package legend.game.submap;

public class Note {
  public NoteType noteType;

  public int frameIndex;

  public Note(NoteType type, int frame){
    this.noteType = type;
    this.frameIndex = frame;
  }
}

