package legend.game.input;

// Needed because GLFW has overlap in int values
// GLFW_GAMEPAD_BUTTON_A == GLFW_HAT_CENTERED == GLFW_MOUSE_BUTTON_1
public enum InputTypeEnum {
  KEYBOARD,
  MOUSE_BUTTON,
  GAMEPAD_BUTTON,
  GAMEPAD_AXIS,
  GAMEPAD_AXIS_BUTTON_POSITIVE,
  GAMEPAD_AXIS_BUTTON_NEGATIVE,
  GAMEPAD_HAT,
}
