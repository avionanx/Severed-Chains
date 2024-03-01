package legend.game.submap;

import org.joml.Vector3f;

public record CameraInfo(Vector3f viewpoint, Vector3f refpoint, int rotation, int projectionDistance) { }


