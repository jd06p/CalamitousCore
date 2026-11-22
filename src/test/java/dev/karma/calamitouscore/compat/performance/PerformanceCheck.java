package dev.karma.calamitouscore.compat.performance;

import com.mojang.blaze3d.vertex.*;
import dev.karma.calamitouscore.compat.stellara.StellaraTestMixinService;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.zip.*;
import net.minecraft.core.BlockPos;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.MixinService;

public final class PerformanceCheck {
    static final Map<String,byte[]> INPUT=new HashMap<>(), OUTPUT=new HashMap<>();
    private static int checks;
    private static final List<String> CSV=new ArrayList<>(List.of("case,original_us,optimized_us,original_bytes,optimized_bytes"));
    public static void main(String[] args) throws Exception {
        boolean server=args.length>0 && args[0].equals("server");
        Map<String,String> methods=new HashMap<>(),fields=new HashMap<>();
        for(String line:Files.readAllLines(Path.of("build/createMcpToSrg/output.tsrg"))){if(!line.startsWith("\t")||line.startsWith("\t\t"))continue;String[]p=line.trim().split(" +");if(p.length>=3&&p[1].startsWith("("))methods.put(p[2],p[0]);else if(p.length==2)fields.put(p[1],p[0]);}
        for(String jar:List.of("thedulling-1.0.0-profiled.jar","geckolib-forge-1.20.1-4.8.4.jar","summonity-0.1.7.1.jar","enlightend-5.0.14-1.20.1.jar","journeymap-forge-1.20.1-6.0.7.jar","NostalgicTweaks-mc1.20.1-Forge-2.0.0-beta.922.jar","jei-1.20.1-forge-15.74.0.229.jar")){
            try(ZipFile zip=new ZipFile("libs/"+jar)){for(ZipEntry e:Collections.list(zip.entries()))if(e.getName().endsWith(".class")){
                byte[] data;try(var in=zip.getInputStream(e)){data=in.readAllBytes();}ClassWriter w=new ClassWriter(0);
                new ClassReader(data).accept(new ClassRemapper(w,new Remapper(){@Override public String mapMethodName(String o,String n,String d){return methods.getOrDefault(n,n);}@Override public String mapFieldName(String o,String n,String d){return fields.getOrDefault(n,n);}}),0);
                INPUT.put(e.getName().replace('/','.').replace(".class",""),w.toByteArray());
            }}
        }
        if(!server) mergedLightFixture();
        StellaraTestMixinService.fixtures=INPUT;System.setProperty("mixin.env.disableRefMap","true");MixinBootstrap.init();MixinEnvironment.getDefaultEnvironment().setSide(server?MixinEnvironment.Side.SERVER:MixinEnvironment.Side.CLIENT);
        Mixins.addConfiguration("calamitouscore.performance.mixins.json");var phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.DEFAULT);
        IMixinTransformer transformer=((StellaraTestMixinService)MixinService.getService()).transformer();
        List<String> common=List.of("net.mcreator.thedulling.world.DullingWorldData","com.fevzi.summonity.entity.MinionHelper","net.mcreator.enlightened_end.procedures.OozeRainChanceProcedure");
        List<String> client=List.of("net.mcreator.thedulling.client.FanWindRenderer","journeymap.client.model.chunk.ChunkMD","journeymap.client.cartography.render.SurfaceRenderer","mod.adrenix.nostalgic.tweak.TweakResult","mezz.jei.common.util.MathUtil","net.mcreator.thedulling.client.HealingPylonRenderEvents","net.mcreator.thedulling.client.VfxSphereRenderer","net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase");
        Loader old=new Loader(false), optimized=new Loader(true);
        for(String name:common){OUTPUT.put(name,transformer.transformClassBytes(name,name,INPUT.get(name)));require(!Arrays.equals(INPUT.get(name),OUTPUT.get(name)),"Common hook absent "+name);verify(OUTPUT.get(name),optimized);}
        if(server){for(String name:client)if(INPUT.containsKey(name))require(Arrays.equals(INPUT.get(name),transformer.transformClassBytes(name,name,INPUT.get(name))),"Client hook activated on SERVER "+name);System.out.println("PASS: common performance hooks transform in SERVER mode; all client-only hooks excluded.");return;}
        for(String name:client){OUTPUT.put(name,transformer.transformClassBytes(name,name,INPUT.get(name)));require(!Arrays.equals(INPUT.get(name),OUTPUT.get(name)),"Client hook absent "+name);verify(OUTPUT.get(name),optimized);}
        positions();tweaks(old,optimized);jei(old,optimized);geometry(old,optimized);oozeShape();oozeSemantics();lightCache();threadIsolation();MinionIndexFixture.run();RecountFixture.run();
        Files.write(Path.of("build/performance-wave2-benchmark.csv"),CSV);
        System.out.println("PASS: "+checks+" assertions; all 11 performance targets transformed/ASM-verified. Isolated fixtures, not live world MSPT/FPS or Embeddium rendering verification.");
    }
    private static void mergedLightFixture() throws Exception {
        String target="net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase", source="mod/adrenix/nostalgic/mixin/tweak/candy/world_lighting/BlockStateBaseMixin";
        ClassNode base;try(var in=PerformanceCheck.class.getResourceAsStream("/"+target+".class")){base=node(in.readAllBytes());}
        ClassWriter remapped=new ClassWriter(0);new ClassReader(INPUT.get(source.replace('/','.'))).accept(new ClassRemapper(remapped,new Remapper(){@Override public String map(String n){return n.equals(source)?target:n;}}),0);
        for(MethodNode method:node(remapped.toByteArray()).methods)if(method.name.contains("nt_world_lighting$modifyLightBlock")){method.visibleAnnotations=null;method.invisibleAnnotations=null;base.methods.add(method);}
        ClassWriter w=new ClassWriter(0);base.accept(w);INPUT.put(target.replace('/','.'),w.toByteArray());
    }
    private static void positions() throws Exception {
        for(int i=-2000;i<4000;i++){
            BlockPos a=ImmutablePositions.at(i*317,i,-i*571),b=ImmutablePositions.at(i*317,i,-i*571);
            require(a==b && !(a instanceof BlockPos.MutableBlockPos) && a.getY()==i,"Immutable position value/identity "+i);
            require(HeightIntegers.valueOf(i).intValue()==i,"Height changed "+i);
        }
        BlockPos[] tile = new BlockPos[16384];
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < tile.length; i++) {
                BlockPos pos = ImmutablePositions.at(-32 + (i & 15), -64 + (i >> 8), 64 + (i >> 4 & 15));
                if (pass == 0) tile[i] = pos;
                else require(tile[i] == pos, "Unexpected allocation revisiting a coordinate tile");
            }
        }
        List<BlockPos> held=new ArrayList<>();for(int i=0;i<50000;i++)held.add(ImmutablePositions.at(i,i%500,i*13));
        for(int i=0;i<held.size();i++)require(held.get(i).getX()==i && held.get(i).getZ()==i*13,"Retained position mutated");
        var executor=Executors.newFixedThreadPool(4);try{List<Callable<Boolean>> jobs=new ArrayList<>();for(int t=0;t<8;t++){int thread=t;jobs.add(()->{for(int i=0;i<10000;i++)if(!ImmutablePositions.at(i,thread,-i).equals(new BlockPos(i,thread,-i)))return false;return true;});}for(var f:executor.invokeAll(jobs))require(f.get(),"Concurrent position mismatch");}finally{executor.shutdownNow();}
        bench("journeymap_repeated_coordinate",()->blackhole=new BlockPos(312,220,-78),()->blackhole=ImmutablePositions.at(312,220,-78),20000);
        bench("journeymap_height_box",()->blackhole=Integer.valueOf(220),()->blackhole=HeightIntegers.valueOf(220),20000);
    }
    private static void tweaks(Loader old,Loader optimized)throws Exception{
        String name="mod.adrenix.nostalgic.tweak.TweakResult";
        for(Loader loader:List.of(old,optimized)){
            Class<?> type=loader.loadClass(name);AtomicInteger calls=new AtomicInteger();Object value=type.getConstructor(Supplier.class).newInstance((Supplier<Integer>)calls::incrementAndGet);
            var get=type.getMethod("get");var clear=type.getMethod("clear");
            for(int i=0;i<1000;i++)require(get.invoke(value).equals(1),"Tweak result not memoized");require(calls.get()==1,"Duplicate delegate evaluation");clear.invoke(value);require(get.invoke(value).equals(2),"Tweak clear failed");
            AtomicInteger nulls=new AtomicInteger();Object nullable=type.getConstructor(Supplier.class).newInstance((Supplier<Object>)()->{nulls.incrementAndGet();return null;});get.invoke(nullable);get.invoke(nullable);require(nulls.get()==2,"Null cached contrary to ConcurrentHashMap semantics");
            AtomicInteger concurrentCalls=new AtomicInteger();Object shared=type.getConstructor(Supplier.class).newInstance((Supplier<Integer>)concurrentCalls::incrementAndGet);
            ExecutorService executor=Executors.newFixedThreadPool(8);try{List<Callable<Object>> jobs=new ArrayList<>();for(int i=0;i<40;i++)jobs.add(()->get.invoke(shared));for(var result:executor.invokeAll(jobs))require(result.get().equals(1),"Concurrent tweak result");require(concurrentCalls.get()==1,"Concurrent duplicate computation");}finally{executor.shutdownNow();}
        }
        Object a=old.loadClass(name).getConstructor(Supplier.class).newInstance((Supplier<Integer>)()->4),b=optimized.loadClass(name).getConstructor(Supplier.class).newInstance((Supplier<Integer>)()->4);
        var ga=a.getClass().getMethod("get");var gb=b.getClass().getMethod("get");
        var ha=java.lang.invoke.MethodHandles.lookup().unreflect(ga).bindTo(a);var hb=java.lang.invoke.MethodHandles.lookup().unreflect(gb).bindTo(b);
        bench("nostalgic_tweak_cached_get",()->blackhole=handle(ha),()->blackhole=handle(hb),20000);
    }
    private static void jei(Loader old,Loader optimized)throws Exception{
        Class<?> ra=old.loadClass("mezz.jei.common.util.ImmutableRect2i"),rb=optimized.loadClass("mezz.jei.common.util.ImmutableRect2i");
        var ca=ra.getConstructor(int.class,int.class,int.class,int.class);var cb=rb.getConstructor(int.class,int.class,int.class,int.class);
        var ma=old.loadClass("mezz.jei.common.util.MathUtil").getMethod("intersects",Collection.class,ra);var mb=optimized.loadClass("mezz.jei.common.util.MathUtil").getMethod("intersects",Collection.class,rb);
        Random random=new Random(44);List<Object> a=new ArrayList<>(),b=new ArrayList<>();
        for(int i=0;i<1000;i++){int x=random.nextInt(300)-150,y=random.nextInt(300)-150,w=random.nextInt(50),h=random.nextInt(50);a.add(ca.newInstance(x,y,w,h));b.add(cb.newInstance(x,y,w,h));if(i%10==0){Object qa=ca.newInstance(140,140,5,5),qb=cb.newInstance(140,140,5,5);require(ma.invoke(null,a,qa).equals(mb.invoke(null,b,qb)),"JEI rectangle result mismatch");}}
        Object qa=ca.newInstance(800,800,1,1),qb=cb.newInstance(800,800,1,1);bench("jei_1000_nonintersections",()->blackhole=ma.invoke(null,a,qa),()->blackhole=mb.invoke(null,b,qb),1000);
    }
    private static void geometry(Loader old,Loader optimized)throws Exception{
        String pylon="net.mcreator.thedulling.client.HealingPylonRenderEvents";Class<?>[] signature={PoseStack.class,VertexConsumer.class,float.class,float.class,float.class,float.class,float.class,float.class,float.class,float.class,float.class};
        var a=old.loadClass(pylon).getDeclaredMethod("renderRippleSphere",signature);a.setAccessible(true);var b=optimized.loadClass(pylon).getDeclaredMethod("renderRippleSphere",signature);b.setAccessible(true);
        for(int sample=0;sample<30;sample++){
            PoseStack pose=new PoseStack();pose.translate(sample*.3,-sample*.1,sample*.2);pose.mulPose(new org.joml.Quaternionf().rotateXYZ(sample*.1F,sample*.17F,sample*.09F));pose.scale(1.2F,.8F,1.1F);
            byte[] before=sphere(a,pose,sample*.43F),after=sphere(b,pose,sample*.43F);require(Arrays.equals(before,after),"Pylon vertex bytes differ at time "+sample);
        }
        PoseStack pose=new PoseStack();
        var positions = PylonSphereMesh.class.getDeclaredMethod("positions", float.class, float.class, float.class, float.class, float.class);
        positions.setAccessible(true);
        try (var stack = org.lwjgl.system.MemoryStack.stackPush()) {
            ByteBuffer encoded = stack.malloc(512 * 16);
            for (int sample = 0; sample < 8; sample++) {
                float time = sample * .43F;
                float[] xyz = (float[]) positions.invoke(null, .48F, time, .035F, .55F, 0F);
                PylonBulkVertices.encode(org.lwjgl.system.MemoryUtil.memAddress(encoded), pose.last().pose(), xyz, new float[xyz.length], .4F, .7F, .2F, .3F);
                byte[] expected = sphere(a, pose, time);
                for (int i = 0; i < expected.length; i++) require(expected[i] == encoded.get(i), "Bulk sphere layout mismatch");
            }
        }
        BufferBuilder ba=new BufferBuilder(10000),bb=new BufferBuilder(10000);
        bench("pylon_10_identical_sphere_layers",()->{for(int i=0;i<10;i++)sphereMeasure(a,pose,31.25F,ba);},()->{for(int i=0;i<10;i++)sphereMeasure(b,pose,31.25F,bb);},256);
    }
    private static byte[] sphere(java.lang.reflect.Method method,PoseStack pose,float time)throws Exception{
        BufferBuilder buffer=new BufferBuilder(10000);buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        method.invoke(null,pose,buffer,.48F,.4F,.7F,.2F,.3F,time,.035F,.55F,0F);
        BufferBuilder.RenderedBuffer rendered=buffer.end();ByteBuffer data=rendered.vertexBuffer();byte[] bytes=new byte[data.remaining()];data.get(bytes);rendered.release();buffer.clear();return bytes;
    }
    private static void sphereMeasure(java.lang.reflect.Method method,PoseStack pose,float time,BufferBuilder buffer)throws Exception{
        buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);method.invoke(null,pose,buffer,.48F,.4F,.7F,.2F,.3F,time,.035F,.55F,0F);
        var data=buffer.end();blackholeInt=data.vertexBuffer().getInt(0);data.release();buffer.clear();
    }
    private static Object handle(java.lang.invoke.MethodHandle handle)throws Exception{try{return handle.invokeExact();}catch(Throwable t){if(t instanceof Exception e)throw e;throw new AssertionError(t);}}
    private static void oozeShape(){
        String name="net.mcreator.enlightened_end.procedures.OozeRainChanceProcedure";var a=node(INPUT.get(name));var b=node(OUTPUT.get(name));
        var ma=a.methods.stream().filter(m->m.desc.startsWith("(Lnet/minecraftforge/eventbus/api/Event;")).findFirst().orElseThrow();var mb=b.methods.stream().filter(m->m.desc.equals(ma.desc)).findFirst().orElseThrow();
        require(ma.maxLocals+2==mb.maxLocals,"Ooze references not invocation-local");
        List<String> before=new ArrayList<>(),after=new ArrayList<>();for(var m:List.of(ma,mb)){var target=m==ma?before:after;for(var i:m.instructions){if(i instanceof FieldInsnNode f && f.getOpcode()==Opcodes.PUTFIELD)target.add(f.owner+f.name+f.desc);if(i instanceof MethodInsnNode call&&call.name.equals("syncData"))target.add(call.owner+call.name+call.desc);}}
        require(before.equals(after),"Ooze timer writes/sync order changed");
    }
    public static net.minecraft.util.RandomSource seededRandom(){return net.minecraft.util.RandomSource.create(7);}
    private static void oozeSemantics()throws Exception{
        String root="net.mcreator.enlightened_end.",procedure=root+"procedures.OozeRainChanceProcedure",mapName=root+"network.EnlightenedEndModVariables$MapVariables",worldName=root+"network.EnlightenedEndModVariables$WorldVariables";
        Map<String,byte[]> savedInput=new HashMap<>(),savedOutput=new HashMap<>();
        for(String name:List.of(procedure,mapName,worldName)){
            savedInput.put(name,INPUT.get(name));savedOutput.put(name,OUTPUT.get(name));
            for(Map<String,byte[]> code:List.of(INPUT,OUTPUT)){
                if(!code.containsKey(name))continue;ClassNode n=node(code.get(name));
                if(name.equals(procedure)){
                    for(MethodNode m:n.methods)for(var i:m.instructions)if(i instanceof MethodInsnNode call&&call.owner.equals("net/minecraft/util/RandomSource")&&call.name.equals("create")&&call.desc.startsWith("()")){call.owner="dev/karma/calamitouscore/compat/performance/PerformanceCheck";call.name="seededRandom";call.itf=false;}
                }else{
                    n.fields.add(new FieldNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"fixtureGets","I",null,null));
                    for(MethodNode m:n.methods)if(m.name.equals("get")){InsnList count=new InsnList();count.add(new FieldInsnNode(Opcodes.GETSTATIC,n.name,"fixtureGets","I"));count.add(new InsnNode(Opcodes.ICONST_1));count.add(new InsnNode(Opcodes.IADD));count.add(new FieldInsnNode(Opcodes.PUTSTATIC,n.name,"fixtureGets","I"));m.instructions.insert(count);m.maxStack+=2;}
                }
                ClassWriter w=new ClassWriter(0);n.accept(w);code.put(name,w.toByteArray());
            }
        }
        try{
            Loader a=new Loader(false),b=new Loader(true);Class<?> ma=a.loadClass(mapName),mb=b.loadClass(mapName),wa=a.loadClass(worldName),wb=b.loadClass(worldName);
            Object am=clientData(ma),bm=clientData(mb),aw=clientData(wa),bw=clientData(wb);
            var ea=a.loadClass(procedure).getDeclaredMethod("execute",net.minecraftforge.eventbus.api.Event.class,net.minecraft.world.level.LevelAccessor.class);ea.setAccessible(true);
            var eb=b.loadClass(procedure).getDeclaredMethod("execute",net.minecraftforge.eventbus.api.Event.class,net.minecraft.world.level.LevelAccessor.class);eb.setAccessible(true);
            int oldGets=0,newGets=0;
            for(int c=0;c<8;c++){
                double delay=c==0?0:10,elapsed=c<2?0:11,chance=c==2?23999:c==3?24000:30000,last=c==6?3:100,till=c==6?3:0;
                for(Object data:List.of(am,bm)){for(String field:List.of("enlightendbiomeseed","dsad","endraintimetillend","endraindelay","endrainlast","endraindo"))data.getClass().getField(field).setDouble(data,0);set(data,"endraindelay",delay);set(data,"endraindo",elapsed);set(data,"endrainlast",last);set(data,"endraintimetillend",till);data.getClass().getField("oozerain").setBoolean(data,c>=5);data.getClass().getMethod("setDirty",boolean.class).invoke(data,false);}
                for(Object data:List.of(aw,bw)){set(data,"endrainchance",chance);data.getClass().getMethod("setDirty",boolean.class).invoke(data,false);}
                for(Class<?> t:List.of(ma,mb,wa,wb))t.getField("fixtureGets").setInt(null,0);
                ea.invoke(null,null,null);eb.invoke(null,null,null);
                for(String f:List.of("endraintimetillend","endraindelay","endrainlast","endraindo","oozerain"))require(ma.getField(f).get(am).equals(mb.getField(f).get(bm)),"Ooze timer/flag differs in case "+c+" / "+f);
                require(wa.getField("endrainchance").get(aw).equals(wb.getField("endrainchance").get(bw)),"Ooze chance differs");
                require(ma.getMethod("isDirty").invoke(am).equals(mb.getMethod("isDirty").invoke(bm)),"Ooze map dirty state differs");
                require(wa.getMethod("isDirty").invoke(aw).equals(wb.getMethod("isDirty").invoke(bw)),"Ooze world dirty state differs");
                int og=ma.getField("fixtureGets").getInt(null)+wa.getField("fixtureGets").getInt(null),ng=mb.getField("fixtureGets").getInt(null)+wb.getField("fixtureGets").getInt(null);
                require(mb.getField("fixtureGets").getInt(null)==1&&wb.getField("fixtureGets").getInt(null)<=1&&ng<=og,"Ooze lookup not invocation-local/lazy");oldGets+=og;newGets+=ng;
            }
            System.out.println("PASS: actual Ooze Rain procedure matches timer/random/flag/dirty-state outcomes in 8 cases; lookup calls "+oldGets+" -> "+newGets+"; sync call order unchanged.");
        }finally{for(var entry:savedInput.entrySet())INPUT.put(entry.getKey(),entry.getValue());for(var entry:savedOutput.entrySet())if(entry.getValue()==null)OUTPUT.remove(entry.getKey());else OUTPUT.put(entry.getKey(),entry.getValue());}
    }
    private static Object clientData(Class<?> type)throws Exception{var field=type.getDeclaredField("clientSide");field.setAccessible(true);return field.get(null);}
    private static void set(Object data,String field,double value)throws Exception{data.getClass().getField(field).setDouble(data,value);}
    private static void lightCache()throws Exception{
        String helper="mod.adrenix.nostalgic.helper.candy.light.LightingHelper",probe="dev.karma.calamitouscore.compat.performance.LightCacheProbe";
        ClassNode merged=node(OUTPUT.get("net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase"));
        require(merged.methods.stream().filter(m->m.name.equals("nt_world_lighting$modifyLightBlock")).flatMap(m->Arrays.stream(m.instructions.toArray())).anyMatch(i->i instanceof MethodInsnNode call&&call.owner.equals("java/util/concurrent/ConcurrentHashMap")&&call.name.equals("get")),"Native Nostalgic cache fast path not applied");
        byte[] previous=INPUT.get(helper);
        ClassWriter stub=new ClassWriter(0);stub.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,helper.replace('.','/'),null,"java/lang/Object",null);stub.visitField(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"LIGHT_BLOCK_CACHE","Ljava/util/concurrent/ConcurrentHashMap;",null,null).visitEnd();stub.visitEnd();INPUT.put(helper,stub.toByteArray());
        try{
            ClassNode n;try(var in=PerformanceCheck.class.getResourceAsStream("/"+probe.replace('.','/')+".class")){n=node(in.readAllBytes());}
            for(MethodNode m:n.methods)for(var i:m.instructions)if(i instanceof FieldInsnNode f&&f.owner.equals(n.name)&&f.name.equals("CACHE")){f.owner=helper.replace('.','/');f.name="LIGHT_BLOCK_CACHE";}
            ClassWriter before=new ClassWriter(0);n.accept(before);INPUT.put(probe,before.toByteArray());require(PerformanceBytecode.lightCacheHits(n)==1,"Captured callback probe not recognized");ClassWriter after=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);n.accept(after);OUTPUT.put(probe,after.toByteArray());
            Object a=new Loader(false).loadClass(probe).getConstructor().newInstance(),b=new Loader(true).loadClass(probe).getConstructor().newInstance();
            var ma=a.getClass().getMethod("calculate",Object.class,int.class);var mb=b.getClass().getMethod("calculate",Object.class,int.class);
            for(Object instance:List.of(a,b)){var method=instance.getClass().getMethod("calculate",Object.class,int.class);require(method.invoke(instance,"block",12).equals(12),"Light miss value");require(method.invoke(instance,"block",99).equals(12),"Light hit uses stale fallback incorrectly");instance.getClass().getMethod("invalidate").invoke(instance);require(method.invoke(instance,"block",99).equals(99),"Light invalidation ignored");require(instance.getClass().getField("computations").getInt(instance)==2,"Light callback run on cache hits");}
            bench("nostalgic_light_cached_lookup",()->blackhole=ma.invoke(a,"block",41),()->blackhole=mb.invoke(b,"block",41),20000);
        }finally{INPUT.put(helper,previous);INPUT.remove(probe);OUTPUT.remove(probe);}
    }
    @SuppressWarnings("unchecked")private static void threadIsolation()throws Exception{
        var f=dev.karma.calamitouscore.compat.summoning.SharedSummonState.class.getDeclaredField("OWNERS");f.setAccessible(true);Object owners=f.get(null);require(owners instanceof ThreadLocal,"Shared synchronized owner map remains");
        ThreadLocal<Map<Object,Object>> local=(ThreadLocal<Map<Object,Object>>)owners;Object key=new Object();local.get().put(key,new Object());var other=new AtomicInteger();Thread thread=new Thread(()->{if(!local.get().containsKey(key))other.incrementAndGet();});thread.start();thread.join();require(other.get()==1,"Owner maps not thread-confined");local.get().remove(key);
    }
    static volatile Object blackhole;
    static volatile int blackholeInt;
    @FunctionalInterface interface Work{void run()throws Exception;}
    static void bench(String name,Work a,Work b,int iterations)throws Exception{
        for(int i=0;i<Math.min(1000,iterations);i++){a.run();b.run();}double[] ma=measure(a,iterations),mb=measure(b,iterations);
        String row=name+","+ma[0]+","+mb[0]+","+ma[1]+","+mb[1];CSV.add(row);System.out.println(row);
    }
    private static double[] measure(Work work,int n)throws Exception{double[] time=new double[5],bytes=new double[5];var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();long id=Thread.currentThread().getId();for(int s=0;s<5;s++){long alloc=bean.getThreadAllocatedBytes(id),start=System.nanoTime();for(int i=0;i<n;i++)work.run();time[s]=(System.nanoTime()-start)/(n*1000.0);bytes[s]=(bean.getThreadAllocatedBytes(id)-alloc)/(double)n;}Arrays.sort(time);Arrays.sort(bytes);return new double[]{time[2],bytes[2]};}
    static ClassNode node(byte[] bytes){ClassNode n=new ClassNode();new ClassReader(bytes).accept(n,0);return n;}
    static void require(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    private static void verify(byte[] bytes,Loader loader){
        // Check changed bodies; unrelated JourneyMap rendering methods require its live Forge access transformers.
        ClassNode selected=node(bytes);selected.methods.removeIf(m->!m.name.contains("calamitouscore$")&&!Set.of("scanChunk","scanActiveFans","getUsedMinionSlots","execute","getBlockPos","getBlockHeight","get","clear","intersects","renderRippleSphere","addQuad","addGradientQuad","vertex","nt_world_lighting$modifyLightBlock").contains(m.name));
        ClassWriter writer=new ClassWriter(0);selected.accept(writer);StringWriter error=new StringWriter();CheckClassAdapter.verify(new ClassReader(writer.toByteArray()),loader,false,new PrintWriter(error));require(error.toString().isBlank(),selected.name+"\n"+error.toString());
    }
    static final class Loader extends ClassLoader{
        final boolean optimized, nativeInterfaceParent;Loader(boolean optimized){this(optimized,false);}Loader(boolean optimized,boolean nativeInterfaceParent){super(PerformanceCheck.class.getClassLoader());this.optimized=optimized;this.nativeInterfaceParent=nativeInterfaceParent;}
        @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{if(nativeInterfaceParent&&name.equals("com.fevzi.summonity.entity.SummonedMinion"))return getParent().loadClass(name);if(INPUT.containsKey(name)||name.startsWith("dev.karma.calamitouscore.compat.performance.")&&!name.contains("PerformanceCheck")&&!name.contains("MinionIndexFixture")){Class<?> c=findLoadedClass(name);if(c==null)c=findClass(name);if(resolve)resolveClass(c);return c;}return super.loadClass(name,resolve);}
        @Override protected Class<?> findClass(String name)throws ClassNotFoundException{byte[] b=optimized?OUTPUT.getOrDefault(name,INPUT.get(name)):INPUT.get(name);if(b==null)try(var in=getParent().getResourceAsStream(name.replace('.','/')+".class")){if(in!=null)b=in.readAllBytes();}catch(IOException e){throw new ClassNotFoundException(name,e);}if(b==null)throw new ClassNotFoundException(name);return defineClass(name,b,0,b.length);}
    }
}




