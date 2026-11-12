package dev.karma.calamitouscore.compat.stellara;

import java.nio.*;import java.nio.file.*;import java.util.*;
import org.joml.Matrix4f;import org.lwjgl.glfw.*;import org.lwjgl.opengl.*;import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.opengl.GL33.*;

/** Actual Intel GPU timer/occlusion queries and exact framebuffer comparisons, hidden FBO only. */
public final class StellaraGpuCheck {
 private static final int[] CORNERS={0,1,4,7,2}; // ll,ul,ur,lr,center in Stellara's 12-vertex fan
 private static final int[] FAN={0,1,2,1,3,2,3,4,2,4,0,2};
 private static int program,vao,vbo,fbo,color,depth,projection;
 public static void main(String[] args)throws Exception {
  GLFWErrorCallback callback=GLFWErrorCallback.createPrint(System.err);callback.set();if(!GLFW.glfwInit())throw new IllegalStateException("No GLFW");long window=0;
  try {
   GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,3);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,3);GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE,GLFW.GLFW_OPENGL_CORE_PROFILE);
   window=GLFW.glfwCreateWindow(32,32,"Stellara hidden GPU test",0,0);if(window==0)throw new IllegalStateException("No context");GLFW.glfwMakeContextCurrent(window);GL.createCapabilities();
   System.out.println("GPU="+glGetString(GL_RENDERER)+"; driver="+glGetString(GL_VERSION));
   setup();
   System.out.println("mesh,width,height,yaw,vertices_before,vertices_after,fragments,gpu_before_ms,gpu_after_ms,upload_draw_wall_before_ms,upload_draw_wall_after_ms");
   for(int[] size:new int[][]{{1280,720},{1920,1080}}) {
    target(size[0],size[1]);
    try(var files=Files.list(Path.of(args[0]))) {for(Path path:files.filter(p->p.toString().endsWith(args.length > 1 ? ".before" : ".vertices")).sorted().toList()) {
     byte[] bytes=Files.readAllBytes(path);String id=path.getFileName().toString().replace(".vertices","");boolean fan=id.contains("Aurora") || id.contains("Ribbon");
     for(int direction=0;direction<(args.length > 1 ? 1 : 4);direction++) {
      if(args.length > 1) direction=Integer.parseInt(path.getFileName().toString().replaceAll(".*-yaw([0-9]+).before","$1"))/90;
      int fov=args.length > 2 ? Integer.parseInt(args[2]) : 70;
      Matrix4f matrix=new Matrix4f().perspective((float)Math.toRadians(fov),(float)size[0]/size[1],0.05F,1024).rotateY((float)(direction*Math.PI/2));
      float[] uniform=new float[16];matrix.get(uniform);glUniformMatrix4fv(projection,false,uniform);
      ByteBuffer baseline=MemoryUtil.memAlloc(bytes.length).order(ByteOrder.nativeOrder());baseline.put(bytes).flip();
      ByteBuffer culled;
      if(args.length > 1){byte[] after=Files.readAllBytes(path.resolveSibling(path.getFileName().toString().replace(".before",".after")));culled=MemoryUtil.memAlloc(after.length).order(ByteOrder.nativeOrder());culled.put(after).flip();}
      else culled=cull(baseline,matrix,fan);
      int mode=GL_TRIANGLES;
      ByteBuffer original=fan?baseline:triangulate(baseline);
      ByteBuffer reduced=fan?culled:triangulate(culled);
      byte[] reference=image(original,mode,size[0],size[1]), result=image(reduced,mode,size[0],size[1]);
      if(!Arrays.equals(reference,result))throw new AssertionError("Framebuffer differs: "+id+" direction="+direction+" at byte "+Arrays.mismatch(reference,result));
      double[] a=measure(original,mode),b=measure(reduced,mode);
      System.out.printf(Locale.ROOT,"%s,%d,%d,%d,%d,%d,%.0f,%.4f,%.4f,%.4f,%.4f%n",id,size[0],size[1],direction*90,original.remaining()/16,reduced.remaining()/16,a[2],a[0],b[0],a[1],b[1]);
      if(!fan){MemoryUtil.memFree(original);MemoryUtil.memFree(reduced);}MemoryUtil.memFree(baseline);MemoryUtil.memFree(culled);if(args.length > 1) break;
     }
    }}
   }
   System.out.println("PASS: pixel-identical original/culled buffers at every tested view; isolated GPU/CPU upload timings, NOT Minecraft FPS.");
  } finally {if(window!=0){if(program!=0)glDeleteProgram(program);if(vao!=0)glDeleteVertexArrays(vao);if(vbo!=0)glDeleteBuffers(vbo);if(fbo!=0)glDeleteFramebuffers(fbo);if(color!=0)glDeleteRenderbuffers(color);if(depth!=0)glDeleteRenderbuffers(depth);GLFW.glfwDestroyWindow(window);}GLFW.glfwTerminate();callback.free();}
 }
 private static ByteBuffer cull(ByteBuffer source,Matrix4f matrix,boolean fan) {
  ByteBuffer out=MemoryUtil.memAlloc(source.remaining()).order(ByteOrder.nativeOrder());SkyClipVolume clip=new SkyClipVolume();clip.set(matrix);float[] points=new float[15];int count=fan?12:4;
  for(int offset=0;offset<source.limit();offset+=count*16){for(int p=0;p<(fan?5:4);p++){int index=offset+(fan?CORNERS[p]:p)*16;for(int axis=0;axis<3;axis++)points[p*3+axis]=source.getFloat(index+axis*4);}if(!clip.outside(points,fan?5:4))for(int i=0;i<count*16;i++)out.put(source.get(offset+i));}
  out.flip();return out;
 }
 private static ByteBuffer triangulate(ByteBuffer input) {ByteBuffer out=MemoryUtil.memAlloc(input.remaining()/4*6).order(ByteOrder.nativeOrder());for(int o=0;o<input.limit();o+=64)for(int i:new int[]{0,1,2,2,3,0})for(int b=0;b<16;b++)out.put(input.get(o+i*16+b));return out.flip();}
 private static void setup(){
  int vs=shader(GL_VERTEX_SHADER,source("position_color.vsh"));
  int fs=shader(GL_FRAGMENT_SHADER,source("position_color.fsh"));
  program=glCreateProgram();glAttachShader(program,vs);glAttachShader(program,fs);glBindAttribLocation(program,0,"Position");glBindAttribLocation(program,1,"Color");glLinkProgram(program);if(glGetProgrami(program,GL_LINK_STATUS)==0)throw new IllegalStateException(glGetProgramInfoLog(program));glDeleteShader(vs);glDeleteShader(fs);glUseProgram(program);projection=glGetUniformLocation(program,"ProjMat");
  glUniformMatrix4fv(glGetUniformLocation(program,"ModelViewMat"),false,new Matrix4f().get(new float[16]));glUniform4f(glGetUniformLocation(program,"ColorModulator"),1,1,1,1);
  vao=glGenVertexArrays();vbo=glGenBuffers();glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);glEnableVertexAttribArray(0);glEnableVertexAttribArray(1);glVertexAttribPointer(0,3,GL_FLOAT,false,16,0);glVertexAttribPointer(1,4,GL_UNSIGNED_BYTE,true,16,12);glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);glDisable(GL_CULL_FACE);glEnable(GL_DEPTH_TEST);glDepthMask(false);
 }
 private static String source(String name){try(var in=StellaraGpuCheck.class.getResourceAsStream("/stellara-gpu/"+name)){if(in==null)throw new IllegalStateException("Missing vanilla shader fixture");return new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}}
 private static int shader(int type,String source){int s=glCreateShader(type);glShaderSource(s,source);glCompileShader(s);if(glGetShaderi(s,GL_COMPILE_STATUS)==0)throw new IllegalStateException(glGetShaderInfoLog(s));return s;}
 private static void target(int w,int h){if(fbo!=0){glDeleteFramebuffers(fbo);glDeleteRenderbuffers(color);glDeleteRenderbuffers(depth);}fbo=glGenFramebuffers();glBindFramebuffer(GL_FRAMEBUFFER,fbo);color=glGenRenderbuffers();glBindRenderbuffer(GL_RENDERBUFFER,color);glRenderbufferStorage(GL_RENDERBUFFER,GL_RGBA8,w,h);glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_RENDERBUFFER,color);depth=glGenRenderbuffers();glBindRenderbuffer(GL_RENDERBUFFER,depth);glRenderbufferStorage(GL_RENDERBUFFER,GL_DEPTH_COMPONENT24,w,h);glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER,depth);if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("FBO incomplete");glViewport(0,0,w,h);}
 private static void draw(ByteBuffer data,int mode){glDepthMask(true);glClearColor(0.08F,0.03F,0.12F,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);glDepthMask(false);glBufferData(GL_ARRAY_BUFFER,data,GL_DYNAMIC_DRAW);glDrawArrays(mode,0,data.remaining()/16);}
 private static byte[] image(ByteBuffer data,int mode,int w,int h){draw(data,mode);ByteBuffer pixels=MemoryUtil.memAlloc(w*h*4);try{glReadPixels(0,0,w,h,GL_RGBA,GL_UNSIGNED_BYTE,pixels);byte[] out=new byte[pixels.remaining()];pixels.get(out);return out;}finally{MemoryUtil.memFree(pixels);}}
 private static double[] measure(ByteBuffer data,int mode){int time=glGenQueries(),samples=glGenQueries();double[] gpu=new double[11],wall=new double[11];long fragments=0;try{for(int i=-5;i<11;i++){glFinish();long start=System.nanoTime();glBeginQuery(GL_TIME_ELAPSED,time);glBeginQuery(GL_SAMPLES_PASSED,samples);draw(data,mode);glEndQuery(GL_SAMPLES_PASSED);glEndQuery(GL_TIME_ELAPSED);glFinish();double ms=(System.nanoTime()-start)/1e6;long ns=glGetQueryObjecti64(time,GL_QUERY_RESULT);fragments=glGetQueryObjecti64(samples,GL_QUERY_RESULT);if(i>=0){gpu[i]=ns/1e6;wall[i]=ms;}}Arrays.sort(gpu);Arrays.sort(wall);return new double[]{gpu[5],wall[5],fragments};}finally{glDeleteQueries(time);glDeleteQueries(samples);}}
}
