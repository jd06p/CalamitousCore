package dev.karma.calamitouscore.compat.stellara;
import org.lwjgl.glfw.GLFW;import org.lwjgl.opengl.*;
public final class StellaraGlProbe {
 public static void main(String[] args) {
  org.lwjgl.glfw.GLFWErrorCallback callback=org.lwjgl.glfw.GLFWErrorCallback.createPrint(System.err); callback.set();
  if(!GLFW.glfwInit()) throw new IllegalStateException("GLFW unavailable");
  long window=0;
  try { GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,3);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,3);GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE,GLFW.GLFW_OPENGL_CORE_PROFILE);
   window=GLFW.glfwCreateWindow(32,32,"Stellara offscreen profiling",0,0); if(window==0)throw new IllegalStateException("No hidden OpenGL context");GLFW.glfwMakeContextCurrent(window);GL.createCapabilities();
   System.out.println("GL_VENDOR="+GL11.glGetString(GL11.GL_VENDOR));System.out.println("GL_RENDERER="+GL11.glGetString(GL11.GL_RENDERER));System.out.println("GL_VERSION="+GL11.glGetString(GL11.GL_VERSION));System.out.println("Timer queries="+GL.getCapabilities().OpenGL33);
  } finally {if(window!=0)GLFW.glfwDestroyWindow(window);GLFW.glfwTerminate();callback.free();}
 }
}
